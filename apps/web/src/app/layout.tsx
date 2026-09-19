import type { Metadata } from "next";
import { Header } from "@/widgets/header/header";
import { Analytics } from "@vercel/analytics/next";
import { SpeedInsights } from "@vercel/speed-insights/next";
import { Geist_Mono, Noto_Sans_KR } from "next/font/google";
import "./globals.css";
import Providers from "@/app/providers";
import { ThirdPartyAnalytics } from "@/shared/analytics/third-party-analytics";
import { generateWebsiteJsonLd } from "@/shared/seo/json-ld";
import {
  BRAND_ICON_180_PATH,
  BRAND_ICON_192_PATH,
  BRAND_ICON_32_PATH,
  BRAND_ICON_512_PATH,
  BRAND_OG_IMAGE,
  SITE_URL,
} from "@/shared/seo/branding";

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

const googleSiteVerification =
  process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION ||
  "DaW5KrDfF4-YAXgvstuFkqdlXCZ75uXt5Xpg6FD71wo";

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: "전북대 수강신청 빈자리 알림 | 전북대학교 줍줍",
  description: "전북대 수강신청 중 원하는 강의에 빈자리가 생기면 알려주는 전북대학교 수강신청 알림 서비스 '줍줍'입니다. 실시간 여석 알림과 스마트 시간표를 무료로 이용하세요.",
  keywords: ["전북대", "전북대학교", "수강신청", "빈자리 알림", "여석 알림", "줍줍", "시간표", "오아시스", "JBNU"],
  manifest: "/manifest.json",
  alternates: {
    canonical: SITE_URL,
  },
  verification: {
    google: googleSiteVerification,
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "default",
    title: "줍줍",
  },
  icons: {
    icon: [
      { url: BRAND_ICON_32_PATH, sizes: "32x32", type: "image/png" },
      { url: BRAND_ICON_192_PATH, sizes: "192x192", type: "image/png" },
    ],
    shortcut: [
      { url: BRAND_ICON_32_PATH, sizes: "32x32", type: "image/png" },
    ],
    apple: [
      { url: BRAND_ICON_180_PATH, sizes: "180x180", type: "image/png" },
    ],
  },
  formatDetection: {
    telephone: false,
  },
  openGraph: {
    title: "전북대 수강신청 빈자리 알림 | 전북대학교 줍줍",
    description: "새로고침은 이제 그만! 전북대 수강신청 여석이 생기면 문자/푸시로 즉시 알려드리는 '줍줍' 서비스입니다.",
    url: SITE_URL,
    siteName: "줍줍",
    locale: "ko_KR",
    type: "website",
    images: [BRAND_OG_IMAGE],
  },
  twitter: {
    card: "summary_large_image",
    title: "전북대 수강신청 빈자리 알림 | 전북대학교 줍줍",
    description: "새로고침은 이제 그만! 전북대 수강신청 여석이 생기면 문자/푸시로 즉시 알려드리는 '줍줍' 서비스입니다.",
    images: [BRAND_ICON_512_PATH],
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      "max-video-preview": -1,
      "max-image-preview": "large",
      "max-snippet": -1,
    },
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const jsonLd = generateWebsiteJsonLd();

  return (
    <html lang="ko" className={`${notoSansKr.variable} ${geistMono.variable}`}>
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className="antialiased" data-clarity-mask="true">
        <Providers>
          <Header />
          {children}
        </Providers>
        <Analytics />
        <SpeedInsights />
        <ThirdPartyAnalytics
          environment={process.env.VERCEL_ENV ?? process.env.NODE_ENV}
          clarityProjectId={process.env.NEXT_PUBLIC_CLARITY_PROJECT_ID}
          gaMeasurementId={process.env.NEXT_PUBLIC_GA_MEASUREMENT_ID}
        />
      </body>
    </html>
  );
}
