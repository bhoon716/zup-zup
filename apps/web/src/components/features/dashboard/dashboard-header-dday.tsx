"use client";

import { motion } from "framer-motion";
import { Timer, CalendarCheck, ClipboardList, Sparkles } from "lucide-react";
import { cn } from "@/lib/utils";

const dDayEvents = [
  {
    title: "장바구니",
    date: "02. 18",
    dDay: "D-1",
    icon: ClipboardList,
    active: true
  },
  {
    title: "1차 수강신청",
    date: "02. 24",
    dDay: "D-7",
    icon: CalendarCheck,
    active: false
  },
  {
    title: "정정 기간",
    date: "03. 02",
    dDay: "D-13",
    icon: Timer,
    active: false
  }
];

export function DashboardHeaderDDay() {
  return (
    <div className="flex flex-wrap md:flex-nowrap gap-3 items-center">
      {dDayEvents.map((event, idx) => (
        <motion.div
          key={event.title}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.3 + idx * 0.1 }}
          className={cn(
            "flex items-center gap-3 px-4 py-2.5 rounded-2xl border transition-all hover:translate-y-[-2px]",
            event.active 
              ? "bg-primary border-primary shadow-lg shadow-primary/20 text-white" 
              : "bg-white dark:bg-gray-900 border-gray-100 dark:border-gray-800 text-gray-600 dark:text-gray-400"
          )}
        >
          <div className={cn(
            "w-8 h-8 rounded-xl flex items-center justify-center shrink-0",
            event.active ? "bg-white/20" : "bg-gray-50 dark:bg-gray-800"
          )}>
            <event.icon className={cn("w-4 h-4", event.active ? "text-white" : "text-primary")} />
          </div>
          
          <div className="flex flex-col min-w-0 pr-1">
            <div className="flex items-center gap-1.5">
              <span className={cn("text-[9px] font-black uppercase tracking-tight", event.active ? "text-white/80" : "text-primary/70")}>
                {event.active ? "Next" : "Wait"}
              </span>
              {event.active && <Sparkles className="w-2.5 h-2.5 text-amber-300 fill-amber-300" />}
            </div>
            <h4 className={cn("text-xs font-bold leading-none truncate", event.active ? "text-white" : "text-gray-900 dark:text-gray-200")}>
              {event.title}
            </h4>
          </div>

          <div className={cn(
            "px-2.5 py-1 rounded-lg text-sm font-black tracking-tighter shrink-0 ml-1",
            event.active ? "bg-white text-primary" : "bg-primary/5 text-primary border border-primary/10"
          )}>
            {event.dDay}
          </div>
        </motion.div>
      ))}
    </div>
  );
}
