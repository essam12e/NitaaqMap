// End-to-end check of the server-side expiry that the Android app relies on.
// Talks to a running Nitaaq server (default http://localhost:3000) exactly like a phone camera
// scanning the QR would: plain GET of /trip/<id>, no redirects followed.
//
//   BASE=http://localhost:3000 TRIP_TOKEN_SECRET=<same secret as server> \
//     node --experimental-strip-types android-app/tools/e2e-server-expiry.mjs [--wait-real-ttl]
//
// --wait-real-ttl additionally waits 5 min + 5 s in real time and re-scans the same QR link.
import crypto from "node:crypto";

const BASE = process.env.BASE ?? "http://localhost:3000";

// Same token format as lib/trip-token.ts (v1 payload, HMAC-SHA256, base64url), with a chosen issue time.
function signTokenIssuedAt(url, issuedAtMs, secret, ttlMs = 5 * 60 * 1000) {
  const payload = { v: 1, url, iat: issuedAtMs, exp: issuedAtMs + ttlMs, nonce: crypto.randomBytes(12).toString("base64url") };
  const encoded = Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
  const signature = crypto.createHmac("sha256", secret).update(encoded).digest("base64url");
  return `${encoded}.${signature}`;
}
const EXPIRED_TEXT = "انتهت صلاحية رابط الرحلة";
let failures = 0;

function check(name, ok, detail = "") {
  console.log(`${ok ? "PASS" : "FAIL"}  ${name}${detail ? "  — " + detail : ""}`);
  if (!ok) failures++;
}

async function scan(tripUrl) {
  const res = await fetch(tripUrl, { redirect: "manual" });
  const body = res.status === 200 ? await res.text() : "";
  return { status: res.status, location: res.headers.get("location"), expired: body.includes(EXPIRED_TEXT) };
}

// 1. Create a trip through the public API (same request the app sends).
const mapsUrl = "https://www.google.com/maps/search/?api=1&query=24.7136,46.6753";
const createdAt = Date.now();
const res = await fetch(`${BASE}/api/trips`, {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ url: mapsUrl }),
});
const created = await res.json();
check("POST /api/trips returns 200", res.status === 200, `status=${res.status}`);
check("response has id, tripUrl, expiresAt", Boolean(created.id && created.tripUrl && created.expiresAt));
const ttl = Date.parse(created.expiresAt) - createdAt;
check("expiresAt is ~5 minutes ahead (server clock)", ttl > 295_000 && ttl <= 301_000, `ttl=${ttl}ms`);
check("tripUrl is /trip/<id> (not the Maps link)", created.tripUrl.endsWith(`/trip/${created.id}`) && !created.tripUrl.includes("google"));
const serverDate = res.headers.get("date");
check("response carries a Date header (used by the app for clock skew)", Boolean(serverDate), serverDate ?? "");

// 2. Scan while valid -> redirect to Google Maps.
const fresh = await scan(created.tripUrl);
check("valid QR redirects to Google Maps", fresh.status >= 300 && fresh.status < 400 && fresh.location === mapsUrl, `status=${fresh.status} location=${fresh.location}`);

// 3. Invalid inputs rejected by the server.
for (const [input, code] of [["", 400], ["https://example.com/maps", 422], ["javascript:alert(1)", 422]]) {
  const r = await fetch(`${BASE}/api/trips`, { method: "POST", headers: { "content-type": "application/json" }, body: JSON.stringify({ url: input }) });
  const j = await r.json();
  check(`server rejects ${JSON.stringify(input)} with ${code}`, r.status === code && typeof j.error === "string", `status=${r.status} error=${j.error}`);
}

// 4. A token signed by the real secret but issued 5 min + 1 s ago (as if the QR was screenshotted).
if (process.env.TRIP_TOKEN_SECRET) {
  const secret = process.env.TRIP_TOKEN_SECRET;
  // Control: the same signer with a fresh issue time IS accepted (proves the signer matches the server).
  const control = await scan(`${BASE}/trip/${signTokenIssuedAt(mapsUrl, Date.now(), secret)}`);
  check("control: locally signed fresh token is accepted", control.status >= 300 && control.status < 400, `status=${control.status}`);
  const old = signTokenIssuedAt(mapsUrl, Date.now() - (5 * 60 * 1000 + 1000), secret);
  const stale = await scan(`${BASE}/trip/${old}`);
  check("screenshot of a QR older than 5 min is refused", stale.status === 200 && stale.expired && !stale.location, `status=${stale.status}`);

  // Server hardening (lib/trip-token.ts): signed tokens longer than the TTL, or pointing outside Google Maps, are refused.
  const longLived = await scan(`${BASE}/trip/${signTokenIssuedAt(mapsUrl, Date.now(), secret, 365 * 24 * 3600 * 1000)}`);
  check("signed token with a TTL longer than 5 min is refused", longLived.status === 200 && longLived.expired && !longLived.location, `status=${longLived.status}`);
  const offsite = await scan(`${BASE}/trip/${signTokenIssuedAt("https://evil.example/phish", Date.now(), secret)}`);
  check("signed token redirecting outside Google Maps is refused", offsite.status === 200 && offsite.expired && !offsite.location, `status=${offsite.status}`);
} else {
  console.log("SKIP  backdated token check (TRIP_TOKEN_SECRET not provided)");
}

// 5. Tampering: extend exp inside the payload without the secret.
const [payload, sig] = created.id.split(".");
const decoded = JSON.parse(Buffer.from(payload, "base64url").toString());
decoded.exp += 24 * 3600 * 1000;
const forged = `${Buffer.from(JSON.stringify(decoded)).toString("base64url")}.${sig}`;
const forgedScan = await scan(`${BASE}/trip/${forged}`);
check("payload with extended exp (forged) is refused", forgedScan.status === 200 && forgedScan.expired && !forgedScan.location);

// 6. The public dev fallback secret must not be accepted by the server under test.
const devSigned = await scan(`${BASE}/trip/${signTokenIssuedAt(mapsUrl, Date.now(), "dev-only-change-this-secret-before-production-deploy")}`);
check("token signed with the public dev fallback secret is refused", devSigned.status === 200 && devSigned.expired && !devSigned.location, `status=${devSigned.status}`);

// 7. Real-time wait: the very same QR link must stop working after 5 minutes.
if (process.argv.includes("--wait-real-ttl")) {
  const waitMs = Date.parse(created.expiresAt) - Date.now() + 5_000;
  console.log(`...waiting ${Math.round(waitMs / 1000)} s of real time`);
  await new Promise((r) => setTimeout(r, waitMs));
  const later = await scan(created.tripUrl);
  check("same QR scanned after 5 min is refused (real wait)", later.status === 200 && later.expired && !later.location, `status=${later.status}`);
}

console.log(failures === 0 ? "\nALL CHECKS PASSED" : `\n${failures} CHECK(S) FAILED`);
process.exit(failures === 0 ? 0 : 1);
