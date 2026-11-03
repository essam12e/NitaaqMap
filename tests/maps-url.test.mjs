// Unit tests for lib/maps-url.ts (server-side validation used by POST /api/trips).
// Run: npm test   (Node >= 22.6, uses --experimental-strip-types)
import { test, describe } from "node:test";
import assert from "node:assert/strict";

const { isGoogleMapsUrl, normalizeGoogleMapsUrl, isCoordinates } = await import(
  "../lib/maps-url.ts"
);

const pin = (lat, lng) => `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
const route = (name) =>
  `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(name)}`;

function expectValid(input, normalized) {
  assert.equal(isGoogleMapsUrl(input), true, `should accept: ${input}`);
  assert.equal(normalizeGoogleMapsUrl(input), normalized, `normalize: ${input}`);
}

function expectInvalid(input) {
  assert.equal(isGoogleMapsUrl(input), false, `should reject: ${input}`);
}

describe("directions links resolve to the final destination", () => {
  test("coordinate origin and destination", () => {
    expectValid(
      "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925",
      pin("21.4858", "39.1925"),
    );
  });

  test("multi-stop route uses the last stop", () => {
    expectValid(
      "https://www.google.com/maps/dir/24.7136,46.6753/26.4207,50.0888/21.4858,39.1925",
      pin("21.4858", "39.1925"),
    );
  });

  test("map view and data blob after the waypoints are ignored", () => {
    expectValid(
      "https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925/@23.1,43.0,7z/data=!3m1!4b1!4m2!4m1!3e0",
      pin("21.4858", "39.1925"),
    );
  });

  test("named destination becomes a directions link to that name", () => {
    expectValid(
      "https://www.google.com/maps/dir/Riyadh/Jeddah+Corniche/@22,40,6z/data=!4m2",
      route("Jeddah Corniche"),
    );
  });

  test("Arabic percent-encoded destination", () => {
    expectValid(
      "https://www.google.com/maps/dir/%D8%A7%D9%84%D8%B1%D9%8A%D8%A7%D8%B6/%D8%AC%D8%AF%D8%A9",
      route("جدة"),
    );
  });

  test("empty origin (current location) and trailing slash", () => {
    expectValid("https://www.google.com/maps/dir//24.7,46.6/", pin("24.7", "46.6"));
  });

  test("coordinates with an encoded space", () => {
    expectValid(
      "https://www.google.com/maps/dir/24.7,46.6/21.4858,+39.1925",
      pin("21.4858", "39.1925"),
    );
    expectValid(
      "https://www.google.com/maps/dir/24.7,46.6/21.4858,%2039.1925",
      pin("21.4858", "39.1925"),
    );
  });

  test("api=1 destination with coordinates, origin ignored", () => {
    expectValid(
      "https://www.google.com/maps/dir/?api=1&origin=24.7,46.6&destination=21.4858,39.1925",
      pin("21.4858", "39.1925"),
    );
  });

  test("api=1 destination with a name", () => {
    expectValid(
      "https://www.google.com/maps/dir/?api=1&origin=Riyadh&destination=Jeddah%20Corniche&travelmode=driving",
      route("Jeddah Corniche"),
    );
  });

  test("directions link without waypoints is kept", () => {
    expectValid("https://www.google.com/maps/dir/", "https://www.google.com/maps/dir/");
  });

  test("directions link inside shared text", () => {
    expectValid(
      "الطريق: https://www.google.com/maps/dir/24.7136,46.6753/21.4858,39.1925 .",
      pin("21.4858", "39.1925"),
    );
  });

  test("invalid destination coordinates are not turned into a pin", () => {
    // 91 is not a latitude: keep the link as a named directions request instead of
    // silently rewriting it to something else.
    expectValid("https://www.google.com/maps/dir/24.7,46.6/91,46", route("91,46"));
  });
});

describe("place and map links", () => {
  test("place uses the pin from the data blob, not the view centre", () => {
    expectValid(
      "https://www.google.com/maps/place/Kingdom+Centre/@24.71,46.67,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d24.7113!4d46.6744",
      pin("24.7113", "46.6744"),
    );
  });

  test("place without data blob keeps the place link", () => {
    const url = "https://www.google.com/maps/place/Riyadh/@24.7136,46.6753,12z";
    expectValid(url, url);
  });

  test("place named by coordinates", () => {
    expectValid(
      "https://www.google.com/maps/place/24.7136,46.6753/@24.7,46.6,15z",
      pin("24.7136", "46.6753"),
    );
  });

  test("map view uses its centre", () => {
    expectValid("google.com/maps/@21.5,39.2,14z", pin("21.5", "39.2"));
  });

  test("search path with coordinates", () => {
    expectValid("https://www.google.com/maps/search/24.7136,+46.6753", pin("24.7136", "46.6753"));
  });

  test("q and query parameters with coordinates", () => {
    expectValid("https://google.com/maps?q=24.7,46.6", pin("24.7", "46.6"));
    expectValid("https://maps.google.com/?q=24.7,46.6", pin("24.7", "46.6"));
    expectValid(
      "https://www.google.com/maps/search/?api=1&query=24.7,46.6",
      pin("24.7", "46.6"),
    );
  });

  test("named queries and short links are kept", () => {
    for (const url of [
      "https://maps.google.com/?q=Kingdom+Centre",
      "https://maps.app.goo.gl/AbCdEf123",
      "https://goo.gl/maps/xyz987",
    ]) {
      expectValid(url, url);
    }
    expectValid("maps.app.goo.gl/AbCdEf123", "https://maps.app.goo.gl/AbCdEf123");
  });

  test("normalization is idempotent", () => {
    for (const input of [
      "https://www.google.com/maps/dir/24.7,46.6/21.4858,39.1925",
      "https://www.google.com/maps/dir/Riyadh/Jeddah+Corniche",
      "https://www.google.com/maps/dir/%D8%A7%D9%84%D8%B1%D9%8A%D8%A7%D8%B6/%D8%AC%D8%AF%D8%A9",
      "24.7136,46.6753",
    ]) {
      const once = normalizeGoogleMapsUrl(input);
      assert.equal(normalizeGoogleMapsUrl(once), once);
      assert.equal(isGoogleMapsUrl(once), true);
    }
  });
});

describe("coordinates", () => {
  test("valid pairs including the exact limits", () => {
    expectValid("24.7136,46.6753", pin("24.7136", "46.6753"));
    expectValid("24.7136, 46.6753", pin("24.7136", "46.6753"));
    expectValid("24.7136 , 46.6753", pin("24.7136", "46.6753"));
    expectValid("-33.8688,151.2093", pin("-33.8688", "151.2093"));
    expectValid("90,180", pin("90", "180"));
    expectValid("-90,-180", pin("-90", "-180"));
    expectValid("90.000,180.0", pin("90.000", "180.0"));
    expectValid("0,0", pin("0", "0"));
  });

  test("out of range values are rejected, not truncated", () => {
    for (const input of [
      "91,46",
      "-91,46",
      "90.0001,46",
      "24.7136,181",
      "24.7136,-180.5",
      "191,46",
      "24.7,1812",
      "1000,10",
      "موقعي 91,46 الآن",
    ]) {
      expectInvalid(input);
      assert.equal(isCoordinates(input), false, input);
    }
  });

  test("malformed pairs are rejected", () => {
    for (const input of ["24.,46", "24.7,", ",46.6", "24.7;46.6", "24.7,46.6,12", "abc,def"]) {
      assert.equal(isCoordinates(input), false, input);
    }
  });

  test("coordinates inside text", () => {
    expectValid("موقعي 24.7136, 46.6753 الآن", pin("24.7136", "46.6753"));
    // The first *valid* pair wins: 95,10 is skipped.
    expectValid("خطأ 95,10 الصحيح 21.4858,39.1925", pin("21.4858", "39.1925"));
  });
});

describe("rejects links that are not Google Maps", () => {
  for (const input of [
    "",
    "   ",
    "hello",
    "https://example.com/maps",
    "https://evil.com/?q=google.com/maps",
    "https://evil.com/?next=https://maps.app.goo.gl/x",
    "https://notgoogle.com/maps",
    "https://mapsgoogle.com/maps",
    "https://google.com.evil.com/maps",
    "https://maps.apple.com/?q=Riyadh",
    "http://goo.gl/abc",
    "ftp://maps.google.com/maps",
    "javascript:alert(1)",
    "https://www.google.com/",
    "https://www.google.com/mail",
    "https://www.google.com.sa/maps/place/x",
  ]) {
    test(JSON.stringify(input), () => expectInvalid(input));
  }
});
