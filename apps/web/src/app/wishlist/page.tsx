"use client";

import { useWishlist } from "@/hooks/useWishlist";
import { CourseTable } from "@/components/features/course/course-table";
import { Loader2 } from "lucide-react";
import type { Course } from "@/types/api";

export default function WishlistPage() {
  const { data: wishlist, isLoading } = useWishlist();

  // Convert WishlistResponse to Course to reuse CourseTable
  // Some fields might be missing from WishlistResponse compared to Course, 
  // but for the table view, we mapped most essential ones.
  const courses: Course[] = wishlist?.map(item => ({
      courseKey: item.courseKey,
      subjectCode: item.subjectCode,
      classNumber: item.classNumber,
      name: item.courseName,
      professor: item.professor,
      classification: item.classification as Course['classification'],
      credits: item.credits,
      classTime: item.classTime,
      current: item.current,
      capacity: item.capacity,
      available: item.available,
      lectureLanguage: '한국어', // Default or need to add to DB/DTO if important
      // ... minimal mapping
  } as Course)) || [];

  return (
    <div className="container py-10 space-y-6">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-black tracking-tight">예비 수강 바구니</h1>
        <p className="text-muted-foreground text-sm font-medium">
          관심 있는 강의를 모아보고 시간표를 시뮬레이션하세요.
        </p>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-20">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      ) : (
        <CourseTable courses={courses} />
      )}
    </div>
  );
}
