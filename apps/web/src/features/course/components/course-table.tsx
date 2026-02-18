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
import { formatClassification } from "@/shared/lib/formatters";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useUser } from "@/features/user/hooks/useUser";
import { useAddCourseToTimetable, useTimetables } from "@/features/timetable/hooks/useTimetable";
import { useSubscribe, useSubscriptions, useUnsubscribe } from "@/features/subscription/hooks/useSubscriptions";
import { useToggleWishlist, useWishlist } from "@/features/wishlist/hooks/useWishlist";
import {
  Bell,
  CalendarPlus,
  Clock3,
  Crown,
  Heart,
  MapPin,
  UserRound,
} from "lucide-react";
import { CourseDetailDialog } from "./course-detail-dialog";
import type { Course } from "@/shared/types/api";

interface CourseTableProps {
  courses: Course[];
  onLoadMore?: () => void;
  hasMore?: boolean;
  isFetchingNextPage?: boolean;
}

/**
 * 여석 상태에 따른 라벨 및 스타일 정보 반환
 */
function getSeatStatus(available: number) {
  if (available <= 0) {
    return {
      label: "마감됨",
      badgeClass: "bg-red-50 text-red-600",
      barClass: "bg-red-500",
    };
  }

  if (available <= 5) {
    return {
      label: "마감 임박",
      badgeClass: "bg-amber-50 text-amber-600",
      barClass: "bg-amber-500",
    };
  }

  return {
    label: "여석 있음",
    badgeClass: "bg-emerald-50 text-emerald-600",
    barClass: "bg-emerald-500",
  };
}

/**
 * 이수 구분에 따른 왼쪽 포인트 색상 반환
 */
function getClassificationStripe(classification?: string) {
  const normalized = classification || "";

  if (normalized.includes("전공필수")) {
    return "bg-blue-500";
  }

  if (normalized.includes("전공")) {
    return "bg-purple-500";
  }

  if (normalized.includes("교양")) {
    return "bg-emerald-500";
  }

  if (normalized.includes("교직")) {
    return "bg-amber-500";
  }

  return "bg-gray-400";
}

/**
 * 검색된 강의 목록을 표시하는 테이블 컴포넌트 (무한 스크롤 지원)
 */
