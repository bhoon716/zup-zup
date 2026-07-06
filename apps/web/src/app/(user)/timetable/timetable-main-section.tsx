"use client";

import { motion } from "framer-motion";
import { CalendarDays, Download, Loader2, Plus } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { TimetableGrid } from "@/features/timetable/components/timetable-grid";
import { TimetableSelect } from "@/features/timetable/components/timetable-select";
import type { TimetablePageModel } from "./useTimetablePage";

type TimetableMainSectionProps = Pick<
  TimetablePageModel,
  | "timetables"
  | "isListLoading"
  | "activeTimetableId"
  | "timetableDetail"
  | "isDetailLoading"
  | "timetableName"
  | "setSelectedTimetableId"
  | "onCreateTimetable"
  | "onDeleteTimetable"
  | "onSetPrimaryTimetable"
  | "isCreatingTimetable"
  | "handleExportImage"
> & {
  onOpenCustomDialog: () => void;
};

export function TimetableMainSection({
  timetables,
  isListLoading,
  activeTimetableId,
  timetableDetail,
  isDetailLoading,
  timetableName,
  setSelectedTimetableId,
  onCreateTimetable,
  onDeleteTimetable,
  onSetPrimaryTimetable,
  isCreatingTimetable,
  handleExportImage,
  onOpenCustomDialog,
}: TimetableMainSectionProps) {
  return (
    <section className="flex min-w-0 flex-1 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm sm:rounded-[1.5rem]">
      <div className="border-b border-slate-100 p-3 sm:px-5 sm:py-4">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div className="space-y-1">
            <h1 className="text-xl font-bold tracking-tight text-slate-900 sm:text-2xl">내 시간표</h1>
            <p className="hidden text-sm text-slate-500 sm:block">강의 시간이 겹치면 대시보드처럼 자동으로 세로 분할되어 표시됩니다.</p>
          </div>

          <div className="flex items-center gap-2">
            <Badge variant="outline" className="rounded-full border-slate-200 px-3 py-1 text-xs font-semibold text-slate-600">
              <CalendarDays className="h-3.5 w-3.5" />
              {timetableName}
            </Badge>
            <Button
              variant="outline"
              size="sm"
              className="h-9 rounded-xl border-slate-200 bg-white text-slate-700 transition-all active:scale-95 hover:bg-slate-50"
              onClick={handleExportImage}
            >
              <Download className="h-4 w-4" />
              내보내기
            </Button>
            <Button
              variant="default"
              size="sm"
              className="h-9 rounded-xl bg-primary text-white shadow-sm transition-all active:scale-95 hover:bg-primary-hover"
              onClick={onOpenCustomDialog}
            >
              <Plus className="h-4 w-4" />
              수업 직접 추가
            </Button>
          </div>
        </div>

        {isListLoading ? (
          <div className="mt-4 flex items-center justify-center py-5">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        ) : (
          <div className="mt-4">
            <TimetableSelect
              timetables={timetables}
              currentId={activeTimetableId || 0}
              onSelect={setSelectedTimetableId}
              onCreate={onCreateTimetable}
              onDelete={onDeleteTimetable}
              onSetPrimary={onSetPrimaryTimetable}
              isLoading={isCreatingTimetable}
            />
          </div>
        )}
      </div>

      <div className="min-h-0 flex-1 bg-slate-50/80 p-1 sm:p-4">
        {isDetailLoading ? (
          <div className="flex h-full items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-slate-400" />
          </div>
        ) : activeTimetableId && timetableDetail ? (
          <motion.div
            initial={{ opacity: 0, scale: 0.99 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
            className="h-full overflow-auto rounded-xl border border-slate-200 bg-white p-1 sm:rounded-2xl sm:p-3"
          >
            <TimetableGrid timetable={timetableDetail} className="mx-auto w-full" />
          </motion.div>
        ) : null}

        {!isListLoading && timetables.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-white">
            <p className="mb-4 text-sm text-slate-500">아직 생성된 시간표가 없습니다.</p>
            <Button onClick={() => onCreateTimetable("기본 시간표")} className="rounded-xl transition-all active:scale-95">
              첫 시간표 만들기
            </Button>
          </div>
        )}
      </div>
    </section>
  );
}
