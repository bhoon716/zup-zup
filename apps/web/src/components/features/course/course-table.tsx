"use client";

import { useEffect, useRef, useCallback, useState } from "react";
import {
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { useSubscribe, useSubscriptions, useUnsubscribe } from "@/hooks/useSubscriptions";
import type { Course } from "@/types/api";
import { cn } from "@/lib/utils";
import { Calendar, Heart, Bell, Crown } from "lucide-react";
import { useToggleWishlist, useWishlist } from "@/hooks/useWishlist";
import { useTimetables, useAddCourseToTimetable } from "@/hooks/useTimetable";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { CourseDetailDialog } from "./course-detail-dialog";
import { formatClassification } from "@/lib/utils/formatters";
import { useUser } from "@/hooks/useUser";
import { useAuthStore } from "@/store/useAuthStore";

interface CourseTableProps {
  courses: Course[];
  onLoadMore?: () => void;
  hasMore?: boolean;
  isFetchingNextPage?: boolean;
}

export function CourseTable({ courses, onLoadMore, hasMore, isFetchingNextPage }: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions();
  const { data: wishlist } = useWishlist();
  const { mutate: toggleWishlist } = useToggleWishlist();
  const { mutate: subscribe, isPending: isSubscribing } = useSubscribe();
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const { data: user } = useUser();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const { data: timetableList } = useTimetables();
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();

  const desktopTarget = useRef<HTMLTableRowElement>(null);
  const mobileTarget = useRef<HTMLDivElement>(null);

  const handleObserver = useCallback((entries: IntersectionObserverEntry[]) => {
    const isIntersecting = entries.some((entry) => entry.isIntersecting);
    if (isIntersecting && hasMore && !isFetchingNextPage && onLoadMore) {
      onLoadMore();
    }
  }, [hasMore, isFetchingNextPage, onLoadMore]);

  useEffect(() => {
    const observer = new IntersectionObserver(handleObserver, {
      root: null, // viewport or scroll container if ref passed
      rootMargin: "20px",
      threshold: 0.1,
    });
    
    if (desktopTarget.current) {
      observer.observe(desktopTarget.current);
    }
    if (mobileTarget.current) {
      observer.observe(mobileTarget.current);
    }
    
    return () => {
      observer.disconnect();
    };
  }, [handleObserver]);

  const isSubscribed = (courseKey: string) => {
    return subscriptions?.some((sub) => sub.courseKey === courseKey);
  };

  const isWished = (courseKey: string) => {
    return wishlist?.some((item) => item.courseKey === courseKey);
  };

  const handleSubscribe = (courseKey: string) => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }
    
    const subscription = subscriptions?.find((sub) => sub.courseKey === courseKey);
    if (subscription) {
      // Already subscribed, so unsubscribe
      unsubscribe(subscription.id);
    } else {
      // Not subscribed, so subscribe
      subscribe({ courseKey });
    }
  };

  const handleCourseClick = (course: Course) => {
    setSelectedCourse(course);
    setIsDialogOpen(true);
  };

  return (
    <>
      {/* Desktop Table View */}
      <div className="hidden md:block border rounded-xl overflow-hidden shadow-sm bg-card/30 backdrop-blur-md">
        <div className="relative overflow-auto max-h-[650px] scrollbar-thin scrollbar-thumb-muted-foreground/20 scrollbar-track-transparent">
          <table className="w-full caption-bottom text-sm">
            <TableHeader className="sticky top-0 z-20 bg-background/95 backdrop-blur-sm shadow-md border-b">
              <TableRow className="hover:bg-transparent border-b border-border/50">
                <TableHead className="w-[80px] text-[11px] font-black uppercase tracking-wider pl-4">강의명</TableHead>
                <TableHead className="w-[60px] text-[11px] font-black uppercase tracking-wider">교수명</TableHead>
                <TableHead className="w-[40px] text-center text-[11px] font-black uppercase tracking-wider">학점</TableHead>
                <TableHead className="w-[90px] text-[11px] font-black uppercase tracking-wider">시간</TableHead>
                <TableHead className="text-center w-[70px] text-[11px] font-black uppercase tracking-wider">인원/정원</TableHead>
                <TableHead className="text-center w-[40px] text-[11px] font-black uppercase tracking-wider">시간표</TableHead>
                <TableHead className="text-center w-[40px] text-[11px] font-black uppercase tracking-wider">찜</TableHead>
                <TableHead className="text-center w-[40px] text-[11px] font-black uppercase tracking-wider pr-4">구독</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {courses.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-20 text-muted-foreground italic font-medium">
                    검색 조건에 맞는 강의가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                <>
                  {courses.map((course, index) => {
                    const subscribed = isSubscribed(course.courseKey);
                    const isAvailable = (course.available ?? 0) > 0;
                    const capacityColor = isAvailable 
                        ? "text-emerald-500 font-bold" 
                        : "text-rose-500 font-bold";

                    return (
                      <TableRow 
                        key={`${course.courseKey}-${index}`}
                        className="group hover:bg-primary/5 transition-colors border-white/5 cursor-pointer"
                        onClick={() => handleCourseClick(course)}
                      >
                        <TableCell className="font-semibold max-w-[80px] sm:max-w-[120px] pl-4">
                          <div className="flex flex-col gap-0.5">
                            <span className="text-foreground hover:text-primary transition-colors truncate block text-[12px]">
                              {course.name}
                            </span>
                            <div className="flex gap-1 text-[9px] text-muted-foreground/50 font-medium truncate">
                                <span>{course.classNumber}분반</span>
                                <span>·</span>
                                <span>{course.department}</span>
                                <span>·</span>
                                <span>{formatClassification(course.classification)}</span>
                            </div>
                          </div>
                        </TableCell>
                        <TableCell className="text-[11px] font-medium text-foreground/80 truncate max-w-[60px]">
                            {course.professor || "-"}
                        </TableCell>
                        <TableCell className="text-center text-[11px] font-medium text-foreground/80">
                            {course.credits}
                        </TableCell>
                        <TableCell className="text-[10px] text-muted-foreground truncate max-w-[90px] font-medium">
                            {course.classTime || "-"}
                        </TableCell>
                        <TableCell className="text-center">
                            <span className={`text-[12px] ${capacityColor}`}>
                                {course.current || 0} / {course.capacity || 0}
                            </span>
                        </TableCell>
                        <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
                          {!user ? (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 hover:bg-transparent"
                              onClick={() => setLoginModalOpen(true)}
                            >
                              <Calendar className="w-4 h-4 text-muted-foreground/40 hover:text-indigo-500/70" />
                            </Button>
                          ) : (
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 hover:bg-transparent"
                                  disabled={isAdding}
                                >
                                  <Calendar
                                    className={cn(
                                      "w-4 h-4 transition-all",
                                      (Array.isArray(timetableList) && timetableList.some(t => t.primary))
                                        ? "text-indigo-500/70 hover:text-indigo-500"
                                        : "text-muted-foreground/40 hover:text-indigo-500/70"
                                    )}
                                  />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end" className="w-56">
                                <DropdownMenuLabel className="text-[10px] font-bold uppercase tracking-tighter text-muted-foreground">시간표 선택</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                {Array.isArray(timetableList) && [...timetableList]
                                  .sort((a, b) => (a.primary ? -1 : 1))
                                  .map((t) => (
                                    <DropdownMenuItem 
                                      key={t.id}
                                      className="flex items-center justify-between cursor-pointer"
                                      onClick={() => {
                                        addToTimetable({ timetableId: t.id, courseKey: course.courseKey });
                                      }}
                                    >
                                      <div className="flex items-center gap-2 text-xs font-medium max-w-[180px]">
                                        {t.primary && <Crown className="w-3 h-3 text-yellow-500 fill-yellow-500 flex-shrink-0" />}
                                        <span className="truncate">{t.name}</span>
                                      </div>
                                    </DropdownMenuItem>
                                  ))}
                                {Array.isArray(timetableList) && timetableList.length === 0 && (
                                  <div className="p-2 text-[10px] text-center text-muted-foreground">시간표가 없습니다.</div>
                                )}
                                {!timetableList && (
                                  <div className="p-2 text-[10px] text-center text-muted-foreground animate-pulse">로딩 중...</div>
                                )}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          )}
                        </TableCell>
                        <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 hover:bg-transparent"
                            onClick={() => {
                              if (!user) {
                                setLoginModalOpen(true);
                                return;
                              }
                              toggleWishlist(course.courseKey);
                            }}
                          >
                            <Heart
                              className={`w-4 h-4 transition-all ${
                                isWished(course.courseKey)
                                  ? "fill-rose-500 text-rose-500 scale-110"
                                  : "text-muted-foreground/40 hover:text-rose-500/70"
                              }`}
                            />
                          </Button>
                        </TableCell>
                        <TableCell className="text-center pr-4" onClick={(e) => e.stopPropagation()}>
                          <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleSubscribe(course.courseKey)}
                              disabled={isSubscribing || isUnsubscribing}
                              className="h-8 w-8 hover:bg-transparent"
                          >
                            <Bell
                               className={`w-4 h-4 transition-all ${
                                subscribed 
                                    ? "fill-primary text-primary scale-110" 
                                    : "text-muted-foreground/40 hover:text-primary/70"
                               }`}
                            />
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })}
                  
                  {/* Sentinel for Infinite Scroll (Desktop) */}
                  <TableRow ref={desktopTarget}>
                     <TableCell colSpan={8} className="h-4 p-0 border-0" />
                  </TableRow>
                </>
              )}
            </TableBody>
          </table>
        </div>
      </div>

      {/* Mobile Card View */}
      <div className="md:hidden space-y-4">
        {courses.length === 0 ? (
          <div className="text-center py-20 text-muted-foreground italic font-medium bg-card/30 rounded-2xl border">
            검색 조건에 맞는 강의가 없습니다.
          </div>
        ) : (
          <>
            {courses.map((course, index) => {
              const subscribed = isSubscribed(course.courseKey);
              const isAvailable = (course.available ?? 0) > 0;
              const capacityColor = isAvailable 
                  ? "text-emerald-500 font-bold" 
                  : "text-rose-500 font-bold";

              return (
                <div 
                  key={`${course.courseKey}-${index}-mobile`}
                  className="bg-white dark:bg-gray-900 border border-border/50 rounded-2xl p-4 shadow-sm active:scale-[0.98] transition-all"
                  onClick={() => handleCourseClick(course)}
                >
                  <div className="flex justify-between items-start mb-3">
                    <div className="space-y-1 pr-2">
                       <h3 className="font-bold text-base text-foreground leading-tight">{course.name}</h3>
                       <div className="flex flex-wrap gap-x-2 gap-y-1 text-[11px] text-muted-foreground font-medium">
                          <span>{course.professor || "미지정"}</span>
                          <span className="opacity-30">|</span>
                          <span>{course.department}</span>
                          <span className="opacity-30">|</span>
                          <span>{formatClassification(course.classification)}</span>
                       </div>
                    </div>
                    <div className="text-right flex-shrink-0">
                       <p className={`text-sm tracking-tighter ${capacityColor}`}>
                         {course.current} / {course.capacity}
                       </p>
                       <p className="text-[10px] text-muted-foreground mt-0.5">{course.credits}학점</p>
                    </div>
                  </div>

                  <div className="bg-accent/30 rounded-xl p-3 mb-4 flex items-center justify-between">
                     <div className="flex items-center gap-2 text-xs text-muted-foreground font-medium">
                        <Calendar className="w-3.5 h-3.5 opacity-60" />
                        <span className="truncate max-w-[180px]">{course.classTime || "시간 미배정"}</span>
                     </div>
                     <span className="text-[10px] font-bold text-muted-foreground/60 px-2 py-0.5 rounded-full border border-border/50 uppercase">{course.subjectCode}</span>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-border/40">
                    <div className="flex items-center gap-1">
                      {!user ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-10 px-3 rounded-xl gap-2 hover:bg-primary/5 active:bg-primary/10"
                          onClick={(e) => {
                            e.stopPropagation();
                            setLoginModalOpen(true);
                          }}
                        >
                          <Calendar className="w-4 h-4 text-muted-foreground" />
                          <span className="text-xs font-bold">시간표</span>
                        </Button>
                      ) : (
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild onClick={(e) => e.stopPropagation()}>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-10 px-3 rounded-xl gap-2 hover:bg-primary/5 active:bg-primary/10"
                              disabled={isAdding}
                            >
                              <Calendar
                                className={cn(
                                  "w-4 h-4",
                                  (Array.isArray(timetableList) && timetableList.some(t => t.primary))
                                    ? "text-indigo-500"
                                    : "text-muted-foreground"
                                )}
                              />
                              <span className="text-xs font-bold">시간표</span>
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="start" className="w-56">
                            <DropdownMenuLabel className="text-[10px] font-bold uppercase tracking-tighter text-muted-foreground">시간표 선택</DropdownMenuLabel>
                            <DropdownMenuSeparator />
                            {Array.isArray(timetableList) && [...timetableList]
                              .sort((a, b) => (a.primary ? -1 : 1))
                              .map((t) => (
                                <DropdownMenuItem 
                                  key={t.id}
                                  className="flex items-center justify-between cursor-pointer"
                                  onClick={() => {
                                    addToTimetable({ timetableId: t.id, courseKey: course.courseKey });
                                  }}
                                >
                                  <div className="flex items-center gap-2 text-xs font-medium max-w-[180px]">
                                    {t.primary && <Crown className="w-3 h-3 text-yellow-500 fill-yellow-500 flex-shrink-0" />}
                                    <span className="truncate">{t.name}</span>
                                  </div>
                                </DropdownMenuItem>
                              ))}
                          </DropdownMenuContent>
                        </DropdownMenu>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-10 w-10 rounded-xl hover:bg-rose-500/5 active:bg-rose-500/10"
                        onClick={(e) => {
                          e.stopPropagation();
                          if (!user) {
                             setLoginModalOpen(true);
                             return;
                          }
                          toggleWishlist(course.courseKey);
                        }}
                      >
                        <Heart
                          className={cn(
                             "w-4 h-4 transition-all",
                             isWished(course.courseKey) ? "fill-rose-500 text-rose-500" : "text-muted-foreground"
                          )}
                        />
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        className={cn(
                          "h-10 px-4 rounded-xl gap-2 font-bold transition-all border-none",
                          subscribed 
                            ? "bg-primary text-white hover:bg-primary/90" 
                            : "bg-primary/10 text-primary hover:bg-primary/20"
                        )}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleSubscribe(course.courseKey);
                        }}
                        disabled={isSubscribing || isUnsubscribing}
                      >
                        <Bell className={cn("w-4 h-4", subscribed && "fill-white")} />
                        <span className="text-xs">{subscribed ? "구독 중" : "알림 신청"}</span>
                      </Button>
                    </div>
                  </div>
                </div>
              );
            })}
            
            {/* Sentinel for Infinite Scroll (Mobile) */}
            <div ref={mobileTarget} className="h-4" />
          </>
        )}

        {isFetchingNextPage && (
          <div className="flex justify-center items-center py-6 gap-2 text-xs text-muted-foreground animate-pulse">
            <div className="h-2 w-2 bg-primary rounded-full animate-bounce delay-75"/>
            <div className="h-2 w-2 bg-primary rounded-full animate-bounce delay-100"/>
            <div className="h-2 w-2 bg-primary rounded-full animate-bounce delay-150"/>
            <span className="font-bold">강의 더 불러오기...</span>
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
