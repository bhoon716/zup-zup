"use client";

// Header removed (Global layout usage)
import { CourseSearchBar } from "@/components/features/course/course-search-bar";
import { CourseTable } from "@/components/features/course/course-table";
import { CourseTableSkeleton } from "@/components/features/course/course-table-skeleton";
import { useCourses } from "@/hooks/useCourses";
import { useState, useCallback } from "react";
import type { CourseSearchCondition } from "@/types/api";

export const dynamic = 'force-dynamic';

export default function SearchPage() {
  const [searchCondition, setSearchCondition] = useState<CourseSearchCondition>({});
  const { 
    data, 
    isLoading, 
    error, 
    fetchNextPage, 
    hasNextPage, 
    isFetchingNextPage 
  } = useCourses(searchCondition);

  const handleSearch = useCallback((condition: CourseSearchCondition) => {
    setSearchCondition(condition);
  }, []);

  const allCourses = data?.pages.flatMap((page) => page.content) || [];

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      
      <main className="container py-10">
        <div className="space-y-8">
          <div className="flex flex-col gap-1.5">
            <h1 className="text-3xl font-black tracking-tight bg-gradient-to-br from-foreground to-foreground/70 bg-clip-text text-transparent">
              강의 검색
            </h1>
            <p className="text-sm text-muted-foreground font-medium">
              원하는 강의를 검색하고 구독하여 빈자리 알림을 받아보세요.
            </p>
          </div>

          <CourseSearchBar onSearch={handleSearch} isLoading={isLoading} />

          {isLoading ? (
            <CourseTableSkeleton />
          ) : error ? (
            <div className="text-center py-20 bg-destructive/5 rounded-3xl border border-destructive/10 animate-in fade-in zoom-in duration-300">
              <p className="text-destructive font-bold">강의 검색에 실패했습니다.</p>
            </div>
          ) : allCourses ? (
            <div className="space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <CourseTable 
                courses={allCourses} 
                onLoadMore={fetchNextPage} 
                hasMore={hasNextPage} 
                isFetchingNextPage={isFetchingNextPage}
              />
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground animate-in fade-in duration-300">
              검색어를 입력하여 강의를 검색하세요.
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
