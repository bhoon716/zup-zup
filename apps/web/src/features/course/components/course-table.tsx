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
  Clock3,
  Crown,
  Heart,
  MapPin,
  Star,
  UserRound,
} from "lucide-react";
import { CourseDetailDialog } from "./course-detail-dialog";
import type { Course, Subscription, TimetableResponse, User, WishlistResponse } from "@/shared/types/api";

interface CourseTableProps {
  courses: Course[];
  onLoadMore?: () => void;
  hasMore?: boolean;
  isFetchingNextPage?: boolean;
  initialUser?: User | null;
  initialSubscriptions?: Subscription[];
  initialWishlist?: WishlistResponse[];
  initialTimetables?: TimetableResponse[];
  skipPersonalFetch?: boolean;
}

/**
 * 여석 상태에 따른 라벨 및 스타일 정보 반환
 */
function getSeatStatus(available: number) {
  if (available <= 0) {
    return {
      label: "마감됨",
      badgeClass: "bg-red-100 text-red-800",
      barClass: "bg-red-500",
    };
  }

  if (available <= 5) {
    return {
      label: "마감 임박",
      badgeClass: "bg-amber-100 text-amber-800",
      barClass: "bg-amber-500",
    };
  }

  return {
    label: "여석 있음",
    badgeClass: "bg-emerald-100 text-emerald-800",
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

  const { data: timetableList } = useTimetables(!initialTimetables && !skipPersonalFetch, initialUser);
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();

  const resolvedUser = initialUser !== undefined ? initialUser : user;
  const resolvedSubscriptions = initialSubscriptions ?? subscriptions;
  const resolvedWishlist = initialWishlist ?? wishlist;
  const resolvedTimetables = initialTimetables ?? timetableList;

  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const loadMoreTarget = useRef<HTMLDivElement>(null);

  const subscriptionMap = useMemo(
    () => new Map(resolvedSubscriptions?.map((sub) => [sub.courseKey, sub]) ?? []),
    [resolvedSubscriptions],
  );

  const wishlistSet = useMemo(
    () => new Set(resolvedWishlist?.map((item) => item.courseKey) ?? []),
    [resolvedWishlist],
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
   * 여석 알림 구독 상태를 토글 (구독 신청 또는 취소)
   */
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

  /**
   * 강의 카드를 클릭했을 때 해당 강의의 상세 정보를 다이얼로그로 표시
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
        {courses.map((rawCourse, index) => {
          const course = normalizeCourse(rawCourse);
          const subscription = subscriptionMap.get(course.courseKey);
          const subscribed = !!subscription;
          const wished = wishlistSet.has(course.courseKey);

          const { capacity, current, available } = course;
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
                          <span className="text-[10px] font-semibold tracking-wide text-gray-600">
                            {course.stdtrNo || "학수번호 미제공"}
                          </span>
                        </div>

                        <h2 className="mb-1 text-sm font-bold text-foreground md:mb-1.5 md:text-base">
                          <button
                            type="button"
                            className="block max-w-full truncate rounded-sm text-left transition-colors group-hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
                            onClick={(event) => {
                              event.stopPropagation();
                              handleCourseClick(course);
                            }}
                          >
                            {course.name}
                          </button>
                        </h2>
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
                          {course.classroom || "강의실 미배정"}
                        </span>
                      </span>
                    </div>
                  </div>

                  <div className="mt-3 flex items-center justify-between border-t border-border pt-2.5 md:mt-0 md:flex md:flex-col md:justify-center md:min-w-[190px] md:border-l md:border-t-0 md:pl-6 md:pt-0">
                    <div className="hidden w-full md:block">
                      <div className="mb-2.5 flex items-center justify-between gap-2">
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

                      <div className="h-2 w-full overflow-hidden rounded-full bg-gray-100">
                        <div
                          className={cn("h-full rounded-full transition-all", seatStatus.barClass)}
                          style={{ width: `${seatRatio}%` }}
                        />
                      </div>
                    </div>

                    <div
                      className="mt-0 flex w-full items-center justify-between gap-1 md:mt-4 md:w-full md:gap-1.5"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center gap-1">
                        <Star className="h-3.5 w-3.5 fill-yellow-400 text-yellow-400" />
                        <span className="text-xs font-bold text-gray-700">{course.averageRating ? course.averageRating.toFixed(1) : "0.0"}</span>
                        <span className="text-[10px] text-gray-600">({course.reviewCount || 0})</span>
                      </div>
                      
                      <div className="flex items-center gap-1 md:gap-1.5">
                      {!resolvedUser ? (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          className="h-7 w-7 rounded-lg border-gray-200 text-gray-500 md:h-8 md:w-8"
                          onClick={() => setLoginModalOpen(true)}
                          aria-label={`${course.name}를 시간표에 추가하려면 로그인`}
                          title="로그인 후 시간표에 추가"
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

                            {Array.isArray(resolvedTimetables) && resolvedTimetables.length > 0 ? (
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
                        className="h-7 w-7 rounded-lg border-gray-200 text-gray-500 md:h-8 md:w-8"
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
                          aria-label={subscribed ? `${course.name} 여석 알림 해제` : `${course.name} 여석 알림 받기`}
                          title={subscribed ? "여석 알림 해제" : "여석 알림 받기"}
                        >
                          <Bell className={cn("h-4 w-4", subscribed && "fill-white")} />
                        </Button>
                      )}
                      </div>
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
