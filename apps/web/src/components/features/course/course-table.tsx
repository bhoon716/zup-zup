"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";
import { formatClassification } from "@/lib/utils/formatters";
import { useAuthStore } from "@/store/useAuthStore";
import { useUser } from "@/hooks/useUser";
import { useAddCourseToTimetable, useTimetables } from "@/hooks/useTimetable";
import { useSubscribe, useSubscriptions, useUnsubscribe } from "@/hooks/useSubscriptions";
import { useToggleWishlist, useWishlist } from "@/hooks/useWishlist";
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
import type { Course } from "@/types/api";

interface CourseTableProps {
  courses: Course[];
  onLoadMore?: () => void;
  hasMore?: boolean;
  isFetchingNextPage?: boolean;
}

function getSeatStatus(available: number) {
  if (available <= 0) {
    return {
      label: "마감됨",
      badgeClass: "bg-gray-100 text-gray-500",
      barClass: "bg-gray-400",
    };
  }

  if (available <= 5) {
    return {
      label: "마감 임박",
      badgeClass: "bg-red-50 text-red-600",
      barClass: "bg-red-500",
    };
  }

  return {
    label: "여석 있음",
    badgeClass: "bg-emerald-50 text-emerald-600",
    barClass: "bg-emerald-500",
  };
}

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
              className="group overflow-hidden rounded-xl border border-border bg-white transition-all duration-300 hover:border-primary/40 hover:shadow-md"
              onClick={() => handleCourseClick(course)}
            >
              <div className="flex items-stretch">
                <div className={cn("w-1.5 shrink-0", getClassificationStripe(course.classification))} />

                <div className="flex min-w-0 flex-1 flex-col gap-3 p-4 md:flex-row md:items-center md:gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="mb-1.5 flex flex-wrap items-center gap-1.5">
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

                    <h3 className="mb-1.5 truncate text-sm font-bold text-foreground transition-colors group-hover:text-primary md:text-base">
                      {course.name}
                    </h3>

                    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[11px] text-gray-600">
                      <span className="flex items-center gap-1">
                        <UserRound className="h-3.5 w-3.5 text-gray-400" />
                        <span className="font-medium text-gray-700">{course.professor || "미지정"}</span>
                      </span>

                      <span className="flex items-center gap-1">
                        <Clock3 className="h-3.5 w-3.5 text-gray-400" />
                        <span>{course.classTime || "시간 미배정"}</span>
                      </span>

                      <span className="flex items-center gap-1">
                        <MapPin className="h-3.5 w-3.5 text-gray-400" />
                        <span>{course.classroom || "강의실 미정"}</span>
                      </span>
                    </div>
                  </div>

                  <div className="flex min-w-[170px] flex-col gap-2 border-t border-border pt-2 md:border-l md:border-t-0 md:pl-4 md:pt-0">
                    <div className="space-y-1">
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
                      className="flex items-center gap-1.5"
                      onClick={(e) => e.stopPropagation()}
                    >
                      {!user ? (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          className="h-8 w-8 rounded-lg border-gray-200 text-gray-500 hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                          onClick={() => setLoginModalOpen(true)}
                          title="시간표에 추가"
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
                              className="h-8 w-8 rounded-lg border-gray-200 text-gray-500 hover:border-primary/30 hover:bg-primary/5 hover:text-primary"
                              disabled={isAdding}
                              title="시간표에 추가"
                            >
                              <CalendarPlus className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>

                          <DropdownMenuContent align="start" className="w-56">
                            <DropdownMenuLabel className="text-[11px] font-bold text-muted-foreground">
                              추가할 시간표 선택
                            </DropdownMenuLabel>
                            <DropdownMenuSeparator />

                            {Array.isArray(timetableList) && timetableList.length > 0 ? (
                              [...timetableList]
                                .sort((a, b) => Number(b.primary) - Number(a.primary))
                                .map((timetable) => (
                                  <DropdownMenuItem
                                    key={timetable.id}
                                    className="cursor-pointer"
                                    onClick={() => {
                                      addToTimetable({
                                        timetableId: timetable.id,
                                        courseKey: course.courseKey,
                                      });
                                    }}
                                  >
                                    <span className="flex max-w-[190px] items-center gap-2 text-xs font-medium">
                                      {timetable.primary && (
                                        <Crown className="h-3 w-3 shrink-0 fill-yellow-500 text-yellow-500" />
                                      )}
                                      <span className="truncate">{timetable.name}</span>
                                    </span>
                                  </DropdownMenuItem>
                                ))
                            ) : (
                              <div className="px-2 py-3 text-center text-xs text-muted-foreground">
                                등록된 시간표가 없습니다.
                              </div>
                            )}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}

                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        className="h-8 w-8 rounded-lg border-gray-200 text-gray-500 hover:border-rose-200 hover:bg-rose-50 hover:text-rose-500"
                        title="관심강좌 담기"
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
                          "h-8 w-8 rounded-lg",
                          subscribed
                            ? "bg-primary text-white hover:bg-primary/90"
                            : "border-gray-200 text-gray-500 hover:border-primary/30 hover:bg-primary/5 hover:text-primary",
                        )}
                        title={subscribed ? "알림 설정 끄기" : "알림 설정 켜기"}
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
