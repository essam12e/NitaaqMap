import type { Metadata, Viewport } from "next";
import { Tajawal } from "next/font/google";
import { Analytics } from "@vercel/analytics/next";
import "./globals.css";

const tajawal = Tajawal({
  subsets: ["arabic", "latin"],
  weight: ["400", "500", "700", "800", "900"],
  display: "swap",
});

export const metadata: Metadata = {
  applicationName: "نطاق ماب",
  title: "نطاق ماب | مشاركة الرحلات بأمان",
  description: "إنشاء QR مؤقت لمشاركة روابط خرائط جوجل بأمان",
  metadataBase: new URL("https://nitaaq-map.vercel.app"),
  manifest: "/manifest.json",
  icons: {
    icon: "/icon-192.png",
    apple: "/apple-touch-icon.png",
  },
  openGraph: {
    title: "نطاق ماب | مشاركة الرحلات بأمان",
    description: "إنشاء QR مؤقت لمشاركة روابط خرائط جوجل بأمان",
    siteName: "نطاق ماب",
    locale: "ar_SA",
    type: "website",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "شعار نطاق ماب",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "نطاق ماب | مشاركة الرحلات بأمان",
    description: "إنشاء QR مؤقت لمشاركة روابط خرائط جوجل بأمان",
    images: ["/og-image.png"],
  },
  appleWebApp: {
    capable: true,
    title: "نطاق ماب",
    statusBarStyle: "black-translucent",
  },
  other: {
    "mobile-web-app-capable": "yes",
    "apple-mobile-web-app-capable": "yes",
    "apple-mobile-web-app-title": "نطاق ماب",
    "apple-mobile-web-app-status-bar-style": "black-translucent",
  },
};

export const viewport: Viewport = {
  themeColor: "#0b1a2b",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ar" dir="rtl">
      <body className={tajawal.className}>
        {children}
        <Analytics />
      </body>
    </html>
  );
}
