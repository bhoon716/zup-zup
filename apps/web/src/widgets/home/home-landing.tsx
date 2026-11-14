import type { ReactNode } from "react";
import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { LandingWalkthrough } from "./landing-walkthrough";

const everyTimeReviews = [
  {
    id: 1,
    content: "덕분에 계절 자리 잡았습니다 감사합니다",
    source: "에브리타임",
  },
  {
    id: 2,
    content: "너무너무 감사합니다… 알림 뜨는거 보고 바로 들어가면 자리 있더라고요!! 덕분에 시간표 예쁘게 만들었어요 감사합니다!!🥹🫶🏻",
    source: "에브리타임",
  },
  {
    id: 3,
    content: "덕분에 전공 빈자리 잡았습니다 ㅜㅜ 감사합니다!!",
    source: "에브리타임",
  },
  {
    id: 4,
    content: "수강신청 실패해서 한 과목 부족했는데 알림 덕분에 안전하게 잡았어요!! 진짜 유용하게 잘 쓰고 있습니다ㅠㅠ",
    source: "에브리타임",
  },
  {
    id: 5,
    content: "알림 속도 엄청 빨라요! 뜨자마자 바로 들어가서 겨우 성공했습니다.. 개발자님 복 받으세요!",
    source: "에브리타임",
  },
  {
    id: 6,
    content: "알림 덕분에 졸업 필수 전공 드디어 잡았어요ㅠㅠ 진짜 은인이십니다 감사합니다!!",
    source: "에브리타임",
  },
];

export function HomeLanding({ countdown }: { countdown: ReactNode }) {
  return (
    <main className="flex flex-col bg-[#fafafa] dark:bg-black">
      <section className="relative pt-16 pb-24 md:pt-32 md:pb-40 overflow-hidden flex items-center">
        <div className="max-w-[1200px] mx-auto px-6 flex flex-col items-center text-center relative z-10 w-full">
          <h1
            className="text-4xl md:text-5xl lg:text-7xl font-bold tracking-tight text-[#161118] mb-8 leading-[1.1]"
          >
            전북대 수강신청 빈자리,<br/>
            <span className="text-primary">이제 알림으로 잡으세요.</span>
          </h1>

          <p
            className="text-lg md:text-xl text-[#161118]/60 mb-12 max-w-2xl mx-auto leading-relaxed"
          >
            하루 종일 새로고침만 누르고 계신가요? 전북대 수강신청 빈자리 알림 서비스 &apos;줍줍&apos;이 빈자리가 생기면 가장 먼저 알려드립니다. 스마트한 시간표 관리까지 한 번에 해결하세요.
          </p>

          <div className="mb-8 w-full flex justify-center">
            {countdown}
          </div>

          <div
            className="flex flex-col sm:flex-row items-center justify-center gap-4 w-full sm:w-auto"
          >
            <Button asChild className="sm:w-auto h-auto w-full rounded-full bg-primary px-8 py-5 text-base font-bold text-white shadow-xl shadow-primary/30 transition-all hover:bg-primary/90">
              <Link href="/login">
                지금 시작하기
              </Link>
            </Button>
            <Button asChild variant="outline" className="sm:w-auto h-auto w-full rounded-full border-2 border-primary/20 bg-white px-8 py-5 text-base font-bold text-primary transition-all hover:bg-primary/5">
              <Link href="/search">
                강의 먼저 둘러보기
              </Link>
            </Button>
          </div>
        </div>
      </section>

      <LandingWalkthrough />

      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-[#161118] dark:text-white flex items-center gap-3">
                생생한 사용자 후기
              </h2>
              <p className="mt-2 text-[#161118]/60 dark:text-slate-400 text-sm md:text-base font-medium">
                줍줍 서비스를 이용한 대학생들의 실제 이용 후기입니다.
              </p>
            </div>
          </div>

          <ul
            aria-label="사용자 후기 목록"
            tabIndex={0}
            className="custom-scrollbar flex snap-x gap-6 overflow-x-auto rounded-2xl py-4 focus-visible:outline-2 focus-visible:outline-primary"
          >
            {everyTimeReviews.map((review) => (
              <li
                key={review.id}
                className="flex min-h-44 w-[min(320px,85%)] shrink-0 snap-start flex-col justify-between rounded-2xl border border-slate-100 bg-white p-6 shadow-xs dark:border-primary/5 dark:bg-primary/5"
              >
                <div>
                  <p className="mb-3 text-xs font-bold text-slate-800 dark:text-slate-200">익명</p>
                  <p className="break-all text-[13px] font-semibold leading-relaxed text-slate-600 dark:text-slate-300">
                    {review.content}
                  </p>
                </div>
                <p className="mt-4 text-right text-[10px] font-bold text-slate-500 dark:text-slate-400">
                  {review.source}
                </p>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </main>
  );
}