export function CourseTable({
  courses,
  onLoadMore,
  hasMore,
  isFetchingNextPage,
}: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions();
  const { data: wishlist } = useWishlist();
  const { mutate: toggleWishlist } = useToggleWishlist();
  const { mutate: subscribe, isPending: isSubscribing } = useSubscribe();
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const { data: user } = useUser();

  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const { data: timetableList } = useTimetables();
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();

  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const loadMoreTarget = useRef<HTMLDivElement>(null);

  const subscriptionMap = useMemo(
    () => new Map(subscriptions?.map((sub) => [sub.courseKey, sub]) ?? []),
    [subscriptions],
  );

  const wishlistSet = useMemo(
    () => new Set(wishlist?.map((item) => item.courseKey) ?? []),
    [wishlist],
  );

  /**
   * 무한 스크롤을 위한 Intersection Observer 콜백
   */
  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const isIntersecting = entries.some((entry) => entry.isIntersecting);

      if (isIntersecting && hasMore && !isFetchingNextPage && onLoadMore) {
        onLoadMore();
      }
    },
    [hasMore, isFetchingNextPage, onLoadMore],
  );

  useEffect(() => {
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
  }, [handleObserver]);

  /**
   * 여석 알림 구독/취소 핸들러
   */
  const handleSubscribe = (courseKey: string) => {
    if (!user) {
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

  /**
   * 강의 클릭 시 상세 다이얼로그 오픈
   */
  const handleCourseClick = (course: Course) => {
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

  return (
    <>
      <div className="space-y-2.5">
        {courses.map((course, index) => {
          const subscription = subscriptionMap.get(course.courseKey);
          const subscribed = !!subscription;
          const wished = wishlistSet.has(course.courseKey);

          const capacity = course.capacity ?? 0;
          const current = course.current ?? 0;
          const available = course.available ?? Math.max(capacity - current, 0);
          const seatRatio = capacity > 0 ? Math.min((current / capacity) * 100, 100) : 0;
          const seatStatus = getSeatStatus(available);

          return (
            <article
              key={`${course.courseKey}-${index}`}
              className="group relative overflow-hidden rounded-xl border border-border bg-white transition-all duration-300 hover:border-primary/40 hover:shadow-md"
              onClick={() => handleCourseClick(course)}
            >
              <div className="flex items-stretch">
                <div
                  className={cn(
                    "w-1 shrink-0 md:w-1.5",
                    getClassificationStripe(course.classification),
                  )}
                />

                <div className="flex min-w-0 flex-1 flex-col p-3 md:flex-row md:items-center md:gap-4 md:p-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-4">
                      <div className="min-w-0 flex-1">
                        <div className="mb-1 flex flex-wrap items-center gap-1 md:mb-1.5 md:gap-1.5">
                          <span className="rounded border border-primary/20 bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
                            {formatClassification(course.classification)}
                          </span>
                          <span className="rounded border border-gray-200 bg-gray-100 px-1.5 py-0.5 text-[10px] font-bold text-gray-600">
                            {course.credits || 0}학점
                          </span>
                          {course.subjectCode && (
                            <span className="text-[10px] font-semibold tracking-wide text-gray-400">
                              {course.subjectCode}
                            </span>
                          )}
                        </div>

                        <h3 className="mb-1 truncate text-sm font-bold text-foreground transition-colors group-hover:text-primary md:mb-1.5 md:text-base">
                          {course.name}
                        </h3>
                      </div>

                      {/* 모바일용 상단 우측 여석 상태 */}
                      <div className="flex items-center gap-2 pt-1 md:hidden">
                        <span
                          className={cn(
                            "rounded-sm px-1.5 py-0.5 text-[9px] font-bold shrink-0",
                            seatStatus.badgeClass,
                          )}
                        >
                          {seatStatus.label}
                        </span>
                        <div className="flex flex-col items-end gap-0.5">
                          <span className="text-[10px] font-bold text-gray-900">
                            {current} / {capacity}
                          </span>
                          <div className="h-1 w-12 overflow-hidden rounded-full bg-gray-100">
                            <div
                              className={cn("h-full transition-all", seatStatus.barClass)}
                              style={{ width: `${seatRatio}%` }}
                            />
                          </div>
                        </div>
                      </div>
                    </div>

                    <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-gray-600 md:text-[11px]">
                      <span className="flex items-center gap-1">
                        <UserRound className="h-3 w-3 text-gray-400 md:h-3.5 md:w-3.5" />
                        <span className="font-medium text-gray-700">
                          {course.professor || "미지정"}
                        </span>
                      </span>

                      <span className="flex items-center gap-1">
                        <Clock3 className="h-3 w-3 text-gray-400 md:h-3.5 md:w-3.5" />
                        <span className="max-w-[120px] truncate md:max-w-none">
                          {course.classTime || "시간 미배정"}
                        </span>
                      </span>

                      <span className="flex items-center gap-1">
                        <MapPin className="h-3 w-3 text-gray-400 md:h-3.5 md:w-3.5" />
                        <span className="max-w-[100px] truncate md:max-w-none">
                          {course.classroom || "강의실 미정"}
                        </span>
                      </span>
                    </div>
                  </div>

                  <div className="mt-3 flex items-center justify-between border-t border-border pt-2.5 md:mt-0 md:block md:min-w-[170px] md:border-l md:border-t-0 md:pl-4 md:pt-0">
                    <div className="hidden space-y-1 md:block">
                      <div className="flex items-center justify-between gap-2">
                        <span
                          className={cn(
                            "rounded px-1.5 py-0.5 text-[10px] font-bold",
                            seatStatus.badgeClass,
                          )}
                        >
                          {seatStatus.label}
                        </span>
                        <span className="text-xs font-bold text-gray-900">
                          {current} / {capacity}
                        </span>
                      </div>

                      <div className="h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
                        <div
                          className={cn("h-full rounded-full transition-all", seatStatus.barClass)}
                          style={{ width: `${seatRatio}%` }}
                        />
                      </div>
                    </div>



                    <div
                      className="flex items-center gap-1 md:gap-1.5"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {!user ? (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          className="h-7 w-7 rounded-lg border-gray-200 text-gray-500 md:h-8 md:w-8"
                          onClick={() => setLoginModalOpen(true)}
                        >
                          <CalendarPlus className="h-4 w-4" />
                        </Button>
                      ) : (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button
                              type="button"
                              variant="outline"
                              size="icon"
                              className="h-7 w-7 rounded-lg border-gray-200 text-gray-500 md:h-8 md:w-8"
                              disabled={isAdding}
                            >
                              <CalendarPlus className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>

                          <DropdownMenuContent align="end" className="w-48">
                            <DropdownMenuLabel className="text-[10px] font-bold text-muted-foreground">
                              추가할 시간표 선택
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />

                            {Array.isArray(timetableList) && timetableList.length > 0 ? (
                              [...timetableList]
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
                        className="h-7 w-7 rounded-lg border-gray-200 text-gray-500 md:h-8 md:w-8"
                        onClick={() => {
                          if (!user) {
                            setLoginModalOpen(true);
                            return;
                          }
                          toggleWishlist(course.courseKey);
                        }}
                      >
                        <Heart
                          className={cn(
                            "h-4 w-4 transition-all",
                            wished && "fill-rose-500 text-rose-500",
                          )}
                        />
                      </Button>

                      <Button
                        type="button"
                        variant={subscribed ? "default" : "outline"}
                        size="icon"
                        className={cn(
                          "h-7 w-7 rounded-lg md:h-8 md:w-8",
                          subscribed
                            ? "bg-primary text-white hover:bg-primary/90"
                            : "border-gray-200 text-gray-500",
                        )}
                        onClick={() => handleSubscribe(course.courseKey)}
                        disabled={isSubscribing || isUnsubscribing}
                      >
                        <Bell className={cn("h-4 w-4", subscribed && "fill-white")} />
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </article>
          );
        })}

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
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
      />
    </>
  );
}
