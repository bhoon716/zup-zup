"use client";

import { motion } from "framer-motion";
import { Timer, CalendarCheck, ClipboardList } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  HoverCard,
  HoverCardContent,
  HoverCardTrigger,
} from "@/components/ui/hover-card";
import Image from "next/image";
import { useState, useEffect } from "react";

const dDayEvents = [
  {
    title: "장바구니",
    date: "02. 18 (화)",
    dDay: "D-1",
    icon: ClipboardList,
    active: true,
    color: "text-blue-500",
    bgColor: "bg-blue-50 dark:bg-blue-950/30",
    borderColor: "border-blue-100 dark:border-blue-900"
  },
  {
    title: "1차 수강신청",
    date: "02. 24 (월)",
    dDay: "D-7",
    icon: CalendarCheck,
    active: false,
    color: "text-purple-500",
    bgColor: "bg-purple-50 dark:bg-purple-950/30",
    borderColor: "border-purple-100 dark:border-purple-900"
  },
  {
    title: "정정 기간",
    date: "03. 02 (월)",
    dDay: "D-13",
    icon: Timer,
    active: false,
    color: "text-amber-500",
    bgColor: "bg-amber-50 dark:bg-amber-950/30",
    borderColor: "border-amber-100 dark:border-amber-900"
  }
];

export function DashboardDDayBlock() {
  const [imageTimestamp, setImageTimestamp] = useState(Date.now());

  // 이미지 갱신을 위한 유틸리티 함수 (Event Bus 역할)
  useEffect(() => {
    const handleImageUpdate = () => setImageTimestamp(Date.now());
    window.addEventListener("schedule-image-updated", handleImageUpdate);
    return () => window.removeEventListener("schedule-image-updated", handleImageUpdate);
  }, []);

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full flex flex-col">
      <div className="flex justify-between items-center mb-5 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center">
            <Timer className="w-5 h-5 text-indigo-600" />
          </div>
          
          <HoverCard openDelay={100} closeDelay={100}>
            <HoverCardTrigger asChild>
              <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 cursor-help decoration-wavy decoration-indigo-300 underline-offset-4 hover:underline">
                주요 일정
              </h3>
            </HoverCardTrigger>
            <HoverCardContent className="w-auto p-0 border-none shadow-2xl bg-transparent" side="right" align="start">
              <div className="relative rounded-xl overflow-hidden border-4 border-white dark:border-gray-800 shadow-xl bg-white dark:bg-gray-900">
                <Image 
                  src={`/uploads/schedule.png?t=${imageTimestamp}`}
                  alt="수강신청 일정 상세" 
                  width={600} 
                  height={800}
                  className="w-auto h-auto max-w-[500px] max-h-[700px] object-contain"
                  onError={(e) => {
                    // 이미지가 없을 경우 숨기거나 기본 이미지 표시
                    (e.target as HTMLImageElement).style.display = 'none';
                  }}
                />
              </div>
            </HoverCardContent>
          </HoverCard>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {dDayEvents.map((event, idx) => (
          <motion.div
            key={event.title}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, delay: 0.1 * idx }}
            className={cn(
              "relative flex items-center gap-4 p-4 rounded-2xl border transition-all cursor-default group",
              event.active 
                ? "bg-white dark:bg-gray-800 border-primary/20 shadow-sm" 
                : "bg-gray-50/50 dark:bg-gray-900/50 border-transparent hover:bg-white dark:hover:bg-gray-800 hover:border-gray-100 dark:hover:border-gray-700"
            )}
          >
            {/* 활성 상태 표시 바 */}
            {event.active && (
              <div className="absolute left-0 top-3 bottom-3 w-1 bg-primary rounded-r-full" />
            )}

            <div className={cn(
              "w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 transition-transform group-hover:scale-110",
              event.bgColor
            )}>
              <event.icon className={cn("w-6 h-6", event.color)} />
            </div>

            <div className="flex-1 min-w-0 flex flex-col gap-0.5">
              <div className="flex items-center gap-2">
                <span className={cn(
                  "text-[10px] font-black uppercase tracking-wider px-1.5 py-0.5 rounded-md",
                  event.active ? "bg-primary/10 text-primary" : "bg-gray-100 text-gray-500"
                )}>
                  {event.active ? "Upcoming" : "Scheduled"}
                </span>
              </div>
              <h4 className="text-sm font-bold text-gray-900 dark:text-gray-100 truncate">
                {event.title}
              </h4>
              <p className="text-xs text-gray-500 font-medium">
                {event.date}
              </p>
            </div>

            <div className={cn(
              "px-3 py-1.5 rounded-xl text-sm font-black tracking-tight shrink-0 flex items-center justify-center min-w-14",
              event.active 
                ? "bg-primary text-white shadow-lg shadow-primary/20" 
                : "bg-white dark:bg-gray-800 text-gray-400 border border-gray-100 dark:border-gray-700"
            )}>
              {event.dDay}
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
