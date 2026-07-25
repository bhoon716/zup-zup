import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "전북대 강의 검색 & 시간표 조회 | 줍줍",
  description: "전북대학교 전체 개설 강좌 검색, 이수구분·학점·평점별 맞춤 필터링, 여석 조회 및 시간표 시뮬레이션을 이용하세요.",
  keywords: ["전북대", "전북대학교", "강의검색", "시간표", "교양추천", "수강신청", "줍줍"],
  alternates: {
    canonical: "https://zup-zup.com/search",
  },
  openGraph: {
    title: "전북대 강의 검색 & 시간표 조회 | 줍줍",
    description: "전북대학교 전체 개설 강좌 검색, 이수구분·학점·평점별 맞춤 필터링, 여석 조회를 무료로 이용하세요.",
    url: "https://zup-zup.com/search",
    siteName: "줍줍",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "전북대 강의 검색 & 시간표 조회 | 줍줍",
    description: "전북대학교 전체 개설 강좌 검색, 이수구분·학점·평점별 맞춤 필터링, 여석 조회를 무료로 이용하세요.",
  },
};

export default function SearchLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
