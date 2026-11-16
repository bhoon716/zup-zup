"use client";

import { useRef, type MouseEvent } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { BookOpen, Heart, Loader2, Trash2, Plus } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { cn } from "@/shared/lib/utils";
import { formatClassTime, formatDayOfWeek } from "@/shared/lib/formatters";
import { CreditStatsCard } from "@/features/timetable/components/credit-stats-card";
import type { TimetablePageModel } from "./useTimetablePage";

type TimetableSidebarSectionProps = Pick<
  TimetablePageModel,
  | "sidebarTab"
  | "setSidebarTab"
  | "totalCredits"
  | "timetableCourses"
  | "wishlist"
  | "isWishlistLoading"
  | "timetableDetail"
  | "activeTimetableId"
  | "handleSidebarCourseClick"
  | "handleAddCourse"
  | "handleRemoveCourse"
  | "isRemovingCourse"
  | "isAddingCourse"
  | "pendingAddCourseKey"
>;

export function TimetableSidebarSection({
  sidebarTab,
  setSidebarTab,
  totalCredits,
  timetableCourses,
  wishlist,
  isWishlistLoading,
  timetableDetail,
  activeTimetableId,
  handleSidebarCourseClick,
  handleAddCourse,
  handleRemoveCourse,
  isRemovingCourse,
  isAddingCourse,
  pendingAddCourseKey,
}: TimetableSidebarSectionProps) {
  const activeTabRef = useRef<HTMLButtonElement>(null);

  const handleRemoveCourseClick = (
    event: MouseEvent<HTMLButtonElement>,
    { courseKey, courseName }: { courseKey: string; courseName: string },
  ) => {
    event.stopPropagation();
    if (!window.confirm(`${courseName} 강의를 시간표에서 삭제하시겠습니까?`)) return;

    activeTabRef.current?.focus({ preventScroll: true });
    handleRemoveCourse(courseKey);
  };

  return (
    <aside className="w-full min-w-0 lg:w-80 lg:shrink-0">
      <div className="flex flex-col overflow-hidden rounded-xl border border-border bg-card">
        <div className="border-b border-border p-3">
          <CreditStatsCard totalCredits={totalCredits} />
        </div>

        <div className="border-b border-border p-3">
          <div className="grid grid-cols-2 rounded-lg bg-muted p-1">
            <button
              type="button"
              ref={sidebarTab === "schedule" ? activeTabRef : undefined}
              aria-pressed={sidebarTab === "schedule"}
              className={cn(
                "h-9 rounded-md px-3 text-sm font-medium transition-colors",
                sidebarTab === "schedule" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              )}
              onClick={() => setSidebarTab("schedule")}
            >
              시간표
            </button>
            <button
              type="button"
              ref={sidebarTab === "wishlist" ? activeTabRef : undefined}
              aria-pressed={sidebarTab === "wishlist"}
              className={cn(
                "h-9 rounded-md px-3 text-sm font-medium transition-colors",
                sidebarTab === "wishlist" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:text-foreground"
              )}
              onClick={() => setSidebarTab("wishlist")}
            >
              찜 목록
            </button>
          </div>
        </div>

        <div className="min-h-0 flex-1 p-3">
          <AnimatePresence initial={false} mode="wait">
            {sidebarTab === "schedule" ? (
              <motion.div
                key="schedule-tab"
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                transition={{ duration: 0.2 }}
              >
                <section aria-labelledby="timetable-courses-heading">
                  <div className="mb-2 flex items-center justify-between gap-2">
                    <h2 id="timetable-courses-heading" className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
                      <BookOpen className="size-4 text-primary" />
                      담은 강의
                    </h2>
                    <span className="text-xs tabular-nums text-muted-foreground">{timetableCourses.length}개</span>
                  </div>
                  <div className="space-y-2">
                    {timetableCourses.length === 0 ? (
                      <div className="rounded-lg border border-dashed border-border p-3 text-center text-sm text-muted-foreground">
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
                          className="group cursor-pointer rounded-lg border border-border p-2.5 transition-colors hover:border-primary/50"
                          onClick={() => handleSidebarCourseClick(course)}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex-1 overflow-hidden">
                              <p className="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary">
                                {course.name}
                              </p>
                              <p className="mt-0.5 text-xs text-muted-foreground">{course.professor}</p>
                            </div>
                            <div className="flex shrink-0 items-center gap-1.5">
                              <Badge variant="secondary" className="rounded-md bg-muted px-1.5 text-xs font-medium text-muted-foreground">
                                {course.credits}학점
                              </Badge>
                              <button
                                type="button"
                                aria-label={`${course.name} 시간표에서 삭제`}
                                disabled={isRemovingCourse}
                                className="grid size-8 place-items-center rounded-md text-muted-foreground transition-colors enabled:hover:bg-destructive/10 enabled:hover:text-destructive disabled:opacity-50"
                                onClick={(event) => handleRemoveCourseClick(event, { courseKey: course.courseKey, courseName: course.name })}
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                          <p className="mt-1 text-xs text-muted-foreground">
                            {(course.schedules ?? [])
                              .map(
                                (schedule) =>
                                  `${formatDayOfWeek(String(schedule.dayOfWeek))} ${schedule.startTime}-${schedule.endTime}`
                              )
                              .join(", ") || formatClassTime(course.classTime) || "시간 정보 없음"}
                          </p>
                          <div className="mt-0.5 flex flex-wrap items-center gap-x-1.5 text-xs text-muted-foreground">
                            <span>{course.classroom || "강의실 미배정"}</span>
                            {course.classification ? <span>• {course.classification}</span> : null}
                          </div>
                        </motion.div>
                      ))
                    )}
                  </div>
                </section>
              </motion.div>
            ) : (
              <motion.div
                key="wishlist-tab"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                transition={{ duration: 0.2 }}
              >
                <div className="mb-2 flex items-center justify-between gap-2">
                  <h2 className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
                    <Heart className="size-4 text-primary" />
                    찜한 강의
                  </h2>
                  <span className="text-xs tabular-nums text-muted-foreground">{wishlist.length}개</span>
                </div>
                {isWishlistLoading ? (
                  <div className="flex items-center justify-center py-6">
                    <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                  </div>
                ) : wishlist.length === 0 ? (
                  <div className="rounded-lg border border-dashed border-border p-3 text-center text-sm text-muted-foreground">
                    찜한 강의가 없습니다.
                  </div>
                ) : (
                  <div className="space-y-2">
                    {wishlist.map((course) => {
                      const isInTimetable = timetableDetail?.courses?.some((timetableCourse) => timetableCourse.courseKey === course.courseKey);
                      const isPending = isAddingCourse && pendingAddCourseKey === course.courseKey;

                      return (
                        <motion.div
                          layout
                          key={course.id}
                          initial={{ opacity: 0, y: 5 }}
                          animate={{ opacity: 1, y: 0 }}
                          whileHover={{ y: -2 }}
                          className="group cursor-pointer rounded-lg border border-border p-2.5 transition-colors hover:border-primary/50"
                          onClick={() => handleSidebarCourseClick(course)}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="flex-1 overflow-hidden">
                              <p className="truncate text-sm font-semibold text-foreground transition-colors group-hover:text-primary">
                                {course.courseName}
                              </p>
                              <p className="mt-0.5 text-xs text-muted-foreground">{course.professor}</p>
                            </div>
                            <div className="flex shrink-0 items-center gap-1.5">
                              <Badge variant="secondary" className="rounded-md bg-muted px-1.5 text-xs font-medium text-muted-foreground">
                                {course.credits}학점
                              </Badge>
                              {isInTimetable ? (
                                <button
                                  type="button"
                                  aria-label={`${course.courseName} 시간표에서 삭제`}
                                  disabled={isPending || isRemovingCourse}
                                  className="grid size-8 place-items-center rounded-md text-muted-foreground transition-colors enabled:hover:bg-destructive/10 enabled:hover:text-destructive disabled:opacity-50"
                                  onClick={(event) => handleRemoveCourseClick(event, course)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </button>
                              ) : (
                                <button
                                  type="button"
                                  aria-label={`${course.courseName} 시간표에 추가`}
                                  disabled={isPending || !activeTimetableId}
                                  className="grid size-8 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-primary/10 hover:text-primary"
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
                          <p className="mt-1 text-xs text-muted-foreground">
                            {formatClassTime(course.classTime) || "시간 정보 없음"}
                          </p>
                          <div className="mt-0.5 text-xs text-muted-foreground">
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
