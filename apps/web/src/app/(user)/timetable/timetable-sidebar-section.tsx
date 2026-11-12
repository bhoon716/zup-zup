"use client";

import { AnimatePresence, motion } from "framer-motion";
import { BookOpen, Clock3, Heart, Loader2, Trash2, Plus } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { cn } from "@/shared/lib/utils";
import { formatDayOfWeek } from "@/shared/lib/formatters";
import { CreditStatsCard } from "@/features/timetable/components/credit-stats-card";
import type { TimetablePageModel } from "./useTimetablePage";

type TimetableSidebarSectionProps = Pick<
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

export function TimetableSidebarSection({
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
}: TimetableSidebarSectionProps) {
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
          <AnimatePresence mode="wait">
            {sidebarTab === "schedule" ? (
              <motion.div
                key="schedule-tab"
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                transition={{ duration: 0.2 }}
                className="space-y-3"
              >
                <div className="rounded-lg border border-border p-3">
                  <div className="mb-2 flex items-center justify-between gap-2">
                    <h2 className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
                      <Clock3 className="size-4 text-primary" />
                      오늘 일정
                    </h2>
                    <Badge variant="outline" className="rounded-full bg-muted text-xs text-muted-foreground">
                      {todayLabel}요일
                    </Badge>
                  </div>
                  {todaySchedules.length === 0 ? (
                    <p className="py-1 text-sm text-muted-foreground">오늘 등록된 일정이 없습니다.</p>
                  ) : (
                    <div className="space-y-2">
                      {todaySchedules.map((item) => (
                        <div key={item.key} className="rounded-md bg-muted px-2.5 py-2">
                          <p className="text-sm font-semibold text-foreground">{item.title}</p>
                          <p className="mt-0.5 text-xs text-muted-foreground">{item.subtitle}</p>
                          <p className="mt-1 text-xs font-medium tabular-nums text-primary">
                            {item.startTime} - {item.endTime}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

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
                                disabled={isRemovingCourse && pendingRemoveCourseKey === course.courseKey}
                                className="grid size-8 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
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
                          <p className="mt-1 text-xs text-muted-foreground">
                            {(course.schedules ?? [])
                              .map(
                                (schedule) =>
                                  `${formatDayOfWeek(String(schedule.dayOfWeek))} ${schedule.startTime}-${schedule.endTime}`
                              )
                              .join(", ") || course.classTime || "시간 정보 없음"}
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
                                  disabled={isPending}
                                  className="grid size-8 place-items-center rounded-md text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
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
                          <p className="mt-1 text-xs text-muted-foreground">{course.classTime}</p>
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
