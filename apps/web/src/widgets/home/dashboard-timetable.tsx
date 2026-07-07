"use client";

import { ExternalLink, Calendar } from "lucide-react";
import { Button } from "@/shared/ui/button";
import Link from "next/link";
import type { TimetableDetailResponse } from "@/shared/types/api";
import { TimetableGrid } from "@/features/timetable/components/timetable-grid";

/**
 * 대시보드에서 사용자의 대표 시간표를 시간표 페이지와 동일한 디자인으로 보여줍니다.
 */
export function DashboardTimetable({ timetable }: { timetable: TimetableDetailResponse | null | undefined }) {
  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-4 sm:p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full flex flex-col min-h-[500px] sm:min-h-[600px]">
      <div className="flex justify-between items-center mb-5">
        <h3 className="text-lg sm:text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-3">
          <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl sm:rounded-2xl bg-blue-50 dark:bg-blue-950/30 flex items-center justify-center">
            <Calendar className="w-4 h-4 sm:w-5 sm:h-5 text-blue-600" />
          </div>
          대표 시간표
        </h3>
        <Link href="/timetable">
          <Button variant="ghost" size="sm" className="text-xs font-bold text-primary hover:text-primary-dark hover:bg-primary/5 transition-colors group">
            전체보기 <ExternalLink className="w-3.5 h-3.5 ml-1.5 group-hover:translate-x-0.5 transition-transform" />
          </Button>
        </Link>
      </div>

      <div className="flex-1 rounded-2xl sm:rounded-3xl border border-slate-100 overflow-hidden bg-white">
        {timetable ? (
          <div className="h-full overflow-auto custom-scrollbar bg-white">
            <TimetableGrid 
              timetable={timetable} 
              className="w-full shadow-none rounded-none" 
            />
          </div>
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-slate-400 gap-3">
            <Calendar className="w-12 h-12 opacity-20" />
            <p className="text-sm font-medium">등록된 시간표가 없습니다.</p>
            <Link href="/timetable">
              <Button size="sm" variant="outline" className="rounded-xl">시간표 작성하러 가기</Button>
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
