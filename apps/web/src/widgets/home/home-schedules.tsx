"use client";

import { Calendar, Loader2 } from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { useClientReady } from "@/shared/lib/use-client-ready";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";
import type { PublicPageData } from "@/shared/lib/public-metadata";
import type { ScheduleResponse } from "@/shared/types/api";

export function HomeSchedules({ initialData }: { initialData?: PublicPageData<ScheduleResponse[]> | null }) {
  const isClientReady = useClientReady();
  const { data: upcomingSchedules, isPending, isError } = useUpcomingSchedules(isClientReady, initialData);
  const isScheduleLoading = !upcomingSchedules && (!isClientReady || isPending);

  return (
    <section className="pb-32">
      <div className="max-w-[1200px] mx-auto px-6">
        <div className="mb-8">
          <h2 className="text-2xl md:text-3xl font-bold text-[#161118] flex items-center gap-3">
            <Calendar className="text-indigo-500 w-8 h-8" />
            학사 및 수강신청 주요 일정
          </h2>
          <p className="mt-2 text-[#161118]/60 text-sm md:text-base font-medium">놓치지 말아야 할 전북대 수강신청 일정을 확인하세요.</p>
        </div>

        {isScheduleLoading ? (
          <div role="status" className="flex h-32 items-center justify-center">
            <Loader2 aria-hidden="true" className="h-6 w-6 text-slate-300 motion-safe:animate-spin" />
            <span className="sr-only">주요 일정을 불러오는 중입니다.</span>
          </div>
        ) : isError && !upcomingSchedules ? (
          <p role="status" className="text-sm text-slate-500">주요 일정을 불러오지 못했습니다.</p>
        ) : upcomingSchedules?.length === 0 ? (
          <div className="flex xl:col-span-12 h-32 items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50">
            <p className="text-sm font-medium text-slate-500">현재 예정된 주요 일정이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {upcomingSchedules?.map((schedule) => (
              <div
                key={schedule.id}
                className="bg-white p-6 rounded-2xl shadow-sm border border-slate-100 flex flex-col justify-between hover:shadow-md transition-shadow hover:border-indigo-100"
              >
                <div>
                  <div className="flex flex-col gap-2 mb-4">
                    <div className="flex items-center justify-between">
                      <span className={cn(
                        "px-3 py-1 rounded-full text-xs font-black tracking-widest uppercase",
                        schedule.dDay === "D-Day" ? "bg-red-50 text-red-600" : "bg-indigo-50 text-indigo-600"
                      )}>
                        {schedule.dDay}
                      </span>
                    </div>
                  </div>
                  <h3 className="text-[1.1rem] font-bold text-[#161118] leading-snug break-keep mb-1">
                    {schedule.scheduleType}
                  </h3>
                </div>
                <div className="mt-6 pt-4 border-t border-slate-100 flex flex-col gap-1 text-[13px] font-semibold text-slate-500">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">일자</span>
                    <span>
                      {schedule.startDate.substring(5).replace("-", ".")} ~ {schedule.endDate.substring(5).replace("-", ".")}
                    </span>
                  </div>
                  {(schedule.startTime || schedule.endTime) && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">시간</span>
                      <span>
                        {schedule.startTime ? schedule.startTime : "--:--"} ~ {schedule.endTime ? schedule.endTime : "--:--"}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
