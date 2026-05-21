import { NextResponse } from "next/server";
import {
  createTripToken,
  isGoogleMapsUrl,
  normalizeGoogleMapsUrl,
} from "@/lib/trip-token";

export async function POST(request: Request) {
  const body = (await request.json().catch(() => null)) as {
    url?: unknown;
    siteOrigin?: unknown;
  } | null;
  const url = typeof body?.url === "string" ? body.url.trim() : "";
  const normalizedUrl = normalizeGoogleMapsUrl(url);

  if (!url) {
    return NextResponse.json(
      { error: "فضلا الصق رابط الرحلة أولا." },
      { status: 400 },
    );
  }

  if (!isGoogleMapsUrl(url)) {
    return NextResponse.json(
      { error: "الرابط يجب أن يكون رابط خرائط Google صالح." },
      { status: 422 },
    );
  }

  const { id, expiresAt } = createTripToken(normalizedUrl);
  const origin =
    typeof body?.siteOrigin === "string" && /^https?:\/\//.test(body.siteOrigin)
      ? body.siteOrigin
      : new URL(request.url).origin;
  const tripUrl = new URL(`/trip/${id}`, origin).toString();

  return NextResponse.json({
    id,
    tripUrl,
    expiresAt,
  });
}
