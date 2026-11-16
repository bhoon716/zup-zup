"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { cn } from "@/shared/lib/utils";
import { normalizeCourse } from "@/shared/lib/course";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/shared/ui/tooltip";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useUser } from "@/features/user/hooks/useUser";
import { useAddCourseToTimetable, useTimetables } from "@/features/timetable/hooks/useTimetable";
import { useSubscribe, useSubscriptions, useUnsubscribe } from "@/features/subscription/hooks/useSubscriptions";
import { useToggleWishlist, useWishlist } from "@/features/wishlist/hooks/useWishlist";
import {
  Bell,
  CalendarPlus,
  Crown,
  Heart,
} from "lucide-react";
import { CourseResultCard, CourseSeatAlertButton, courseResultActionBorder, courseResultActionSize } from "./course-result-card";
import { CourseDetailDialog } from "./course-detail-dialog";
import { useCourseListWindow } from "../hooks/useCourseListWindow";
import { COURSE_CARD_GAP_PX } from "../constants/course-list-layout";
import type { Course, Subscription, TimetableResponse, User, WishlistResponse } from "@/shared/types/api";

interface CourseTableProps {
  courses: Course[];
  unmaskPublicCourseInfo?: boolean;
  onLoadMore?: () => void;
  hasMore?: boolean;
  isFetchingNextPage?: boolean;
  initialUser?: User | null;
  initialSubscriptions?: Subscription[];
  initialWishlist?: WishlistResponse[];
  initialTimetables?: TimetableResponse[];
  skipPersonalFetch?: boolean;
}

