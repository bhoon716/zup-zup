"use client";

import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "@/shared/ui/dialog";
import type { Course } from "@/shared/types/api";
import { useCourseDetail } from "@/features/course/hooks/useCourses";
import { Loader2 } from "lucide-react";

import { CourseDetailContent } from "./course-detail-content";

interface CourseDetailDialogProps {
  course: Course | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CourseDetailDialog({ course, open, onOpenChange }: CourseDetailDialogProps) {
  const courseKey = course?.courseKey ?? "";
  const { data: detailedCourse, isLoading } = useCourseDetail(open ? courseKey : "");
  if (!course) return null;
  const displayCourse = (detailedCourse ?? course) as Course;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl p-0 overflow-hidden border-0 bg-transparent shadow-none flex flex-col">
        <DialogTitle className="sr-only">강의 상세 정보</DialogTitle>
        <div className="relative w-full bg-white dark:bg-[#121212] rounded-3xl overflow-y-auto shadow-2xl flex flex-col border border-gray-100 dark:border-gray-800 max-h-[90vh]">
          {isLoading ? (
            <div className="flex items-center justify-center p-20 min-h-[400px]">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
            </div>
          ) : (
            <CourseDetailContent course={displayCourse} />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
