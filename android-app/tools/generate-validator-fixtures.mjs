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
  // Directions: must resolve to the FINAL destination, never the origin
  "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925",
  "https://www.google.com/maps/dir/24.7136,46.6753/26.4207,50.0888/21.4858,39.1925",
  "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925/@23.1,43.0,7z/data=!3m1!4b1!4m2!4m1!3e0",
  "https://www.google.com/maps/dir/Riyadh/Jeddah+Corniche/@22,40,6z/data=!4m2",
  "https://www.google.com/maps/dir/%D8%A7%D9%84%D8%B1%D9%8A%D8%A7%D8%B6/%D8%AC%D8%AF%D8%A9",
  "https://www.google.com/maps/dir//24.7,46.6/",
  "https://www.google.com/maps/dir/24.7,46.6/21.4858,+39.1925",
  "https://www.google.com/maps/dir/24.7,46.6/21.4858,%2039.1925",
  "https://www.google.com/maps/dir/?api=1&origin=24.7,46.6&destination=21.4858,39.1925",
  "https://www.google.com/maps/dir/?api=1&origin=Riyadh&destination=Jeddah%20Corniche&travelmode=driving",
  "https://www.google.com/maps/dir/?api=1&destination=caf%C3%A9+%26+bakery",
  "https://www.google.com/maps/dir/",
  "https://www.google.com/maps/dir/24.7,46.6/91,46",
  "https://www.google.com/maps/dir/A/%E0%A4%A",
  "https://www.google.com/maps/dir/A/it%27s+(here)*~!",
  "الطريق: https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925 .",
  // Places / map views
  "https://www.google.com/maps/place/Riyadh/@24.7136,46.6753,12z",
  "https://www.google.com/maps/place/Kingdom+Centre/@24.71,46.67,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d24.7113!4d46.6744",
  "https://www.google.com/maps/place/24.7136,46.6753/@24.7,46.6,15z",
  "https://www.google.com/maps/place/X/data=!3d95.1!4d46.6",
  "https://www.google.com/maps/search/24.7136,+46.6753",
  "https://www.google.com/maps/search/Kingdom+Centre",
  "https://www.google.com/maps/@91,46,10z",
  "https://maps.google.com/?q=24.7,46.6",
  "https://maps.google.com/maps?query=Riyadh&q=24.7,46.6",
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
  "-90,-180",
  "90.000,180.0",
  "0,0",
  "24.7136 , 46.6753",
  "24.7136,\u00a046.6753",
  "خطأ 95,10 الصحيح 21.4858,39.1925",
  // Out of range / malformed coordinates: rejected, never truncated
  "91,46",
  "-91,46",
  "90.0001,46",
  "24.7136,181",
  "24.7136,-180.5",
  "191,46",
  "24.7,1812",
  "1000,10",
  "موقعي 91,46 الآن",
  "24.,46",
  "24.7;46.6",
  "24.7,46.6,12",
  // Invalid
  "",
  "   ",
  "hello",
  "https://example.com/maps",
  "https://evil.com/?q=google.com/maps",
  "https://evil.com/?next=https://maps.app.goo.gl/x",
  "موقعي:https://maps.app.goo.gl/x",
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
