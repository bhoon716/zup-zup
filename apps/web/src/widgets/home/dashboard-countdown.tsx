"use client";

import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { CalendarDays } from "lucide-react";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";
import type { ScheduleResponse } from "@/shared/types/api";

export function DashboardCountdown({ upcomingSchedules }: { upcomingSchedules?: ScheduleResponse[] } = {}) {
  const { data: fetchedUpcomingSchedules } = useUpcomingSchedules(upcomingSchedules === undefined);
  const schedules = upcomingSchedules ?? fetchedUpcomingSchedules;
  const [ddayString, setDdayString] = useState("");

  useEffect(() => {
    const handleInitialSetup = () => {
      const now = new Date();
      const endOfSemesterEvent = schedules?.find((s) =>
        s.scheduleType.includes("종강")
      );

      let targetDate = new Date("2026-06-19T18:00:00");
      if (endOfSemesterEvent) {
        const timePart = endOfSemesterEvent.endTime ? endOfSemesterEvent.endTime : "18:00:00";
        targetDate = new Date(`${endOfSemesterEvent.endDate}T${timePart}`);
      }

      const diffMs = targetDate.getTime() - now.getTime();
      if (diffMs <= 0) {
        setDdayString("D-Day");
      } else {
        const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
        setDdayString(`D-${diffDays}`);
      }
    };

    handleInitialSetup();
    const intervalTimer = setInterval(handleInitialSetup, 60000); // 1분 단위 갱신

    return () => clearInterval(intervalTimer);
  }, [schedules]);

  if (!ddayString) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="hidden md:flex items-center gap-2 bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-100/50 dark:border-indigo-900/30 rounded-xl px-3 h-9 select-none shrink-0"
    >
      <CalendarDays className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400 shrink-0" />
      <span className="text-[10px] font-black text-indigo-600 dark:text-indigo-400 tracking-tight whitespace-nowrap">
        앞으로 종강까지
      </span>
      <span className="text-sm font-black text-gray-900 dark:text-gray-100 font-mono tracking-tight whitespace-nowrap">
        {ddayString}
      </span>
    </motion.div>
  );
}