export function CourseTable({
  courses,
  unmaskPublicCourseInfo = false,
  onLoadMore,
  hasMore,
  isFetchingNextPage,
  initialUser,
  initialSubscriptions,
  initialWishlist,
  initialTimetables,
  skipPersonalFetch,
}: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions(!initialSubscriptions && !skipPersonalFetch, initialUser);
  const { data: wishlist } = useWishlist(!initialWishlist && !skipPersonalFetch, initialUser);
  const { mutate: toggleWishlist } = useToggleWishlist();
  const { mutate: subscribe, isPending: isSubscribing } = useSubscribe();
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const { data: user } = useUser({ enabled: initialUser === undefined && !skipPersonalFetch });

  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const [timetableMenuOpen, setTimetableMenuOpen] = useState(false);
  const { data: timetableList, isLoading: isTimetableLoading } = useTimetables(
    timetableMenuOpen && !initialTimetables && !skipPersonalFetch, initialUser,
  );
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();

  const resolvedUser = initialUser !== undefined ? initialUser : user;
  const resolvedSubscriptions = initialSubscriptions ?? subscriptions;
  const resolvedWishlist = initialWishlist ?? wishlist;
  const resolvedTimetables = initialTimetables ?? timetableList;

  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const detailTrigger = useRef<HTMLButtonElement>(null);
  const { listRef, virtualizer, enabled: isVirtual, scrollMargin, getItemKey } = useCourseListWindow(courses, activeIndex);

  const loadMoreTarget = useRef<HTMLDivElement>(null);

  const subscriptionMap = useMemo(
    () => new Map(resolvedSubscriptions?.map((sub) => [sub.courseKey, sub]) ?? []),
    [resolvedSubscriptions],
  );

  const wishlistSet = useMemo(
    () => new Set(resolvedWishlist?.map((item) => item.courseKey) ?? []),
    [resolvedWishlist],
  );

  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const isIntersecting = entries.some((entry) => entry.isIntersecting);

      if (isIntersecting && hasMore && !isFetchingNextPage && onLoadMore) {
        onLoadMore();
      }
    },
    [hasMore, isFetchingNextPage, onLoadMore],
  );

  const hasCourses = courses.length > 0;
  useEffect(() => {
    if (!hasCourses) return;
    if (!loadMoreTarget.current) {
      return;
    }

    const observer = new IntersectionObserver(handleObserver, {
      root: null,
      rootMargin: "180px",
      threshold: 0.1,
    });

    observer.observe(loadMoreTarget.current);

    return () => {
      observer.disconnect();
    };
  }, [handleObserver, hasCourses]);

  const handleSubscribe = (courseKey: string) => {
    if (!resolvedUser) {
      setLoginModalOpen(true);
      return;
    }

    const subscription = subscriptionMap.get(courseKey);

    if (subscription) {
      unsubscribe(subscription.id);
      return;
    }

    subscribe({ courseKey });
  };

  const openCourseDetail = (course: Course, index: number, trigger: HTMLButtonElement) => {
    detailTrigger.current = trigger;
    setActiveIndex(index);
    setSelectedCourse(course);
    setIsDialogOpen(true);
  };

  if (courses.length === 0) {
    return (
      <div className="rounded-2xl border border-border bg-white px-6 py-20 text-center shadow-sm">
        <p className="text-sm font-medium text-muted-foreground">
          검색 조건에 맞는 강의가 없습니다.
        </p>
      </div>
    );
  }

  const renderCourse = (rawCourse: Course, index: number) => {
    const course = normalizeCourse(rawCourse);
    const subscription = subscriptionMap.get(course.courseKey);
    const subscribed = !!subscription;
    const wished = wishlistSet.has(course.courseKey);

    return (
      <CourseResultCard
        course={course}
        unmaskPublicCourseInfo={unmaskPublicCourseInfo}
        onFocus={() => setActiveIndex(index)}
        onOpen={(event) => openCourseDetail(course, index, event.currentTarget)}
        actions={<>
          {!resolvedUser ? (
            <Button
              type="button"
              variant="outline"
              size="icon"
              className={cn(courseResultActionSize, courseResultActionBorder)}
              onClick={() => setLoginModalOpen(true)}
              aria-label={`${course.name}를 시간표에 추가하려면 로그인`}
              title="로그인 후 시간표에 추가"
            >
              <CalendarPlus className="h-4 w-4" />
            </Button>
          ) : (
            <DropdownMenu onOpenChange={(open) => {
              if (open) setActiveIndex(index);
              setTimetableMenuOpen(open);
            }}>
              <DropdownMenuTrigger asChild>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  className={cn(courseResultActionSize, courseResultActionBorder)}
                  disabled={isAdding}
                  aria-label={`${course.name}를 시간표에 추가`}
                  title="시간표에 추가"
                >
                  <CalendarPlus className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>

              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel className="text-[10px] font-bold text-muted-foreground">
                  추가할 시간표 선택
                </DropdownMenuLabel>
                <DropdownMenuSeparator />

                {isTimetableLoading ? (
                  <div role="status" className="px-2 py-3 text-center text-xs text-muted-foreground">
                    시간표를 불러오는 중입니다.
                  </div>
                ) : Array.isArray(resolvedTimetables) && resolvedTimetables.length > 0 ? (
                  [...resolvedTimetables]
                    .sort((a, b) => Number(b.primary) - Number(a.primary))
                    .map((timetable) => (
                      <DropdownMenuItem
                        key={timetable.id}
                        className="cursor-pointer py-1.5"
                        onClick={() => {
                          addToTimetable({
                            timetableId: timetable.id,
                            courseKey: course.courseKey,
                          });
                        }}
                      >
                        <span className="flex max-w-[160px] items-center gap-2 text-[11px] font-medium">
                          {timetable.primary && (
                            <Crown className="h-2.5 w-2.5 shrink-0 fill-yellow-500 text-yellow-500" />
                          )}
                          <span className="truncate">{timetable.name}</span>
                        </span>
                      </DropdownMenuItem>
                    ))
                ) : (
                  <div className="px-2 py-3 text-center text-xs text-muted-foreground">
                    시간표가 없습니다.
                  </div>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}

          <Button
            type="button"
            variant="outline"
            size="icon"
            className={cn(courseResultActionSize, courseResultActionBorder)}
            onClick={() => {
              if (!resolvedUser) {
                setLoginModalOpen(true);
                return;
              }
              toggleWishlist(course.courseKey);
            }}
            aria-label={wished ? `${course.name} 관심 강의 해제` : `${course.name} 관심 강의 추가`}
            title={wished ? "관심 강의 해제" : "관심 강의 추가"}
          >
            <Heart
              className={cn(
                "h-4 w-4 transition-all",
                wished && "fill-rose-500 text-rose-500",
              )}
            />
          </Button>

          {!course.isSubscribable ? (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="cursor-not-allowed">
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      className="h-7 w-7 rounded-lg border-gray-100 text-gray-400 pointer-events-none md:h-8 md:w-8"
                      disabled
                      aria-label="현재 추적 중인 학기가 아닙니다"
                      title="현재 추적 중인 학기가 아닙니다"
                    >
                      <Bell className="h-4 w-4" />
                    </Button>
                  </div>
                </TooltipTrigger>
                <TooltipContent side="top">
                  <p>현재 추적 중인 학기가 아닙니다.</p>
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
          ) : (
            <CourseSeatAlertButton
              subscribed={subscribed}
              onClick={() => handleSubscribe(course.courseKey)}
              disabled={isSubscribing || isUnsubscribing}
              aria-label={subscribed ? `${course.name} 빈자리 알림 해제` : `${course.name} 빈자리 알림 받기`}
              title={subscribed ? "빈자리 알림 해제" : "빈자리 알림 받기"}
            />
          )}
        </>}
      />
    );
  };

  return (
    <>
      <div className="space-y-2.5">
        <div
          ref={listRef}
          className={isVirtual ? "relative [overflow-anchor:none]" : "grid grid-cols-1"}
          style={isVirtual ? { height: virtualizer.getTotalSize() } : { rowGap: COURSE_CARD_GAP_PX }}
        >
          {isVirtual
            ? virtualizer.getVirtualItems().map((row) => (
              <div
                key={row.key}
                ref={virtualizer.measureElement}
                data-index={row.index}
                className="absolute inset-x-0 top-0"
                style={{ transform: `translateY(${row.start - scrollMargin}px)` }}
              >
                {renderCourse(courses[row.index], row.index)}
              </div>
            ))
            : courses.map((course, index) => (
              <div key={getItemKey(index)}>{renderCourse(course, index)}</div>
            ))}
        </div>
        <div ref={loadMoreTarget} className="h-4" />

        {isFetchingNextPage && (
          <div className="flex items-center justify-center gap-2 py-4 text-xs font-medium text-muted-foreground">
            <span className="h-2 w-2 animate-bounce rounded-full bg-primary [animation-delay:0ms]" />
            <span className="h-2 w-2 animate-bounce rounded-full bg-primary [animation-delay:120ms]" />
            <span className="h-2 w-2 animate-bounce rounded-full bg-primary [animation-delay:240ms]" />
            강의 더 불러오는 중...
          </div>
        )}
      </div>

      <CourseDetailDialog
        course={selectedCourse}
        unmaskPublicCourseInfo={unmaskPublicCourseInfo}
        returnFocusRef={detailTrigger}
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </>
  );
}
