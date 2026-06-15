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
  title: "줍줍 | 전북대학교 수강신청 빈자리 알림",
  description: "전북대학교 수강신청 빈자리 알림 서비스 '줍줍'. 실시간 여석 알림, 스마트 시간표 시뮬레이션 및 정밀 강의 검색을 무료로 이용하세요.",
  keywords: ["전북대", "전북대학교", "수강신청", "빈자리 알림", "여석 알림", "줍줍", "시간표", "오아시스", "JBNU"],
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
  openGraph: {
    title: "줍줍 | 전북대학교 수강신청 빈자리 알림",
    description: "새로고침은 이제 그만! 전북대 수강신청 여석이 생기면 문자/푸시로 즉시 알려드리는 '줍줍' 서비스입니다.",
    url: "https://zup-zup.com",
    siteName: "줍줍",
    locale: "ko_KR",
    type: "website",
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
