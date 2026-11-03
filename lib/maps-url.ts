// Google Maps link validation and normalization.
//
// The Android app ships a line-by-line Kotlin port of this file
// (android-app/core/.../domain/MapsLinkValidator.kt). Any behaviour change here must be
// mirrored there and the shared fixture regenerated with
// `node --experimental-strip-types android-app/tools/generate-validator-fixtures.mjs`.

// A coordinate number as people paste it: optional sign, up to 3 integer digits, optional
// fraction. Range (lat ±90, lng ±180) is checked numerically, never by regex.
const NUMBER = "-?\\d{1,3}(?:\\.\\d+)?";

const coordinatePattern = new RegExp(`^(${NUMBER})\\s*,\\s*(${NUMBER})$`);

// Same pair inside free text. The look-arounds stop a pair from being cut out of a longer
// number (e.g. "191,46" must not yield "91,46", and "24.7,1812" must not yield "24.7,181").
const coordinateSearch = new RegExp(
  `(?<![\\d.])(${NUMBER})\\s*,\\s*(${NUMBER})(?![\\d.])`,
  "g",
);

const bareHostPrefix =
  /^(google\.com|www\.google\.com|maps\.google\.com|maps\.app\.goo\.gl|goo\.gl)\//i;

// A Maps link inside shared text. It must start at a word boundary of the text, so
// "notgoogle.com/maps" or "evil.com/?q=google.com/maps" are not mistaken for Google links.
const urlSearch =
  /(?<![A-Za-z0-9._\-/@=?&%+~#])(https?:\/\/)?((www\.)?google\.com\/maps|maps\.google\.com|maps\.app\.goo\.gl|goo\.gl\/maps)[^\s<>"']*/i;

const trailingPunctuation = /[)\].,،]+$/;

// Place pages carry the real pin in their data blob: ...!3d<lat>!4d<lng>...
const placeDataCoordinates = /!3d(-?\d+(?:\.\d+)?)!4d(-?\d+(?:\.\d+)?)/g;

const mapViewSegment = /^@(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)(?:,|$)/;

