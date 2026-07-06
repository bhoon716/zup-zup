"use client";

import { AnimatePresence, motion } from "framer-motion";
import {
  CalendarDays,
  Clock3,
  Download,
  Heart,
  BookOpen,
  Loader2,
  Plus,
  Trash2,
} from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";
import { formatDayOfWeek } from "@/shared/lib/formatters";
import { TimetableGrid } from "@/features/timetable/components/timetable-grid";
import { TimetableSelect } from "@/features/timetable/components/timetable-select";
import { CreditStatsCard } from "@/features/timetable/components/credit-stats-card";
import { CustomScheduleDialog } from "@/features/timetable/components/custom-schedule-dialog";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import type { TimetablePageModel } from "./useTimetablePage";

export function TimetablePage({ model }: { model: TimetablePageModel }) {
  const {
    customDialogOpen,
    setCustomDialogOpen,
    selectedSidebarCourse,
    sidebarCourseDialogOpen,
    setSidebarCourseDialogOpen,
    ...sections
  } = model;

  return (
    <div className="min-h-[calc(100vh-4rem)] bg-slate-100">
      <motion.main
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="mx-auto flex w-full max-w-[1600px] flex-col gap-2 p-2 sm:gap-4 sm:p-4 lg:flex-row"
      >
        <TimetableMainSection model={sections} onOpenCustomDialog={() => setCustomDialogOpen(true)} />
        <TimetableSidebarSection model={sections} />
      </motion.main>

      <CustomScheduleDialog
        timetableId={model.activeTimetableId}
        open={customDialogOpen}
        onOpenChange={setCustomDialogOpen}
      />

      <CourseDetailDialog
        course={selectedSidebarCourse}
        open={sidebarCourseDialogOpen}
        onOpenChange={setSidebarCourseDialogOpen}
      />
    </div>
  );
}

