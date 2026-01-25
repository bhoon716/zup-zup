"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { useSubscribe, useSubscriptions } from "@/hooks/useSubscriptions";
import type { Course } from "@/types/api";
import { Check } from "lucide-react";
import { CourseDetailDialog } from "./course-detail-dialog";
import { useState } from "react";

interface CourseTableProps {
  courses: Course[];
}

export function CourseTable({ courses }: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions();
  const { mutate: subscribe, isPending } = useSubscribe();
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [isDialogOpen, setIsDialogOpen] = useState(false);

  const isSubscribed = (courseKey: string) => {
    return subscriptions?.some((sub) => sub.courseKey === courseKey);
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
        <Table>
          <TableHeader className="sticky top-0 z-10 bg-background/80 backdrop-blur-md shadow-sm">
            <TableRow className="hover:bg-transparent border-b border-white/10">
              <TableHead className="w-[100px] text-[11px] font-black uppercase tracking-wider">과목코드</TableHead>
              <TableHead className="text-[11px] font-black uppercase tracking-wider">강좌명</TableHead>
              <TableHead className="text-[11px] font-black uppercase tracking-wider">교수명</TableHead>
              <TableHead className="text-center text-[11px] font-black uppercase tracking-wider">이수구분</TableHead>
              <TableHead className="text-center text-[11px] font-black uppercase tracking-wider">언어</TableHead>
              <TableHead className="text-center text-[11px] font-black uppercase tracking-wider">정원</TableHead>
              <TableHead className="text-center text-[11px] font-black uppercase tracking-wider">여석</TableHead>
              <TableHead className="text-center text-[11px] font-black uppercase tracking-wider">상태</TableHead>
              <TableHead className="text-center w-[120px] text-[11px] font-black uppercase tracking-wider">구독</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {courses.length === 0 ? (
              <TableRow>
                <TableCell colSpan={9} className="text-center py-20 text-muted-foreground italic font-medium">
                  검색 조건에 맞는 강좌가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              courses.map((course) => {
                const subscribed = isSubscribed(course.courseKey);
                const isAvailable = (course.available ?? 0) > 0;

                return (
                  <TableRow 
                    key={course.courseKey} 
                    className="group hover:bg-primary/5 transition-colors border-white/5 cursor-pointer"
                    onClick={() => handleCourseClick(course)}
                  >
                    <TableCell className="font-mono text-[10px] text-muted-foreground/60 transition-colors group-hover:text-primary">
                      {course.subjectCode}
                    </TableCell>
                    <TableCell className="font-semibold">
                      <div className="flex flex-col gap-0.5">
                        <span
                          className="text-foreground hover:text-primary transition-colors truncate max-w-[200px]"
                        >
                          {course.name}
                        </span>
                        <span className="text-[10px] text-muted-foreground/50 font-medium group-hover:text-muted-foreground transition-colors truncate max-w-[180px]">
                          {course.department}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="text-[13px] font-medium text-foreground/80">{course.professor || "-"}</TableCell>
                    <TableCell className="text-center">
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-lg bg-secondary/30 text-secondary-foreground border border-white/5 group-hover:border-primary/20 transition-all">
                        {course.classification || "-"}
                      </span>
                    </TableCell>
                    <TableCell className="text-center text-xs font-medium text-muted-foreground/70">{course.lectureLanguage || "-"}</TableCell>
                    <TableCell className="text-center text-xs font-bold text-foreground/70">{course.capacity || 0}</TableCell>
                    <TableCell className="text-center font-black">
                      <span className={isAvailable ? "text-primary drop-shadow-[0_0_8px_rgba(var(--primary),0.2)]" : "text-destructive/70"}>
                        {course.available || 0}
                      </span>
                    </TableCell>
                    <TableCell className="text-center">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-tighter ${
                          isAvailable
                            ? "bg-primary/10 text-primary border border-primary/20"
                            : "bg-destructive/10 text-destructive/70 border border-destructive/10"
                        }`}
                      >
                        {isAvailable ? "Available" : "Full"}
                      </span>
                    </TableCell>
                    <TableCell className="text-center" onClick={(e) => e.stopPropagation()}>
                      {subscribed ? (
                        <div className="flex items-center justify-center gap-1.5 py-1 text-primary/70 font-bold text-[10px] uppercase tracking-wider bg-primary/5 rounded-lg border border-primary/10">
                          <Check className="w-3 h-3" />
                          <span>Subscribed</span>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          onClick={() => handleSubscribe(course.courseKey)}
                          disabled={isPending}
                          className="h-8 w-full rounded-lg bg-primary hover:bg-primary/90 text-primary-foreground font-bold text-[11px] transition-all"
                        >
                          구독
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
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
