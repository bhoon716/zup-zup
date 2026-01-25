"use client";

import { Header } from "@/components/layout/header";
import { CourseSearchBar } from "@/components/features/course/course-search-bar";
import { CourseTable } from "@/components/features/course/course-table";
import { CourseTableSkeleton } from "@/components/features/course/course-table-skeleton";
import { useCourses } from "@/hooks/useCourses";
import { useState, useCallback } from "react";
import type { CourseSearchCondition } from "@/types/api";
import { Loader2 } from "lucide-react";

export const dynamic = 'force-dynamic';

export default function SearchPage() {
  const [searchCondition, setSearchCondition] = useState<CourseSearchCondition>({});
  const { data, isLoading, error } = useCourses(searchCondition);

  const handleSearch = useCallback((condition: CourseSearchCondition) => {
    setSearchCondition(condition);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="container py-10">
        <div className="space-y-8">
          <div className="flex flex-col gap-1.5">
            <h1 className="text-3xl font-black tracking-tight bg-gradient-to-br from-foreground to-foreground/70 bg-clip-text text-transparent">
              강좌 검색
            </h1>
            <p className="text-sm text-muted-foreground font-medium">
              원하는 강좌를 검색하고 구독하여 빈자리 알림을 받아보세요.
            </p>
          </div>

          <CourseSearchBar onSearch={handleSearch} isLoading={isLoading} />

          {isLoading ? (
            <CourseTableSkeleton />
          ) : error ? (
            <div className="text-center py-20 bg-destructive/5 rounded-3xl border border-destructive/10 animate-in fade-in zoom-in duration-300">
              <p className="text-destructive font-bold">강좌 검색에 실패했습니다.</p>
            </div>
          ) : data ? (
            <div className="space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
              <div className="flex items-center justify-between px-1">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-primary/60" />
                  <p className="text-[13px] font-bold text-muted-foreground">
                    총 <span className="text-foreground">{data.length}</span>개의 강좌 검색됨
                  </p>
                </div>
              </div>
              <CourseTable courses={data} />
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground animate-in fade-in duration-300">
              검색어를 입력하여 강좌를 검색하세요.
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