function TimetableMainSection({
  model,
  onOpenCustomDialog,
}: {
  model: Pick<
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
  >;
  onOpenCustomDialog: () => void;
}) {
  const {
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
  } = model;

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

function TimetableSidebarSection({
  model,
}: {
  model: Pick<
    TimetablePageModel,
    | "sidebarTab"
    | "setSidebarTab"
    | "totalCredits"
    | "todayLabel"
    | "todaySchedules"
    | "timetableCourses"
    | "wishlist"
    | "isWishlistLoading"
    | "timetableDetail"
    | "activeTimetableId"
    | "handleSidebarCourseClick"
    | "handleAddCourse"
    | "handleRemoveCourse"
    | "isAddingCourse"
    | "isRemovingCourse"
    | "pendingAddCourseKey"
    | "pendingRemoveCourseKey"
  >;
}) {
  const {
    sidebarTab,
    setSidebarTab,
    totalCredits,
    todayLabel,
    todaySchedules,
    timetableCourses,
    wishlist,
    isWishlistLoading,
    timetableDetail,
    activeTimetableId,
    handleSidebarCourseClick,
    handleAddCourse,
    handleRemoveCourse,
    isAddingCourse,
    isRemovingCourse,
    pendingAddCourseKey,
    pendingRemoveCourseKey,
  } = model;

  return (
    <aside className="w-full lg:w-[340px]">
      <div className="flex h-full flex-col overflow-hidden rounded-[1.5rem] border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 p-5">
          <CreditStatsCard totalCredits={totalCredits} />
        </div>

        <div className="border-b border-slate-100 p-4">
          <div className="grid grid-cols-2 rounded-xl bg-slate-100 p-1">
            <button
              type="button"
              className={cn(
                "rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200 ease-out",
                sidebarTab === "schedule" ? "bg-primary text-white shadow-sm" : "text-slate-500 hover:text-slate-700"
              )}
              onClick={() => setSidebarTab("schedule")}
            >
              시간표
            </button>
            <button
              type="button"
              className={cn(
                "rounded-lg px-3 py-2 text-sm font-medium transition-all duration-200 ease-out",
                sidebarTab === "wishlist" ? "bg-primary text-white shadow-sm" : "text-slate-500 hover:text-slate-700"
              )}
              onClick={() => setSidebarTab("wishlist")}
            >
              찜 목록
            </button>
          </div>
        </div>

        <div className="min-h-0 flex-1 space-y-4 overflow-y-auto bg-slate-50/30 p-4">
          <AnimatePresence mode="wait">
            {sidebarTab === "schedule" ? (
              <motion.div
                key="schedule-tab"
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                transition={{ duration: 0.2 }}
                className="space-y-4"
              >
                <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                  <div className="mb-3 flex items-center justify-between">
                    <div className="flex items-center gap-1.5 text-sm font-bold text-slate-800">
                      <Clock3 className="h-4 w-4 text-primary" />
                      오늘 일정
                    </div>
                    <Badge variant="outline" className="rounded-full border-slate-200 bg-slate-50 text-xs text-slate-600">
                      {todayLabel}요일
                    </Badge>
                  </div>
                  {todaySchedules.length === 0 ? (
                    <p className="py-2 text-center text-xs text-slate-500">오늘 등록된 일정이 없습니다.</p>
                  ) : (
                    <div className="space-y-2">
                      {todaySchedules.map((item) => (
                        <div key={item.key} className="rounded-xl border border-slate-100 bg-slate-50/50 p-3">
                          <p className="text-sm font-bold text-slate-800">{item.title}</p>
                          <p className="mt-1 text-xs text-slate-500">{item.subtitle}</p>
                          <p className="mt-2 text-xs font-semibold text-primary">
                            {item.startTime} - {item.endTime}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div>
                  <div className="mb-2 flex items-center gap-1.5 px-1 text-xs font-bold text-slate-500">
                    <BookOpen className="h-3.5 w-3.5" />
                    시간표 강의 {timetableCourses.length}건
                  </div>
                  <div className="space-y-2">
                    {timetableCourses.length === 0 ? (
                      <div className="rounded-xl border border-dashed border-slate-200 bg-white p-4 text-center text-xs text-slate-500">
                        강의가 아직 없습니다.
                      </div>
                    ) : (
                      timetableDetail?.courses?.map((course) => (
                        <motion.div
                          layout
                          key={course.courseKey}
                          initial={{ opacity: 0, y: 5 }}
                          animate={{ opacity: 1, y: 0 }}
                          whileHover={{ y: -2 }}
                          className="group cursor-pointer rounded-xl border border-slate-200 bg-white p-3 transition-shadow hover:border-primary/50 hover:shadow-md"
                          onClick={() => handleSidebarCourseClick(course)}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex-1 overflow-hidden">
                              <p className="truncate text-sm font-bold text-slate-800 transition-colors group-hover:text-primary">
                                {course.name}
                              </p>
                              <p className="mt-1 text-xs text-slate-500">{course.professor}</p>
                            </div>
                            <div className="flex shrink-0 flex-col items-end gap-1.5">
                              <Badge variant="secondary" className="h-5 rounded-md px-1.5 text-[10px] font-semibold">
                                {course.credits}학점
                              </Badge>
                              <button
                                type="button"
                                disabled={isRemovingCourse && pendingRemoveCourseKey === course.courseKey}
                                className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  handleRemoveCourse(course.courseKey, course.name);
                                }}
                              >
                                {isRemovingCourse && pendingRemoveCourseKey === course.courseKey ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Trash2 className="h-4 w-4" />
                                )}
                              </button>
                            </div>
                          </div>
                          <p className="mt-2 text-[11px] text-slate-500">
                            {(course.schedules ?? [])
                              .map(
                                (schedule) =>
                                  `${formatDayOfWeek(String(schedule.dayOfWeek))} ${schedule.startTime}-${schedule.endTime}`
                              )
                              .join(", ") || course.classTime || "시간 정보 없음"}
                          </p>
                          <div className="mt-2 flex items-center gap-1.5 text-[11px] text-slate-500">
                            <span>{course.classroom || "강의실 미배정"}</span>
                            {course.classification ? <span>• {course.classification}</span> : null}
                          </div>
                        </motion.div>
                      ))
                    )}
                  </div>
                </div>
              </motion.div>
            ) : (
              <motion.div
                key="wishlist-tab"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.2 }}
              >
                <div className="mb-2 flex items-center gap-1.5 px-1 text-xs font-bold text-slate-500">
                  <Heart className="h-3.5 w-3.5" />
                  찜한 강의 {wishlist.length}건
                </div>
                {isWishlistLoading ? (
                  <div className="flex items-center justify-center py-6">
                    <Loader2 className="h-5 w-5 animate-spin text-slate-400" />
                  </div>
                ) : wishlist.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-slate-200 bg-white p-4 text-center text-xs text-slate-500">
                    찜한 강의가 없습니다.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {wishlist.map((course) => {
                      const isInTimetable = timetableDetail?.courses?.some((timetableCourse) => timetableCourse.courseKey === course.courseKey);
                      const isPending =
                        (isAddingCourse && pendingAddCourseKey === course.courseKey) ||
                        (isRemovingCourse && pendingRemoveCourseKey === course.courseKey);

                      return (
                        <motion.div
                          layout
                          key={course.id}
                          initial={{ opacity: 0, y: 5 }}
                          animate={{ opacity: 1, y: 0 }}
                          whileHover={{ y: -2 }}
                          className="group cursor-pointer rounded-xl border border-slate-200 bg-white p-3 transition-shadow hover:border-primary/50 hover:shadow-md"
                          onClick={() => handleSidebarCourseClick(course)}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex-1 overflow-hidden">
                              <p className="truncate text-sm font-bold text-slate-800 transition-colors group-hover:text-primary">
                                {course.courseName}
                              </p>
                              <p className="mt-1 text-xs text-slate-500">{course.professor}</p>
                            </div>
                            <div className="flex shrink-0 flex-col items-end gap-1.5">
                              <Badge variant="secondary" className="h-5 rounded-md px-1.5 text-[10px] font-semibold">
                                {course.credits}학점
                              </Badge>
                              {isInTimetable ? (
                                <button
                                  type="button"
                                  disabled={isPending}
                                  className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleRemoveCourse(course.courseKey, course.courseName);
                                  }}
                                >
                                  {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                                </button>
                              ) : (
                                <button
                                  type="button"
                                  disabled={isPending || !activeTimetableId}
                                  className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-primary/10 hover:text-primary"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleAddCourse(course.courseKey);
                                  }}
                                >
                                  {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Plus className="h-4 w-4" />}
                                </button>
                              )}
                            </div>
                          </div>
                          <p className="mt-1 text-[11px] text-slate-500">{course.classTime}</p>
                          <div className="mt-2 flex items-center gap-1.5 text-[11px] font-medium text-slate-500">
                            <span>{course.classification}</span>
                          </div>
                        </motion.div>
                      );
                    })}
                  </div>
                )}
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </aside>
  );
}
