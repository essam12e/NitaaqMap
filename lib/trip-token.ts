import crypto from "node:crypto";
export { isGoogleMapsUrl, normalizeGoogleMapsUrl } from "./maps-url";

const TOKEN_TTL_MS = 5 * 60 * 1000;
const SECRET =
  process.env.TRIP_TOKEN_SECRET ??
  "dev-only-change-this-secret-before-production-deploy";

type TripPayload = {
  v: 1;
  url: string;
  iat: number;
  exp: number;
  nonce: string;
};

export type TripTokenResult =
  | { ok: true; url: string; expiresAt: string }
  | { ok: false; reason: "expired" | "invalid" };

function encodeBase64Url(value: string) {
  return Buffer.from(value, "utf8").toString("base64url");
}

function decodeBase64Url(value: string) {
  return Buffer.from(value, "base64url").toString("utf8");
}

function sign(payload: string) {
  return crypto.createHmac("sha256", SECRET).update(payload).digest("base64url");
}

function timingSafeEqual(left: string, right: string) {
  const leftBuffer = Buffer.from(left);
  const rightBuffer = Buffer.from(right);

  if (leftBuffer.length !== rightBuffer.length) {
    return false;
  }

  return crypto.timingSafeEqual(leftBuffer, rightBuffer);
}

export function createTripToken(url: string) {
  const now = Date.now();
  const payload: TripPayload = {
    v: 1,
    url,
    iat: now,
    exp: now + TOKEN_TTL_MS,
    nonce: crypto.randomBytes(12).toString("base64url"),
  };

  const encodedPayload = encodeBase64Url(JSON.stringify(payload));
  const signature = sign(encodedPayload);

  return {
    id: `${encodedPayload}.${signature}`,
    expiresAt: new Date(payload.exp).toISOString(),
  };
}

export function verifyTripToken(id: string): TripTokenResult {
  const [encodedPayload, signature, ...rest] = id.split(".");

  if (!encodedPayload || !signature || rest.length > 0) {
    return { ok: false, reason: "invalid" };
  }

  const expectedSignature = sign(encodedPayload);

  if (!timingSafeEqual(signature, expectedSignature)) {
    return { ok: false, reason: "invalid" };
  }

  try {
    const payload = JSON.parse(decodeBase64Url(encodedPayload)) as Partial<TripPayload>;

    if (
      payload.v !== 1 ||
      typeof payload.url !== "string" ||
      typeof payload.exp !== "number"
    ) {
      return { ok: false, reason: "invalid" };
    }

    if (payload.exp <= Date.now()) {
      return { ok: false, reason: "expired" };
    }

    return {
      ok: true,
      url: payload.url,
      expiresAt: new Date(payload.exp).toISOString(),
    };
  } catch {
    return { ok: false, reason: "invalid" };
  }
}
