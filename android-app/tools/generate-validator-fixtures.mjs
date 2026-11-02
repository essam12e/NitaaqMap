// Generates core/src/test/resources/maps_url_cases.json from the *real* web validator
// (lib/maps-url.ts) so the Kotlin port is tested for parity with the server.
//
// Usage (Node >= 22.6): node --experimental-strip-types android-app/tools/generate-validator-fixtures.mjs
import { writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const { isGoogleMapsUrl, normalizeGoogleMapsUrl } = await import(join(here, "../../lib/maps-url.ts"));

const inputs = [
  // Valid share links
  "https://maps.app.goo.gl/AbCdEf123",
  "maps.app.goo.gl/AbCdEf123",
  "https://goo.gl/maps/xyz987",
  "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925",
  "https://www.google.com/maps/place/Riyadh/@24.7136,46.6753,12z",
  "https://google.com/maps?q=24.7,46.6",
  "https://maps.google.com/?q=Kingdom+Centre",
  "https://www.google.com/maps/search/?api=1&query=24.7,46.6",
  "https://www.google.com/maps/dir/?api=1&destination=21.4858,39.1925",
  "https://www.google.com.sa/maps/place/x",
  "HTTPS://WWW.GOOGLE.COM/MAPS/place/x",
  "www.google.com/maps/place/Jeddah",
  "google.com/maps/@21.5,39.2,14z",
  "https://www.google.com/?q=cafe",
  "https://www.google.com/search?q=maps",
  // Wrapped in shared text (Google Maps share sheet adds text around the link)
  "شوف موقعي: https://maps.app.goo.gl/AbCdEf123 وصلني بسرعة",
  "(https://maps.app.goo.gl/AbCdEf123).",
  "Meet me here https://maps.app.goo.gl/AbCdEf123، شكراً",
  // Coordinates
  "24.7136,46.6753",
  "24.7136, 46.6753",
  "-33.8688,151.2093",
  "90,180",
  "موقعي 24.7136, 46.6753 الآن",
  "91,46",
  "24.7136,181",
  // Invalid
  "",
  "   ",
  "hello",
  "https://example.com/maps",
  "https://evil.com/?q=google.com/maps",
  "https://google.com.evil.com/maps",
  "https://maps.apple.com/?q=Riyadh",
  "http://goo.gl/abc",
  "https://goo.gl/maps",
  "ftp://maps.google.com/maps",
  "javascript:alert(1)",
  "https://www.google.com/",
  "https://www.google.com/mail",
  "https://user@www.google.com/maps/place/x",
  "https://www.google.com:8443/maps/x",
  "https://notgoogle.com/maps",
  "https://mapsgoogle.com/maps",
  "https://www.google.com/travel/flights?q=x",
  "https://drive.google.com/file?query=1",
];

const cases = inputs.map((input) => ({
  input,
  valid: isGoogleMapsUrl(input),
  normalized: normalizeGoogleMapsUrl(input),
}));

const out = join(here, "../core/src/test/resources/maps_url_cases.json");
writeFileSync(out, JSON.stringify(cases, null, 2) + "\n");
console.log(`wrote ${cases.length} cases -> ${out}`);
