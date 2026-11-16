"use client";

import { Button } from "@/shared/ui/button";
import { motion } from "framer-motion";
import { ArrowRight, Bell, Calendar, MessageSquare, Search } from "lucide-react";
import Link from "next/link";
import { cn } from "@/shared/lib/utils";
import { DashboardCountdown } from "./dashboard-countdown";
import { VacancyNoticePreview } from "./vacancy-notice-preview";
import styles from "./vacancy-notice.module.css";

const statistics = [
  { label: "누적 사용자", minimum: 700, unit: "명" },
  { label: "보낸 빈자리 알림", minimum: 7000, unit: "개" },
];
const countFormatter = new Intl.NumberFormat("ko-KR");

const everyTimeReviews = [
  {
    id: 1,
    content: "덕분에 계절 자리 잡았습니다 감사합니다",
    createdAt: "06/12 10:15",
    source: "에브리타임",
  },
  {
    id: 2,
    content: "너무너무 감사합니다… 알림 뜨는거 보고 바로 들어가면 자리 있더라고요!! 덕분에 시간표 예쁘게 만들었어요 감사합니다!!🥹🫶🏻",
    createdAt: "06/13 14:02",
    source: "에브리타임",
  },
  {
    id: 3,
    content: "덕분에 전공 빈자리 잡았습니다 ㅜㅜ 감사합니다!!",
    createdAt: "06/14 09:30",
    source: "에브리타임",
  },
  {
    id: 4,
    content: "수강신청 실패해서 한 과목 부족했는데 알림 덕분에 안전하게 잡았어요!! 진짜 유용하게 잘 쓰고 있습니다ㅠㅠ",
    createdAt: "06/14 11:20",
    source: "에브리타임",
  },
  {
    id: 5,
    content: "알림 속도 엄청 빨라요! 뜨자마자 바로 들어가서 겨우 성공했습니다.. 개발자님 복 받으세요!",
    createdAt: "06/15 09:05",
    source: "에브리타임",
  },
  {
    id: 6,
    content: "알림 덕분에 졸업 필수 전공 드디어 잡았어요ㅠㅠ 진짜 은인이십니다 감사합니다!!",
    createdAt: "06/15 14:50",
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

export function HomeLanding() {
  return (
    <div className="flex flex-col">
      <section className="overflow-hidden pt-9 pb-12 sm:pt-12 md:pt-10 md:pb-14 lg:pt-14">
        <div className={cn(styles.heroGrid, "mx-auto w-full max-w-[1200px] px-6")}>
          <div className="min-w-0 text-center md:text-left" data-testid="landing-hero-copy">
            <h1 className={cn(styles.heroTitle, "mb-5 break-keep font-bold leading-[1.12] tracking-tight text-foreground sm:mb-6")}>
              수강신청 빈자리,<br />
              <span className="text-primary">이제 알림으로 잡으세요.</span>
            </h1>

            <p className="mx-auto mb-6 max-w-[34rem] text-base leading-relaxed break-keep text-foreground/65 sm:mb-8 sm:text-lg md:mx-0 lg:text-xl">
              <span className="block text-balance md:text-wrap">듣고 싶은 강의가 마감됐다면 알림을 켜두세요.</span>
              <span className="block text-balance md:text-wrap">자리가 나면 줍줍이 휴대폰으로 알려드려요.</span>
            </p>

            <div className="mb-4 flex justify-center empty:hidden md:justify-start">
              <DashboardCountdown animateOnMount />
            </div>

            <div className="flex flex-col items-stretch justify-center gap-3 sm:flex-row sm:items-center sm:gap-4 md:justify-start">
              <Button asChild className="h-auto rounded-full bg-primary py-3.5 text-base font-bold text-primary-foreground shadow-lg shadow-primary/20 has-[>svg]:px-8 sm:py-4">
                <Link href="/login">
                  지금 시작하기 <ArrowRight aria-hidden="true" />
                </Link>
              </Button>
              <Button asChild variant="outline" className="h-auto rounded-full border-2 border-primary/20 bg-card py-3.5 text-base font-bold text-primary hover:bg-primary/5 hover:text-primary has-[>svg]:px-8 sm:py-4">
                <Link href="/search">
                  <Search aria-hidden="true" /> 강의 먼저 둘러보기
                </Link>
              </Button>
            </div>
          </div>

          <VacancyNoticePreview />
        </div>
      </section>

      {           }
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {features.map((feature, idx) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: idx * 0.1 }}
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
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {                   }
      <section className="pb-12 md:pb-16">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4">
            <div>
              <h3 className="text-2xl md:text-3xl font-bold text-[#161118] dark:text-white flex items-center gap-3">
                생생한 사용자 후기
              </h3>
              <p className="mt-2 text-[#161118]/60 dark:text-slate-400 text-sm md:text-base font-medium">
                줍줍 서비스를 이용한 대학생들의 실제 이용 후기입니다.
              </p>
            </div>
          </div>

          {everyTimeReviews.length === 0 ? (
            <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50/50 dark:border-slate-800 dark:bg-primary/5 py-16 px-4 text-center">
              <MessageSquare className="mx-auto h-12 w-12 text-slate-300 dark:text-slate-700 mb-4" />
              <p className="text-base font-bold text-slate-800 dark:text-slate-200">아직 등록된 후기가 없습니다.</p>
              <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">첫 번째 후기를 기다리고 있습니다.</p>
            </div>
          ) : (
            <div className="relative w-full overflow-hidden py-4">
              {                     }
              <div className="absolute left-0 top-0 bottom-0 w-16 md:w-32 bg-gradient-to-r from-[#fafafa] dark:from-black to-transparent z-10 pointer-events-none" />
              <div className="absolute right-0 top-0 bottom-0 w-16 md:w-32 bg-gradient-to-l from-[#fafafa] dark:from-black to-transparent z-10 pointer-events-none" />

              <div className="flex w-max gap-6 animate-marquee hover:[animation-play-state:paused]">
                {             }
                {everyTimeReviews.map((review) => (
                  <div
                    key={`first-${review.id}`}
                    className="w-[320px] shrink-0 bg-white dark:bg-primary/5 p-6 rounded-2xl border border-slate-100 dark:border-primary/5 shadow-xs hover:shadow-lg transition-all cursor-default flex flex-col justify-between h-[180px]"
                  >
                    <div className="flex-1 flex flex-col justify-between">
                      <div>
                        {                   }
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <div className="text-xs font-bold text-slate-800 dark:text-slate-200">익명</div>
                          </div>
                        </div>

                        {           }
                        <p className="text-[13px] leading-relaxed text-slate-600 dark:text-slate-300 font-semibold break-all line-clamp-3">
                          {review.content}
                        </p>
                      </div>

                      {                   }
                      <div className="mt-4 flex justify-end">
                        <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold">
                          {review.source}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}

                {                          }
                {everyTimeReviews.map((review) => (
                  <div
                    key={`second-${review.id}`}
                    className="w-[320px] shrink-0 bg-white dark:bg-primary/5 p-6 rounded-2xl border border-slate-100 dark:border-primary/5 shadow-xs hover:shadow-lg transition-all cursor-default flex flex-col justify-between h-[180px]"
                  >
                    <div className="flex-1 flex flex-col justify-between">
                      <div>
                        {                   }
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <div className="text-xs font-bold text-slate-800 dark:text-slate-200">익명</div>
                          </div>
                        </div>

                        {           }
                        <p className="text-[13px] leading-relaxed text-slate-600 dark:text-slate-300 font-semibold break-all line-clamp-3">
                          {review.content}
                        </p>
                      </div>

                      {                   }
                      <div className="mt-4 flex justify-end">
                        <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold">
                          {review.source}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              <style>{`
                @keyframes marquee-slide {
                  0% { transform: translateX(0); }
                  100% { transform: translateX(calc(-50% - 12px)); }
                }
                .animate-marquee {
                  animation: marquee-slide 40s linear infinite;
                }
              `}</style>
            </div>
          )}
        </div>
      </section>

      <section aria-label="서비스 이용 통계" className="mx-auto w-full max-w-[1200px] px-6 pb-12 md:pb-16">
        <dl className="grid grid-cols-2 divide-x rounded-2xl border bg-card">
          {statistics.map(({ label, minimum, unit }) => (
            <div key={label} className="px-4 py-5 sm:px-7 sm:py-6">
              <dt className="break-keep text-sm text-muted-foreground">{label}</dt>
              <dd className="mt-1.5 text-2xl font-bold tabular-nums text-primary sm:text-4xl">
                {countFormatter.format(minimum)}+<span className="ml-1 text-sm font-medium">{unit}</span>
              </dd>
            </div>
          ))}
        </dl>
        <p className="mt-2.5 text-right text-xs text-muted-foreground">
          <time dateTime="2026-11-16">2026년 11월 16일</time> 기준
        </p>
      </section>
    </div>
  );
}
