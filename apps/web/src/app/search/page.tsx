"use client";

import { Header } from "@/components/layout/header";
import { CourseSearchBar } from "@/components/features/course/course-search-bar";
import { CourseTable } from "@/components/features/course/course-table";
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
      <main className="container py-8">
        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-bold mb-2">강좌 검색</h1>
            <p className="text-muted-foreground">
              원하는 강좌를 검색하고 구독하여 빈자리 알림을 받아보세요.
            </p>
          </div>

          <CourseSearchBar onSearch={handleSearch} />

          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : error ? (
            <div className="text-center py-12">
              <p className="text-red-600">강좌 검색에 실패했습니다.</p>
            </div>
          ) : data ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  총 {data.totalElements}개의 강좌가 검색되었습니다.
                </p>
              </div>
              <CourseTable courses={data.content} />
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              검색어를 입력하여 강좌를 검색하세요.
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
