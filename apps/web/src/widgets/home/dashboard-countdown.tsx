"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { CalendarDays } from "lucide-react";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";

export function DashboardCountdown() {
  const { data: upcomingSchedules } = useUpcomingSchedules();
  
  const [currentTime, setCurrentTime] = useState<Date | null>(null);
  const [ddayString, setDdayString] = useState("");

  useEffect(() => {
    // SSR 수화 불일치(Hydration Mismatch) 방지를 위해 마운트 후 비동기 셋업
    const handleInitialSetup = () => {
      const now = new Date();
      setCurrentTime(now);

      // 1. 백엔드 일정 목록에서 '종강' 키워드가 들어간 일정을 검색
      const endOfSemesterEvent = upcomingSchedules?.find((s) => 
        s.scheduleType.includes("종강")
      );

      // 2. 동적 타겟 날짜 판별 (없을 시 기본 폴백인 2026-06-19T18:00:00 사용)
      let targetDate = new Date("2026-06-19T18:00:00");
      if (endOfSemesterEvent) {
        const timePart = endOfSemesterEvent.endTime ? endOfSemesterEvent.endTime : "18:00:00";
        // 'YYYY-MM-DDTHH:mm:ss' 포맷 조립
        targetDate = new Date(`${endOfSemesterEvent.endDate}T${timePart}`);
      }

      // 3. 종강 D-Day 계산
      const diffMs = targetDate.getTime() - now.getTime();
      if (diffMs <= 0) {
        setDdayString("D-Day");
      } else {
        const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
        setDdayString(`D-${diffDays}`);
      }
    };

    // 비동기 틱으로 스케줄링하여 카스케이딩 렌더링 차단 (Lint 해결)
    const initTimer = setTimeout(handleInitialSetup, 0);
    const intervalTimer = setInterval(handleInitialSetup, 1000);

    return () => {
      clearTimeout(initTimer);
      clearInterval(intervalTimer);
    };
  }, [upcomingSchedules]);

  if (!currentTime) {
    return (
      <div className="h-[90px] w-full xl:w-[480px] bg-white dark:bg-gray-900 rounded-3xl animate-pulse" />
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5, delay: 0.1 }}
      className="relative overflow-hidden bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl border border-gray-100 dark:border-gray-800 rounded-3xl p-5 shadow-floating flex flex-row items-center justify-between gap-4 w-full max-w-[420px] mx-auto"
    >
      {/* 백그라운드 오색 빛 그라디언트 이펙트 */}
      <div className="absolute inset-0 pointer-events-none -z-10">
        <div className="absolute -top-12 -left-12 w-24 h-24 bg-indigo-500/10 dark:bg-indigo-500/20 blur-2xl rounded-full" />
        <div className="absolute -bottom-12 -right-12 w-24 h-24 bg-primary/10 dark:bg-primary/20 blur-2xl rounded-full" />
      </div>

      {/* 종강 D-Day 카운트다운 영역 (아이콘 좌측, 날짜 우측 끝 배치) */}
      <div className="flex items-center gap-4 min-w-0 w-full justify-between">
        <div className="w-11 h-11 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center shrink-0">
          <CalendarDays className="w-6 h-6 text-indigo-600 dark:text-indigo-400" />
        </div>
        <div className="flex flex-col min-w-0 items-end text-right pr-1">
          <div className="text-[10px] font-black text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
            종강 COUNTDOWN
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-black text-gray-900 dark:text-gray-100 tracking-tight whitespace-nowrap">
              {ddayString}
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
