"use client";

import { motion } from "framer-motion";
import { Timer, ClipboardList, Loader2 } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";

export function DashboardDDayBlock() {
  const { data: upcomingSchedules, isLoading } = useUpcomingSchedules();

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full flex flex-col">
      <div className="flex justify-between items-center mb-5 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center">
            <Timer className="w-5 h-5 text-indigo-600" />
          </div>
          
          <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100">
            주요 일정
          </h3>
        </div>
      </div>

      <div className="flex flex-col gap-3 flex-1 overflow-y-auto min-h-0 pr-1">
        {isLoading ? (
          <div className="flex items-center justify-center h-full min-h-[150px]">
            <Loader2 className="w-6 h-6 animate-spin text-gray-300" />
          </div>
        ) : upcomingSchedules?.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full min-h-[150px] text-center gap-2 text-gray-400">
            <ClipboardList className="w-8 h-8 opacity-20" />
            <p className="text-sm font-medium">현재 등록된 주요 일정이 없습니다.</p>
          </div>
        ) : (
          upcomingSchedules?.map((event, idx) => {
            const isDDay = event.dDay === "D-Day";
            return (
              <motion.div
                key={event.id}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ duration: 0.4, delay: 0.1 * idx }}
                className={cn(
                  "relative flex items-center gap-4 p-4 rounded-2xl border transition-all cursor-default group",
                  isDDay 
                    ? "bg-white dark:bg-gray-800 border-primary/20 shadow-sm" 
                    : "bg-gray-50/50 dark:bg-gray-900/50 border-transparent hover:bg-white dark:hover:bg-gray-800 hover:border-gray-100 dark:hover:border-gray-700"
                )}
              >
                {/* 활성 상태 표시 바 */}
                {isDDay && (
                  <div className="absolute left-0 top-3 bottom-3 w-1 bg-primary rounded-r-full" />
                )}

                <div className={cn(
                  "w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 transition-transform group-hover:scale-110",
                  isDDay ? "bg-red-50 text-red-600" : "bg-indigo-50 text-indigo-600"
                )}>
                  <ClipboardList className="w-6 h-6" />
                </div>

                <div className="flex-1 min-w-0 flex flex-col gap-0.5">
                  <div className="flex items-center gap-2">
                    <span className={cn(
                      "text-[10px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-md",
                      isDDay ? "bg-primary/10 text-primary" : "bg-gray-100 text-gray-500"
                    )}>
                      {isDDay ? "Today" : "Scheduled"}
                    </span>
                  </div>
                  <h4 className="text-sm font-bold text-gray-900 dark:text-gray-100 break-keep">
                    {event.scheduleType}
                  </h4>
                  <p className="text-[11px] text-gray-500 font-medium">
                    {event.startDate} {event.startTime ? event.startTime : ""} 
                    {" "}~{" "} 
                    {event.endDate} {event.endTime ? event.endTime : ""}
                  </p>
                </div>

                <div className={cn(
                  "px-3 py-1.5 rounded-xl text-sm font-black tracking-tight shrink-0 flex items-center justify-center min-w-14",
                  isDDay 
                    ? "bg-primary text-white shadow-lg shadow-primary/20" 
                    : "bg-white dark:bg-gray-800 text-gray-400 border border-gray-100 dark:border-gray-700"
                )}>
                  {event.dDay}
                </div>
              </motion.div>
            );
          })
        )}
      </div>
      
      <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-800 shrink-0">
        <p className="text-[11px] text-gray-400 font-medium text-center">
          * 실제 일정과 차이가 있을 수 있습니다. <br className="sm:hidden" />자세한 내용은 전북대학교 오아시스 및 수강신청 시스템을 우선적으로 참고해 주세요.
        </p>
      </div>
    </div>
  );
}
