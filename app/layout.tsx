import type { Metadata } from "next";
import { Tajawal } from "next/font/google";
import "./globals.css";

const tajawal = Tajawal({
  subsets: ["arabic", "latin"],
  weight: ["400", "500", "700", "800", "900"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "نطاق | ماب - شارك رحلتك بدون تسليم جوالك",
  description:
    "أنشئ QR مؤقت لرابط رحلتك من Google Maps وشاركه مع السائق بأمان خلال ثواني.",
  metadataBase: new URL("https://nitaaq-map.vercel.app"),
  icons: {
    icon: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ar" dir="rtl">
      <body className={tajawal.className}>{children}</body>
    </html>
  );
}
