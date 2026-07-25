import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "내 시간표 시뮬레이터 | 줍줍",
  description: "전북대학교 시간표 시뮬레이션, 강의 중복 체크, 학점 통계 및 여석 알림 설정을 손쉽게 이용하세요.",
  keywords: ["줍줍", "시간표", "전북대 시간표", "시간표 시뮬레이터", "수강신청"],
  alternates: {
    canonical: "https://zup-zup.com/timetable",
  },
  openGraph: {
    title: "내 시간표 시뮬레이터 | 줍줍",
    description: "전북대학교 시간표 시뮬레이션, 강의 중복 체크, 학점 통계 및 여석 알림 설정을 손쉽게 이용하세요.",
    url: "https://zup-zup.com/timetable",
    siteName: "줍줍",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "내 시간표 시뮬레이터 | 줍줍",
    description: "전북대학교 시간표 시뮬레이션, 강의 중복 체크, 학점 통계 및 여석 알림 설정을 손쉽게 이용하세요.",
  },
};

export default function TimetableLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
