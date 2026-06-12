import type { Metadata } from "next";
import { Header } from "@/widgets/header/header";
import { Analytics } from "@vercel/analytics/next";
import { SpeedInsights } from "@vercel/speed-insights/next";
import { Geist_Mono, Noto_Sans_KR } from "next/font/google";
import "./globals.css";
import Providers from "@/app/providers";

const notoSansKr = Noto_Sans_KR({
  weight: ["400", "700", "900"],
  display: "swap",
  preload: false,
  variable: "--font-noto-sans-kr",
});

const geistMono = Geist_Mono({
  weight: ["400", "500", "700"],
  display: "swap",
  preload: false,
  variable: "--font-geist-mono",
});

export const metadata: Metadata = {
  title: "줍줍 (zup-zup)",
  description: "전북대학교 수강신청 빈자리 알림 서비스 '줍줍'",
  manifest: "/manifest.json",
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "줍줍",
  },
  icons: {
    apple: [
      { url: "/zub-zub-logo.png", sizes: "192x192", type: "image/png" },
    ],
  },
  formatDetection: {
    telephone: false,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${notoSansKr.variable} ${geistMono.variable}`}>
      <body className="antialiased">
        <Providers>
          <Header />
          {children}
        </Providers>
        <Analytics />
        <SpeedInsights />
      </body>
    </html>
  );
}
