"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { Clock, CalendarDays, Zap } from "lucide-react";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";

export function DashboardCountdown() {
  const { data: upcomingSchedules } = useUpcomingSchedules();
  
  const [currentTime, setCurrentTime] = useState<Date | null>(null);
  const [ddayString, setDdayString] = useState("");
  const [timeLeftString, setTimeLeftString] = useState("");

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
        setTimeLeftString("즐거운 종강입니다! 🎉");
      } else {
        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        const diffHrs = Math.floor((diffMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const diffMins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
        const diffSecs = Math.floor((diffMs % (1000 * 60)) / 1000);

        setDdayString(`D-${diffDays}`);
        setTimeLeftString(`${diffHrs}시간 ${diffMins}분 ${diffSecs}초 남음`);
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

  // 오아시스 표준 서버 시각 포맷팅 (HH:mm:ss)
  const timeString = currentTime.toLocaleTimeString("ko-KR", {
    hour12: false,
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5, delay: 0.1 }}
      className="relative overflow-hidden bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl border border-gray-100 dark:border-gray-800 rounded-3xl p-5 shadow-floating flex flex-row items-center justify-between gap-6 w-full xl:w-[500px]"
    >
      {/* 백그라운드 오색 빛 그라디언트 이펙트 */}
      <div className="absolute inset-0 pointer-events-none -z-10">
        <div className="absolute -top-12 -left-12 w-24 h-24 bg-primary/10 dark:bg-primary/20 blur-2xl rounded-full" />
        <div className="absolute -bottom-12 -right-12 w-24 h-24 bg-indigo-500/10 dark:bg-indigo-500/20 blur-2xl rounded-full" />
      </div>

      {/* 1. 오아시스 서버 시계 영역 */}
      <div className="flex items-center gap-3.5 flex-1 min-w-0">
        <div className="w-10 h-10 rounded-2xl bg-primary/10 dark:bg-primary/20 flex items-center justify-center shrink-0">
          <Clock className="w-5 h-5 text-primary animate-pulse" />
        </div>
        <div className="flex flex-col min-w-0">
          <div className="flex items-center gap-1.5 text-[10px] font-black text-primary uppercase tracking-widest">
            <Zap className="w-2.5 h-2.5 text-primary animate-bounce" />
            OASIS SERVER TIME
          </div>
          <span className="text-2xl font-black text-gray-900 dark:text-gray-100 tracking-tight font-mono tabular-nums leading-tight">
            {timeString}
          </span>
        </div>
      </div>

      {/* 중앙 분할 선 */}
      <div className="w-px h-9 bg-gray-100 dark:bg-gray-800 shrink-0 hidden sm:block" />

      {/* 2. 종강 D-Day 카운트다운 영역 */}
      <div className="flex items-center gap-3.5 flex-1 min-w-0 sm:justify-end">
        <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center shrink-0">
          <CalendarDays className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
        </div>
        <div className="flex flex-col min-w-0">
          <div className="text-[10px] font-black text-indigo-600 dark:text-indigo-400 uppercase tracking-widest">
            종강 COUNTDOWN
          </div>
          <div className="flex items-baseline gap-1.5">
            <span className="text-xl font-black text-gray-900 dark:text-gray-100 tracking-tight whitespace-nowrap">
              {ddayString}
            </span>
            <span className="text-[10px] text-gray-400 dark:text-gray-500 font-bold font-mono tabular-nums truncate max-w-[140px] sm:max-w-none whitespace-nowrap">
              ({timeLeftString})
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
