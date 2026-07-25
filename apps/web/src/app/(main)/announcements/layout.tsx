import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "공지사항 | 줍줍",
  description: "전북대학교 수강신청 빈자리 알림 서비스 '줍줍'의 주요 안내사항, 수강신청 팁 및 서비스 업데이트 공지를 확인하세요.",
  keywords: ["줍줍", "공지사항", "전북대", "수강신청 안내", "수강신청 일정"],
  alternates: {
    canonical: "https://zup-zup.com/announcements",
  },
  openGraph: {
    title: "공지사항 | 줍줍",
    description: "전북대학교 수강신청 빈자리 알림 서비스 '줍줍'의 주요 안내사항, 수강신청 팁 및 서비스 업데이트 공지를 확인하세요.",
    url: "https://zup-zup.com/announcements",
    siteName: "줍줍",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "공지사항 | 줍줍",
    description: "전북대학교 수강신청 빈자리 알림 서비스 '줍줍'의 주요 안내사항, 수강신청 팁 및 서비스 업데이트 공지를 확인하세요.",
  },
};

export default function AnnouncementsLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
