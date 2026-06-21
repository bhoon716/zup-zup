"use client";

import { motion } from "framer-motion";
import { CalendarDays } from "lucide-react";
import { useActiveDday } from "@/features/admin/hooks/useDday";
import { useClientReady } from "@/shared/lib/use-client-ready";
import type { ScheduleResponse } from "@/shared/types/api";

export function DashboardCountdown({
  suppressFetch = false,
}: {
  upcomingSchedules?: ScheduleResponse[];
  suppressFetch?: boolean;
} = {}) {
  const isClientReady = useClientReady();

  const { data: activeDday } = useActiveDday(isClientReady && !suppressFetch);

  if (!activeDday) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="flex items-center gap-1.5 sm:gap-2 bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-100/50 dark:border-indigo-900/30 rounded-xl px-2.5 sm:px-3 h-8 sm:h-9 select-none shrink-0"
    >
      <CalendarDays className="w-3 h-3 sm:w-3.5 sm:h-3.5 text-indigo-600 dark:text-indigo-400 shrink-0" />
      <span className="text-[10px] sm:text-xs font-black text-indigo-600 dark:text-indigo-400 tracking-tight whitespace-nowrap">
        앞으로 {activeDday.title}까지
      </span>
      <span className="text-sm sm:text-base font-black text-gray-900 dark:text-gray-100 font-mono tracking-tight whitespace-nowrap">
        {activeDday.dDay}
      </span>
    </motion.div>
  );
}
