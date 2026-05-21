const coordinatePattern =
  /^-?([0-8]?\d(\.\d+)?|90(\.0+)?),\s*-?((1[0-7]\d|\d?\d)(\.\d+)?|180(\.0+)?)$/;

export function coordinateToGoogleMapsUrl(value: string) {
  const [latitude, longitude] = value.split(",").map((part) => part.trim());
  return `https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}`;
}

export function isCoordinates(value: string) {
  return coordinatePattern.test(value.trim());
}

export function normalizeGoogleMapsUrl(value: string) {
  const trimmed = value.trim();

  if (isCoordinates(trimmed)) {
    return coordinateToGoogleMapsUrl(trimmed);
  }

  return trimmed;
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
    const url = new URL(trimmed);
    const host = url.hostname.toLowerCase();
    const path = url.pathname.toLowerCase();
    const search = url.search.toLowerCase();

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
