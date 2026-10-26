"use client";

import { Sparkles } from "lucide-react";
import { motion } from "framer-motion";
import { RecentNotifications } from "./recent-notifications";
import { DashboardAnnouncements } from "./dashboard-announcements";
import { DashboardTimetable } from "./dashboard-timetable";
import { DashboardDDayBlock } from "./dashboard-dday-block";
import { DashboardCountdown } from "./dashboard-countdown";
import type { DashboardSnapshotResponse } from "@/shared/types/api";





export function Dashboard({ snapshot }: { snapshot: DashboardSnapshotResponse }) {
  const { user, notifications, primaryTimetable, upcomingSchedules, announcements } = snapshot;

  if (!user) {
    return null;
  }

  return (
    <main className="grow py-8 md:py-12 px-4 sm:px-6 lg:px-8 max-w-[1700px] mx-auto w-full">
      {                                                 }
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
          <DashboardCountdown />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {                                          }
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

        {                                            }
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
