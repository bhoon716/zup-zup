"use client";

import { Button } from "@/shared/ui/button";
import { motion } from "framer-motion";
import { Bell, Calendar, Search, Megaphone, Pin, ChevronRight, MessageSquare } from "lucide-react";
import Link from "next/link";
import { cn } from "@/shared/lib/utils";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import { Loader2 } from "lucide-react";
import { DashboardCountdown } from "./dashboard-countdown";
import { useClientReady } from "@/shared/lib/use-client-ready";

/**
 * 에브리타임 실제 사용자 후기 리스트입니다.
 */
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

/**
 * 날짜를 포맷팅하는 유틸리티 함수입니다.
 */
const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

/**
 * 서비스의 주요 특징을 정의하는 상수 리스트입니다.
 */
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

/**
 * 인덱스 페이지에서 서비스를 소개하고 핵심 기능을 보여주는 랜딩용 섹션입니다.
 */
export function HomeLanding() {
  const isClientReady = useClientReady();

  const { data: upcomingSchedules, isLoading: upcomingSchedulesLoading } = useUpcomingSchedules(isClientReady);
  const { data: announcements, isLoading: announcementsLoading } = useAnnouncements({ enabled: isClientReady });
  const isScheduleLoading = !isClientReady || upcomingSchedulesLoading;
  const isAnnouncementLoading = !isClientReady || announcementsLoading;
  
  const latestAnnouncements = (announcements ?? []).slice(0, 4);

  return (
    <div className="flex flex-col">
      {/* Hero Section: 서비스의 핵심 가치 전달 */}
      <section className="relative pt-16 pb-24 md:pt-32 md:pb-40 overflow-hidden flex items-center">
        <div className="max-w-[1200px] mx-auto px-6 flex flex-col items-center text-center relative z-10 w-full">
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-4xl md:text-5xl lg:text-7xl font-bold tracking-tight text-foreground mb-8 leading-[1.1]"
          >
            수강신청 빈자리,<br/>
            <span className="text-primary">이제 알림으로 잡으세요.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-lg md:text-xl text-muted-foreground mb-12 max-w-2xl mx-auto leading-relaxed"
          >
            하루 종일 새로고침만 누르고 계신가요? 수강신청 빈자리 알림 서비스 &apos;줍줍&apos;이 빈자리가 생기면 가장 먼저 알려드립니다. 스마트한 시간표 관리까지 한 번에 해결하세요.
          </motion.p>

          <div className="mb-8 w-full flex justify-center">
            <DashboardCountdown upcomingSchedules={upcomingSchedules} suppressFetch={!isClientReady || isScheduleLoading} />
          </div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 w-full sm:w-auto"
          >
            <Link href="/login" className="w-full sm:w-auto">
              <Button className="h-auto w-full rounded-full bg-primary px-8 py-5 text-base font-bold text-white shadow-xl shadow-primary/30 transition-all hover:bg-primary/90">
                지금 시작하기
              </Button>
            </Link>
            <Link href="/search" className="w-full sm:w-auto">
              <Button variant="outline" className="h-auto w-full rounded-full border-2 border-primary/20 bg-white px-8 py-5 text-base font-bold text-primary transition-all hover:bg-primary/5">
                강의 먼저 둘러보기
              </Button>
            </Link>
          </motion.div>
        </div>
      </section>

      {/* 특징 섹션 */}
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
                  <h3 className="text-2xl font-bold text-foreground mb-4">{feature.title}</h3>
                  <p className="text-muted-foreground leading-relaxed text-base font-medium">
                    {feature.description}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* 생생한 사용자 후기 섹션 */}
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-10 flex flex-col md:flex-row md:items-end md:justify-between gap-4">
            <div>
              <h3 className="text-2xl md:text-3xl font-bold text-foreground flex items-center gap-3">
                생생한 사용자 후기
              </h3>
              <p className="mt-2 text-muted-foreground text-sm md:text-base font-medium">
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
              {/* 좌우 그라데이션 페이드 효과 */}
              <div className="absolute left-0 top-0 bottom-0 w-16 md:w-32 bg-gradient-to-r from-[#fafafa] dark:from-black to-transparent z-10 pointer-events-none" />
              <div className="absolute right-0 top-0 bottom-0 w-16 md:w-32 bg-gradient-to-l from-[#fafafa] dark:from-black to-transparent z-10 pointer-events-none" />

              <div className="flex w-max gap-6 animate-marquee hover:[animation-play-state:paused]">
                {/* 첫 번째 세트 */}
                {everyTimeReviews.map((review) => (
                  <div
                    key={`first-${review.id}`}
                    className="w-[320px] shrink-0 bg-white dark:bg-primary/5 p-6 rounded-2xl border border-slate-100 dark:border-primary/5 shadow-xs hover:shadow-lg transition-all cursor-default flex flex-col justify-between h-[180px]"
                  >
                    <div className="flex-1 flex flex-col justify-between">
                      <div>
                        {/* 카드 헤더 (익명 정보) */}
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <div className="text-xs font-bold text-slate-800 dark:text-slate-200">익명</div>
                          </div>
                        </div>

                        {/* 카드 본문 */}
                        <p className="text-[13px] leading-relaxed text-slate-600 dark:text-slate-300 font-semibold break-all line-clamp-3">
                          {review.content}
                        </p>
                      </div>

                      {/* 카드 푸터 (출처 표시) */}
                      <div className="mt-4 flex justify-end">
                        <span className="text-[10px] text-slate-400 dark:text-slate-500 font-bold">
                          {review.source}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}

                {/* 무한 루프를 위한 두 번째 세트 복제 */}
                {everyTimeReviews.map((review) => (
                  <div
                    key={`second-${review.id}`}
                    className="w-[320px] shrink-0 bg-white dark:bg-primary/5 p-6 rounded-2xl border border-slate-100 dark:border-primary/5 shadow-xs hover:shadow-lg transition-all cursor-default flex flex-col justify-between h-[180px]"
                  >
                    <div className="flex-1 flex flex-col justify-between">
                      <div>
                        {/* 카드 헤더 (익명 정보) */}
                        <div className="flex items-center justify-between mb-3">
                          <div>
                            <div className="text-xs font-bold text-slate-800 dark:text-slate-200">익명</div>
                          </div>
                        </div>

                        {/* 카드 본문 */}
                        <p className="text-[13px] leading-relaxed text-slate-600 dark:text-slate-300 font-semibold break-all line-clamp-3">
                          {review.content}
                        </p>
                      </div>

                      {/* 카드 푸터 (출처 표시) */}
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

      {/* 다가오는 일정 섹션 */}
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-8">
            <h3 className="text-2xl md:text-3xl font-bold text-foreground flex items-center gap-3">
              <Calendar className="text-primary w-8 h-8" />
              학사 및 수강신청 주요 일정
            </h3>
            <p className="mt-2 text-muted-foreground text-sm md:text-base font-medium">놓치지 말아야 할 전북대 수강신청 일정을 확인하세요.</p>
          </div>

          {isScheduleLoading ? (
            <div className="flex h-32 items-center justify-center">
              <Loader2 className="h-6 w-6 animate-spin text-slate-300" />
            </div>
          ) : upcomingSchedules?.length === 0 ? (
            <div className="flex xl:col-span-12 h-32 items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
              <p className="text-sm font-medium text-slate-500">현재 예정된 주요 일정이 없습니다.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {upcomingSchedules?.map((schedule, idx) => (
                <motion.div
                  key={schedule.id}
                  initial={{ opacity: 0, y: 10 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.4, delay: idx * 0.1 }}
                  className="bg-card p-6 rounded-2xl shadow-sm border border-border flex flex-col justify-between hover:shadow-md transition-shadow hover:border-primary/30"
                >
                  <div>
                    <div className="flex flex-col gap-2 mb-4">
                      <div className="flex items-center justify-between">
                        <span className={cn(
                          "px-3 py-1 rounded-full text-xs font-black tracking-widest uppercase",
                          schedule.dDay === "D-Day" ? "bg-red-50 text-red-600" : "bg-primary/10 text-primary"
                        )}>
                          {schedule.dDay}
                        </span>
                      </div>
                    </div>
                    <h4 className="text-[1.1rem] font-bold text-foreground leading-snug break-keep mb-1">
                      {schedule.scheduleType}
                    </h4>
                  </div>
                  <div className="mt-6 pt-4 border-t border-slate-100 flex flex-col gap-1 text-[13px] font-semibold text-slate-500">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">일자</span>
                      <span>
                        {schedule.startDate.substring(5).replace("-", ".")} ~ {schedule.endDate.substring(5).replace("-", ".")}
                      </span>
                    </div>
                    {(schedule.startTime || schedule.endTime) && (
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400">시간</span>
                        <span>
                          {schedule.startTime ? schedule.startTime : "--:--"} ~ {schedule.endTime ? schedule.endTime : "--:--"}
                        </span>
                      </div>
                    )}
                  </div>
                </motion.div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* 공지사항 섹션 */}
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h3 className="text-2xl md:text-3xl font-bold text-foreground flex items-center gap-3 mb-2">
                <Megaphone className="text-primary w-8 h-8 fill-primary/10" />
                공지사항
              </h3>
              <p className="text-muted-foreground text-sm md:text-base font-medium">서비스 및 학사의 주요 공지사항을 확인하세요.</p>
            </div>
            <Link href="/announcements">
              <Button variant="link" className="text-primary font-bold flex items-center gap-1 hover:no-underline group p-0">
                전체보기 <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </Button>
            </Link>
          </div>

          {isAnnouncementLoading ? (
            <div className="flex h-36 items-center justify-center text-sm text-slate-500">
              <Loader2 className="mr-2 h-6 w-6 animate-spin" />
            </div>
          ) : latestAnnouncements.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
              등록된 공지사항이 없습니다.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              {latestAnnouncements.map((item, idx) => (
                <Link key={item.id} href={`/announcements/${item.id}`}>
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95 }}
                    whileInView={{ opacity: 1, scale: 1 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.4, delay: idx * 0.05 }}
                    className="bg-white dark:bg-primary/5 p-6 rounded-2xl border border-primary/5 shadow-sm flex flex-col gap-3 hover:bg-primary/5 transition-all cursor-pointer group hover:border-primary/20 h-full"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="flex min-w-0 items-center gap-2.5">
                        {item.pinned ? (
                          <span className="inline-flex shrink-0 items-center gap-1 rounded-md border border-primary/30 bg-primary/10 px-2 py-0.5 text-[10px] font-black text-primary">
                            <Pin className="h-3 w-3" />
                            고정
                          </span>
                        ) : null}
                        <h4 className="truncate text-base md:text-lg font-bold text-gray-800 transition-colors group-hover:text-primary dark:text-gray-200">
                          {item.title}
                        </h4>
                      </div>
                      <span className="shrink-0 text-xs font-medium text-gray-400">{formatDate(item.createdAt)}</span>
                    </div>
                    <p className="line-clamp-2 text-sm text-slate-500">{item.previewContent || "-"}</p>
                  </motion.div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
