"use client";

import { Loader2, Sparkles } from "lucide-react";
import { motion } from "framer-motion";
import { RecentNotifications } from "./recent-notifications";
import { DashboardAnnouncements } from "./dashboard-announcements";
import { DashboardTimetable } from "./dashboard-timetable";
import { DashboardDDayBlock } from "./dashboard-dday-block";
import { DashboardCountdown } from "./dashboard-countdown";
import { useDashboardSnapshot } from "./hooks/useDashboard";
import { Button } from "@/shared/ui/button";

/**
 * 사용자의 개인 대시보드를 렌더링하는 메인 컴포넌트입니다.
 * 환영 메시지, 수강신청 일정(세로), 공지사항, 대표 시간표, 최근 알림 및 찜한 강의 요약을 포함합니다.
 */
export function Dashboard() {
  const { data: snapshot, isLoading, isError, refetch } = useDashboardSnapshot();

  if (isLoading || !snapshot) {
    if (isError) {
      return (
        <main className="grow flex items-center justify-center py-8 md:py-12 px-4 sm:px-6 lg:px-8 max-w-[1700px] mx-auto w-full">
          <div className="rounded-3xl border border-red-100 bg-white p-8 text-center shadow-sm">
            <p className="text-sm font-bold text-slate-900">대시보드 데이터를 불러오지 못했습니다.</p>
            <p className="mt-2 text-xs text-slate-500">잠시 후 다시 시도해 주세요.</p>
            <Button className="mt-4" onClick={() => void refetch()}>
              다시 시도
            </Button>
          </div>
        </main>
      );
    }

    return (
      <main className="grow flex items-center justify-center py-8 md:py-12 px-4 sm:px-6 lg:px-8 max-w-[1700px] mx-auto w-full">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </main>
    );
  }

  const { user, notifications, primaryTimetable, upcomingSchedules, announcements } = snapshot;

  return (
    <main className="grow py-8 md:py-12 px-4 sm:px-6 lg:px-8 max-w-[1700px] mx-auto w-full">
      {/* 환영 헤더 섹션: 좌측 인사말 + 우측 종강 D-Day 바 (오른쪽 벽 정렬) */}
      <div className="flex flex-col xl:flex-row justify-between items-start xl:items-end mb-8 gap-6 w-full">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="min-w-0"
        >
          <div className="flex items-center gap-2 text-primary uppercase tracking-[0.2em] text-[10px] font-black mb-2">
            <div className="w-8 h-px bg-primary/30" />
            <Sparkles className="w-3 h-3" />
            다시 만나서 반가워요
          </div>
          <h1 className="text-4xl md:text-5xl font-black text-gray-900 dark:text-gray-100 leading-tight">
            안녕하세요, <span className="text-primary">{user.name}님!</span>
          </h1>
        </motion.div>

        <div className="w-full xl:w-auto flex justify-end shrink-0">
          <DashboardCountdown upcomingSchedules={upcomingSchedules} />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* 좌측: 대표 시간표 (Lg 기준 7/12) - 컴팩트 사이즈 조정 */}
        <div className="lg:col-span-7 flex flex-col gap-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="h-full"
          >
            <DashboardTimetable timetable={primaryTimetable} />
          </motion.div>
        </div>

        {/* 우측: 알림 및 공지사항 (Lg 기준 5/12) - 정보 스트림 집중 */}
        <div className="lg:col-span-5 flex flex-col gap-6">
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            <DashboardDDayBlock upcomingSchedules={upcomingSchedules} />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.4 }}
          >
            <RecentNotifications notifications={notifications || []} />
          </motion.div>
          
          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.5 }}
          >
            <DashboardAnnouncements announcements={announcements} />
          </motion.div>
        </div>
      </div>
    </main>
  );
}