// RFC 3986, Appendix B.
const uriParts = /^([a-zA-Z][a-zA-Z0-9+.-]*):(\/\/([^/?#]*))?([^?#]*)(\?[^#]*)?(#.*)?$/;

type Coordinates = { latitude: string; longitude: string };

function toCoordinates(latitude: string, longitude: string): Coordinates | null {
  const lat = Number(latitude);
  const lng = Number(longitude);

  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }

  if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
    return null;
  }

  return { latitude, longitude };
}

function parseCoordinates(value: string): Coordinates | null {
  const match = value.trim().match(coordinatePattern);
  return match ? toCoordinates(match[1], match[2]) : null;
}

function findCoordinates(value: string): Coordinates | null {
  for (const match of value.matchAll(coordinateSearch)) {
    const coordinates = toCoordinates(match[1], match[2]);
    if (coordinates) {
      return coordinates;
    }
  }

  return null;
}

function coordinatesUrl({ latitude, longitude }: Coordinates) {
  return `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;
}

function directionsUrl(destination: string) {
  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destination)}`;
}

function decodeComponent(value: string) {
  const plusDecoded = value.replace(/\+/g, " ");
  try {
    return decodeURIComponent(plusDecoded);
  } catch {
    return plusDecoded;
  }
}

function queryParam(query: string, name: string): string | null {
  for (const pair of query.replace(/^\?/, "").split("&")) {
    const separator = pair.indexOf("=");
    const key = decodeComponent(separator === -1 ? pair : pair.slice(0, separator));
    if (key === name) {
      return decodeComponent(separator === -1 ? "" : pair.slice(separator + 1));
    }
  }

  return null;
}

export function coordinateToGoogleMapsUrl(value: string) {
  const [latitude, longitude] = value.split(",").map((part) => part.trim());
  return coordinatesUrl({ latitude, longitude });
}

export function isCoordinates(value: string) {
  return parseCoordinates(value) !== null;
}

/**
 * Turns whatever the user pasted into the single link a driver should open.
 *
 * - Bare coordinates ("24.7,46.6"), or coordinates inside text, become a search pin.
 * - Directions links (`/maps/dir/<origin>/<stop>/<destination>` and `?destination=`) resolve to
 *   the FINAL destination, never the origin.
 * - Place links keep the place (the `!3d!4d` pin when present), not the map-view centre.
 * - Short links (maps.app.goo.gl, goo.gl/maps) are kept as they are.
 */
export function normalizeGoogleMapsUrl(value: string) {
  const trimmed = value.trim();

  const coordinates = parseCoordinates(trimmed);
  if (coordinates) {
    return coordinatesUrl(coordinates);
  }

  const urlMatch = trimmed.match(urlSearch);
  if (urlMatch) {
    return resolveMapsUrl(urlMatch[0].replace(trailingPunctuation, ""));
  }

  const found = findCoordinates(trimmed);
  if (found) {
    return coordinatesUrl(found);
  }

  return trimmed;
}

function resolveMapsUrl(candidate: string) {
  const url = bareHostPrefix.test(candidate) ? `https://${candidate}` : candidate;
  const parts = url.match(uriParts);

  if (!parts) {
    return url;
  }

  const host = (parts[3] ?? "").toLowerCase();
  if (host === "maps.app.goo.gl" || host === "goo.gl") {
    return url;
  }

  const path = parts[4] ?? "";
  const query = parts[5] ?? "";

  const destination = queryParam(query, "destination")?.trim();
  if (destination) {
    const coordinates = parseCoordinates(destination);
    return coordinates ? coordinatesUrl(coordinates) : directionsUrl(destination);
  }

  const segments = path.split("/");
  if ((segments[1] ?? "").toLowerCase() === "maps") {
    const rest = segments.slice(2);
    const kind = (rest[0] ?? "").toLowerCase();

    if (kind === "dir") {
      const waypoints: string[] = [];
      for (const segment of rest.slice(1)) {
        if (segment.startsWith("@") || segment.startsWith("data=")) {
          break;
        }
        const waypoint = decodeComponent(segment).trim();
        if (waypoint) {
          waypoints.push(waypoint);
        }
      }

      const last = waypoints[waypoints.length - 1];
      if (last) {
        const coordinates = parseCoordinates(last);
        return coordinates ? coordinatesUrl(coordinates) : directionsUrl(last);
      }
    }

    if (kind === "place") {
      let pin: Coordinates | null = null;
      for (const match of path.matchAll(placeDataCoordinates)) {
        pin = toCoordinates(match[1], match[2]) ?? pin;
      }
      pin ??= parseCoordinates(decodeComponent(rest[1] ?? ""));
      return pin ? coordinatesUrl(pin) : url;
    }

    if (kind === "search") {
      const coordinates = parseCoordinates(decodeComponent(rest[1] ?? ""));
      if (coordinates) {
        return coordinatesUrl(coordinates);
      }
    }

    const view = (rest[0] ?? "").match(mapViewSegment);
    if (view) {
      const coordinates = toCoordinates(view[1], view[2]);
      if (coordinates) {
        return coordinatesUrl(coordinates);
      }
    }
  }

  const q = queryParam(query, "q") ?? queryParam(query, "query");
  const queryCoordinates = q === null ? null : parseCoordinates(q);
  if (queryCoordinates) {
    return coordinatesUrl(queryCoordinates);
  }

  return url;
}

export function isGoogleMapsUrl(value: string) {
  const trimmed = value.trim();

  if (!trimmed) {
    return false;
  }

  if (isCoordinates(trimmed)) {
    return true;
  }

  try {
    const candidate = normalizeGoogleMapsUrl(trimmed);
    const url = new URL(candidate);
    const host = url.hostname.toLowerCase();
    const path = url.pathname.toLowerCase();
    const search = url.search.toLowerCase();

    if (url.protocol !== "https:" && url.protocol !== "http:") {
      return false;
    }

    if (host === "maps.app.goo.gl") {
      return true;
    }

    if (host === "goo.gl" && path.startsWith("/maps")) {
      return true;
    }

    const isGoogleHost =
      host === "google.com" ||
      host.endsWith(".google.com") ||
      host === "maps.google.com" ||
      host.endsWith(".maps.google.com");

    if (!isGoogleHost) {
      return false;
    }

    return (
      path === "/maps" ||
      path.startsWith("/maps/") ||
      path.includes("/maps") ||
      search.includes("api=1") ||
      search.includes("q=") ||
      search.includes("query=") ||
      search.includes("destination=")
    );
  } catch {
    return false;
  }
}
