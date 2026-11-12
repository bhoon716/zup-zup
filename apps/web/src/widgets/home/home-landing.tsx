import type { ReactNode } from "react";
import { Bell, Calendar, Search } from "lucide-react";
import Link from "next/link";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";

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

const features = [
  {
    title: "실시간 여석 알림",
    description: "원하는 강의에 빈자리가 생기면 즉시 웹 푸시 알림으로 알려드립니다. 더 이상 모니터 앞에서 대기하지 마세요.",
    icon: Bell,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  {
    title: "스마트 시간표 관리",
    description: "복잡한 경우의 수를 고려하여 최적의 시간표를 시뮬레이션 해보세요. 중복되는 시간대와 총 이수 학점까지 실시간으로 체크합니다.",
    icon: Calendar,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  {
    title: "정밀 강의 검색",
    description: "강의명, 교수님은 물론 이수 구분, 학점, 강의 시간대 등 다양한 필터로 나에게 딱 맞는 강의를 쉽고 빠르게 찾으세요.",
    icon: Search,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
];

export function HomeLanding({ countdown, schedules, announcements }: {
  countdown: ReactNode;
  schedules: ReactNode;
  announcements: ReactNode;
}) {
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

      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <h2 className="sr-only">줍줍 주요 기능</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="bg-white dark:bg-primary/10 p-10 rounded-2xl shadow-sm border border-primary/5 flex flex-col gap-6 hover:shadow-xl transition-all group cursor-default"
              >
                <div className={cn("w-16 h-16 rounded-2xl flex items-center justify-center mb-2 group-hover:scale-110 transition-transform", feature.bgColor)}>
                  <feature.icon className={cn("w-8 h-8", feature.color)} />
                </div>
                <div>
                  <h3 className="text-2xl font-bold text-[#161118] mb-4">{feature.title}</h3>
                  <p className="text-[#161118]/60 leading-relaxed text-base font-medium">
                    {feature.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

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
      {schedules}
      {announcements}
    </main>
  );
}
