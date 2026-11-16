"use client";

import { Sparkles } from "lucide-react";
import { DashboardAnnouncements } from "./dashboard-announcements";
import { DashboardTimetable } from "./dashboard-timetable";
import { DashboardDDayBlock } from "./dashboard-dday-block";
import { DashboardCountdown } from "./dashboard-countdown";
import type { DashboardSnapshotResponse } from "@/shared/types/api";

export function Dashboard({ snapshot }: { snapshot: DashboardSnapshotResponse }) {
  const { user, primaryTimetable, upcomingSchedules, announcements } = snapshot;

  if (!user) {
    return null;
  }

  return (
    <main className="mx-auto w-full max-w-[1700px] grow px-4 py-5 sm:px-6 md:py-7 lg:px-8">
      <div className="mb-5 flex w-full flex-wrap items-end justify-between gap-3">
        <div className="min-w-0">
          <p className="mb-1.5 flex items-center gap-1.5 text-xs font-semibold text-primary">
            <Sparkles aria-hidden="true" className="size-3.5" />
            다시 만나서 반가워요
          </p>
          <h1 className="text-4xl font-black leading-tight text-foreground md:text-5xl">
            안녕하세요, <span className="text-primary">{user.name}님</span>
          </h1>
        </div>

        <div className="shrink-0 empty:hidden">
          <DashboardCountdown />
        </div>
      </div>

      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-12">
        <div className="min-w-0 lg:col-span-7">
          <DashboardTimetable timetable={primaryTimetable} />
        </div>

        <div className="flex min-w-0 flex-col gap-4 lg:col-span-5">
          <DashboardDDayBlock upcomingSchedules={upcomingSchedules} />

          <DashboardAnnouncements announcements={announcements} />
        </div>
      </div>
    </main>
  );
}
