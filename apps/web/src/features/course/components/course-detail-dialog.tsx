"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from "@/shared/ui/dialog";
import type { Course } from "@/shared/types/api";
import { useCourseDetail } from "@/features/course/hooks/useCourses";
import { Loader2 } from "lucide-react";
import type { RefObject } from "react";

import { CourseDetailContent } from "./course-detail-content";

interface CourseDetailDialogProps {
  course: Course | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  unmaskPublicCourseInfo?: boolean;
  returnFocusRef?: RefObject<HTMLButtonElement | null>;
}

export function CourseDetailDialog({ course, open, onOpenChange, unmaskPublicCourseInfo = false, returnFocusRef }: CourseDetailDialogProps) {
  const courseKey = course?.courseKey ?? "";
  const { data: detailedCourse, isLoading } = useCourseDetail(open ? courseKey : "");
  if (!course) return null;
  const displayCourse = detailedCourse ?? course;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        className="flex flex-col gap-0 overflow-hidden rounded-2xl bg-card p-0 sm:max-w-3xl"
        onCloseAutoFocus={(event) => {
          const trigger = returnFocusRef?.current;
          if (!trigger?.isConnected) return;
          event.preventDefault();
          trigger.focus();
        }}
      >
        <DialogTitle className="sr-only">강의 상세 정보</DialogTitle>
        <DialogDescription className="sr-only">
          선택한 강의의 상세 정보를 확인하는 대화상자입니다.
        </DialogDescription>
        <div className="max-h-[calc(100dvh-2rem)] w-full overflow-y-auto overscroll-contain">
          {isLoading ? (
            <div className="flex items-center justify-center p-20 min-h-[400px]">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
            </div>
          ) : (
            <CourseDetailContent course={displayCourse} unmaskPublicCourseInfo={unmaskPublicCourseInfo} />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
