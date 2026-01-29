"use client";

import React, { useState } from "react";
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
import { cn } from "@/lib/utils";
import { Check, Calendar, Heart, Bell } from "lucide-react";
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

  const { data: timetableList } = useTimetables();
  const { mutate: addToTimetable, isPending: isAdding } = useAddCourseToTimetable();

  const isSubscribed = (courseKey: string) => {
    return subscriptions?.some((sub) => sub.courseKey === courseKey);
  };

  const isWished = (courseKey: string) => {
    return wishlist?.some((item) => item.courseKey === courseKey);
  };

  const handleSubscribe = (courseKey: string) => {
    subscribe({ courseKey });
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
                                (Array.isArray(timetableList) && timetableList.some(t => t.isPrimary))
                                  ? "text-indigo-500/70 hover:text-indigo-500"
                                  : "text-muted-foreground/40 hover:text-indigo-500/70"
                              )}
                            />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" className="w-56">
                          <DropdownMenuLabel className="text-[10px] font-bold uppercase tracking-tighter text-muted-foreground">시간표 선택</DropdownMenuLabel>
                          <DropdownMenuSeparator />
                          {Array.isArray(timetableList) && timetableList.map((t) => {
                            // Since we don't have per-timetable detail here, 
                            // we'd ideally need a way to check if course is in t.id
                            // For now, let's keep it simple as an Add action or 
                            // add a note that toggling requires more data.
                            return (
                              <DropdownMenuItem 
                                key={t.id}
                                className="flex items-center justify-between cursor-pointer"
                                onClick={() => {
                                  addToTimetable({ timetableId: t.id, courseKey: course.courseKey });
                                }}
                              >
                                <div className="flex items-center gap-2">
                                  <span className="text-xs font-medium">{t.name}</span>
                                  {t.isPrimary && <span className="text-[9px] px-1 bg-indigo-500/10 text-indigo-500 rounded-sm font-bold">대표</span>}
                                </div>
                              </DropdownMenuItem>
                            );
                          })}
                          {(!Array.isArray(timetableList) || timetableList.length === 0) && (
                            <div className="p-2 text-[10px] text-center text-muted-foreground">시간표가 없습니다.</div>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
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
