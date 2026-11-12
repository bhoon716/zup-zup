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
    <section className="flex min-w-0 flex-1 flex-col overflow-hidden rounded-xl border border-border bg-card">
      <div className="space-y-3 border-b border-border p-3 sm:px-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex min-w-0 items-center gap-2">
            <h1 className="shrink-0 text-2xl font-bold tracking-tight text-foreground sm:text-[28px]">내 시간표</h1>
            <Badge variant="outline" className="min-w-0 shrink gap-1.5 rounded-full px-2 py-1 text-xs font-medium text-muted-foreground">
              <CalendarDays className="size-3.5 shrink-0" />
              <span className="truncate">{timetableName}</span>
            </Badge>
          </div>

          <div className="flex shrink-0 flex-wrap items-center gap-2">
            <Button
              variant="outline"
              className="gap-1.5 has-[>svg]:px-2.5 sm:gap-2 sm:has-[>svg]:px-3"
              onClick={handleExportImage}
            >
              <Download className="h-4 w-4" />
              이미지로 저장
            </Button>
            <Button className="gap-1.5 has-[>svg]:px-2.5 sm:gap-2 sm:has-[>svg]:px-3" onClick={onOpenCustomDialog}>
              <Plus className="h-4 w-4" />
              수업 직접 추가
            </Button>
          </div>
        </div>

        {isListLoading ? (
          <div className="flex items-center justify-center py-2">
            <Loader2 className="h-6 w-6 animate-spin text-primary" />
          </div>
        ) : (
          <TimetableSelect
            timetables={timetables}
            currentId={activeTimetableId || 0}
            onSelect={setSelectedTimetableId}
            onCreate={onCreateTimetable}
            onDelete={onDeleteTimetable}
            onSetPrimary={onSetPrimaryTimetable}
            isLoading={isCreatingTimetable}
          />
        )}
      </div>

      <div className="min-h-0 flex-1 p-1 sm:p-3">
        {isDetailLoading ? (
          <div className="flex h-full items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : activeTimetableId && timetableDetail ? (
          <motion.div
            initial={{ opacity: 0, scale: 0.99 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
            className="overflow-auto rounded-lg border border-border bg-white"
          >
            <TimetableGrid timetable={timetableDetail} className="mx-auto w-full" />
          </motion.div>
        ) : null}

        {!isListLoading && timetables.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border px-3 py-8">
            <p className="text-sm text-muted-foreground">아직 생성된 시간표가 없습니다.</p>
            <Button onClick={() => onCreateTimetable("기본 시간표")}>
              첫 시간표 만들기
            </Button>
          </div>
        )}
      </div>
    </section>
  );
}
