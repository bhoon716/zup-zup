"use client";

import { Button } from "@/shared/ui/button";
import { motion } from "framer-motion";
import { Bell, Calendar, Search, Megaphone, Pin, ChevronRight } from "lucide-react";
import Link from "next/link";
import { cn } from "@/shared/lib/utils";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import { Loader2 } from "lucide-react";

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
    description: "원하는 강의에 빈자리가 생기면 즉시 푸시 알림과 문자로 알려드립니다. 더 이상 모니터 앞에서 대기하지 마세요.",
    icon: Bell,
    color: "text-primary",
    bgColor: "bg-primary/10",
  },
  {
    title: "스마트 시간표 관리",
    description: "복잡한 경우의 수를 고려하여 최적의 시간표를 시뮬레이션 해보세요. 중복 시간대와 이동 동선까지 체크합니다.",
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
  const { data: upcomingSchedules, isLoading: isScheduleLoading } = useUpcomingSchedules();
  const { data: announcements, isLoading: isAnnouncementLoading } = useAnnouncements();
  
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
            className="text-4xl md:text-5xl lg:text-7xl font-bold tracking-tight text-[#161118] mb-8 leading-[1.1]"
          >
            수강신청 빈자리,<br/>
            <span className="text-primary">이제 알림으로 잡으세요.</span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-lg md:text-xl text-[#161118]/60 mb-12 max-w-2xl mx-auto leading-relaxed"
          >
            하루 종일 새로고침만 누르고 계신가요? 전북대 수강신청 도우미가 빈자리가 생기면 가장 먼저 알려드립니다. 스마트한 시간표 관리까지 한 번에 해결하세요.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 w-full sm:w-auto"
          >
            <Link href="/login" className="w-full sm:w-auto">
              <Button className="w-full px-10 py-7 rounded-full bg-primary text-white font-bold text-lg hover:bg-primary/90 transition-all shadow-xl shadow-primary/30 h-auto">
                지금 시작하기
              </Button>
            </Link>
            <Link href="/search" className="w-full sm:w-auto">
              <Button variant="outline" className="w-full px-10 py-7 rounded-full bg-white text-primary border-2 border-primary/20 font-bold text-lg hover:bg-primary/5 transition-all h-auto">
                강의 먼저 둘러보기
              </Button>
            </Link>
          </motion.div>
        </div>
      </section>

      {/* Features Section */}
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

      {/* Upcoming Schedules Section */}
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="mb-8">
            <h3 className="text-2xl md:text-3xl font-bold text-[#161118] flex items-center gap-3">
              <Calendar className="text-indigo-500 w-8 h-8" />
              학사 및 수강신청 주요 일정
            </h3>
            <p className="mt-2 text-[#161118]/60 text-sm md:text-base font-medium">놓치지 말아야 할 전북대 수강신청 일정을 확인하세요.</p>
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
                  className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow hover:border-indigo-100"
                >
                  <div>
                    <div className="flex flex-col gap-2 mb-4">
                      <div className="flex items-center justify-between">
                        <span className={cn(
                          "px-3 py-1 rounded-full text-xs font-black tracking-widest uppercase",
                          schedule.dDay === "D-Day" ? "bg-red-50 text-red-600" : "bg-indigo-50 text-indigo-600"
                        )}>
                          {schedule.dDay}
                        </span>
                      </div>
                    </div>
                    <h4 className="text-[1.1rem] font-bold text-[#161118] leading-snug break-keep mb-1">
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

      {/* Announcements Section */}
      <section className="pb-32">
        <div className="max-w-[1200px] mx-auto px-6">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h3 className="text-2xl md:text-3xl font-bold text-[#161118] flex items-center gap-3 mb-2">
                <Megaphone className="text-primary w-8 h-8 fill-primary/10" />
                공지사항
              </h3>
              <p className="text-[#161118]/60 text-sm md:text-base font-medium">서비스 및 학사의 주요 공지사항을 확인하세요.</p>
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
                          <span className="inline-flex shrink-0 items-center gap-1 rounded-md border border-indigo-100 bg-indigo-50 px-2 py-0.5 text-[10px] font-black text-indigo-600 dark:border-indigo-900/50 dark:bg-indigo-950/50">
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
