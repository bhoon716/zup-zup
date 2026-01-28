"use client";

import {
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { useSubscribe, useSubscriptions } from "@/hooks/useSubscriptions";
import type { Course } from "@/types/api";
import { Heart, Bell, Calendar } from "lucide-react";
import { useToggleWishlist, useWishlist } from "@/hooks/useWishlist";
import { usePrimaryTimetable, useAddCourseToTimetable, useRemoveCourseFromTimetable } from "@/hooks/useTimetable";
import { CourseDetailDialog } from "./course-detail-dialog";
import { toast } from "sonner";
import { formatClassification } from "@/lib/utils/formatters";
import { useState } from "react";
import type { TimetableEntryResponse } from "@/types/api";

interface CourseTableProps {
  courses: Course[];
}

export function CourseTable({ courses }: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions();
  const { data: wishlist } = useWishlist();
  const { mutate: toggleWishlist } = useToggleWishlist();
  const { mutate: subscribe, isPending } = useSubscribe();
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const { data: primaryTimetable } = usePrimaryTimetable();
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();
  const { mutate: removeFromTimetable, isPending: isRemoving } = useRemoveCourseFromTimetable();

  const isSubscribed = (courseKey: string) => {
    return subscriptions?.some((sub) => sub.courseKey === courseKey);
  };

  const isWished = (courseKey: string) => {
    return wishlist?.some((item) => item.courseKey === courseKey);
  };

  const isInTimetable = (courseKey: string) => {
    return primaryTimetable?.entries.some((entry: TimetableEntryResponse) => entry.courseKey === courseKey);
  };

  const handleSubscribe = (courseKey: string) => {
    subscribe({ courseKey });
  };

  const handleTimetableAction = (courseKey: string) => {
    if (!primaryTimetable) {
      toast.error('대표 시간표가 없습니다. 내 시간표 메뉴에서 시간표를 먼저 생성해주세요.');
      return;
    }

    if (isInTimetable(courseKey)) {
      removeFromTimetable({ timetableId: primaryTimetable.id, courseKey });
    } else {
      addToTimetable({ timetableId: primaryTimetable.id, courseKey });
    }
  };

  const handleCourseClick = (course: Course) => {
    setSelectedCourse(course);
    setIsDialogOpen(true);
  };

  return (
    <>
    <div className="border rounded-xl overflow-hidden shadow-sm bg-card/30 backdrop-blur-md">
      <div className="relative overflow-auto max-h-[650px] scrollbar-thin scrollbar-thumb-muted-foreground/20 scrollbar-track-transparent">
        <table className="w-full caption-bottom text-sm">
          <TableHeader className="sticky top-0 z-20 bg-background/95 backdrop-blur-sm shadow-md border-b">
            <TableRow className="hover:bg-transparent border-b border-border/50">
              <TableHead className="w-[80px] text-[11px] font-black uppercase tracking-wider pl-4">강좌명</TableHead>
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
                <TableCell colSpan={7} className="text-center py-20 text-muted-foreground italic font-medium">
                  검색 조건에 맞는 강좌가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              courses.map((course) => {
                const subscribed = isSubscribed(course.courseKey);
                const isAvailable = (course.available ?? 0) > 0;
                // capacity display color logic
                const capacityColor = isAvailable 
                    ? "text-emerald-500 font-bold" 
                    : "text-rose-500 font-bold";

                return (
                  <TableRow 
                    key={course.courseKey} 
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
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 hover:bg-transparent"
                        onClick={() => handleTimetableAction(course.courseKey)}
                        disabled={isAdding || isRemoving}
                      >
                        <Calendar
                          className={`w-4 h-4 transition-all ${
                            isInTimetable(course.courseKey)
                              ? "fill-indigo-500 text-indigo-500 scale-110"
                              : "text-muted-foreground/40 hover:text-indigo-500/70"
                          }`}
                        />
                      </Button>
                    </TableCell>
                    <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8 hover:bg-transparent"
                        onClick={() => toggleWishlist(course.courseKey)}
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
                          disabled={isPending}
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
              })
            )}
            </TableBody>
        </table>
      </div>
    </div>
    
    <CourseDetailDialog 
        course={selectedCourse} 
        open={isDialogOpen} 
        onOpenChange={setIsDialogOpen} 
    />
    </>
  );
}
