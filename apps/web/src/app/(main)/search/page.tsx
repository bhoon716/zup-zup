"use client";

import { useCallback, useMemo, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CourseSearchBar } from "@/features/course/components/course-search-bar";
import { CourseTable } from "@/features/course/components/course-table";
import { CourseTableSkeleton } from "@/features/course/components/course-table-skeleton";
import { useCourses } from "@/features/course/hooks/useCourses";
import type { CourseSearchCondition } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/shared/ui/sheet";
import { SlidersHorizontal, X, Search, ChevronRight, ListFilter } from "lucide-react";

export const dynamic = "force-dynamic";

const DEFAULT_CONDITION: CourseSearchCondition = {
  academicYear: "2026",
  semester: "U211600010",
};

interface FilterChip {
  id: string;
  label: string;
  patch: Partial<CourseSearchCondition>;
}

/**
 * 강의 검색 페이지 메인 컴포넌트
 */
export default function SearchPage() {
  const [searchCondition, setSearchCondition] = useState<CourseSearchCondition>(DEFAULT_CONDITION);
  const [sortOption, setSortOption] = useState<string>("name");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const [isFilterExpanded, setIsFilterExpanded] = useState(false);

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useCourses({
    ...searchCondition,
    sortBy: sortOption,
    sortOrder,
  });

  const handleSearch = useCallback((condition: CourseSearchCondition) => {
    setSearchCondition(condition);
    setIsFilterExpanded(false);
  }, []);

  /**
   * 무한 페이징 데이터를 평탄화하여 전체 강의 리스트 생성
   */
  const allCourses = useMemo(
    () => data?.pages.flatMap((page) => page.content) || [],
    [data],
  );

  /**
   * 검색 조건이 변경될 때마다 갱신되는 고유 키
   */
  const searchConditionKey = useMemo(
    () => JSON.stringify({ ...searchCondition, sortOption, sortOrder }),
    [searchCondition, sortOption, sortOrder],
  );

  /**
   * 활성화된 필터들을 칩(Chip) 형태로 표시하기 위한 데이터 가공
   */
  const activeFilters = useMemo<FilterChip[]>(() => {
    const filters: FilterChip[] = [];

    if (searchCondition.department) {
      filters.push({
        id: "department",
        label: searchCondition.department,
        patch: { department: undefined },
      });
    }

    if (searchCondition.classification) {
      filters.push({
        id: "classification",
        label: searchCondition.classification,
        patch: { classification: undefined, generalCategory: undefined, generalDetail: undefined },
      });
    }

    if (searchCondition.name) {
      filters.push({
        id: "name",
        label: `강의명: ${searchCondition.name}`,
        patch: { name: undefined },
      });
    }

    if (searchCondition.professor) {
      filters.push({
        id: "professor",
        label: `교수: ${searchCondition.professor}`,
        patch: { professor: undefined },
      });
    }

    if (searchCondition.isAvailableOnly) {
      filters.push({
        id: "available",
        label: "여석 있는 강의",
        patch: { isAvailableOnly: undefined },
      });
    }

    if (searchCondition.isWishedOnly) {
      filters.push({
        id: "wished",
        label: "관심 강의만",
        patch: { isWishedOnly: undefined },
      });
    }

    if (searchCondition.selectedSchedules && searchCondition.selectedSchedules.length > 0) {
      filters.push({
        id: "schedule",
        label: `시간대 ${searchCondition.selectedSchedules.length}칸`,
        patch: { selectedSchedules: undefined },
      });
    }

    return filters;
  }, [searchCondition]);

  const [keyword, setKeyword] = useState(searchCondition.name || "");

  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (searchCondition.classification) count++;
    if (searchCondition.gradingMethod) count++;
    if (searchCondition.credits) count++;
    if (searchCondition.department) count++;
    if (searchCondition.lectureLanguage) count++;
    if (searchCondition.status) count++;
    if (searchCondition.selectedSchedules?.length) count++;
    if (searchCondition.disclosure === "공개") count++;
    return count;
  }, [searchCondition]);

  /**
   * 키워드 입력을 통한 검색을 처리합니다.
   * 입력된 강의명을 검색 조건에 반영합니다.
   */

  /**
   * 개별 필터 칩 삭제 핸들러입니다.
   * 선택된 필터 속성만 검색 조건에서 제거합니다.
   */
  const clearSingleFilter = useCallback((patch: Partial<CourseSearchCondition>) => {
    setSearchCondition((prev) => ({ ...prev, ...patch }));
  }, []);

  /**
   * 모든 필터 초기화 핸들러입니다.
   * 검색 조건을 초기 상태로 되돌리고 입력 중인 키워드도 비웁니다.
   */
  const resetAllFilters = useCallback(() => {
    setSearchCondition(DEFAULT_CONDITION);
    setKeyword("");
  }, []);

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
      {/* 모바일 검색 영역 (상단) */}
      <div className="sticky top-16 z-30 border-b border-border/50 bg-white/95 px-4 py-4 backdrop-blur-md lg:hidden">
        <div className="space-y-3">
          {/* 강의명 검색창 */}
          <form 
            onSubmit={(e) => {
              e.preventDefault();
              handleSearch({ ...searchCondition, name: keyword || undefined });
            }} 
            className="flex items-center gap-2"
          >
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="찾으시는 강의명을 입력하세요"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="h-11 w-full rounded-2xl border-none bg-muted/80 pl-11 pr-4 text-sm font-bold placeholder:text-muted-foreground/60 focus:bg-white focus:ring-2 focus:ring-primary/20"
              />
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            </div>
            <Button 
              type="submit" 
              disabled={isLoading}
              className="h-11 rounded-2xl bg-primary px-5 font-bold text-white shadow-lg shadow-primary/20"
            >
              {isLoading ? (
                <span className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
              ) : (
                "검색"
              )}
            </Button>
          </form>

          {/* 상세 검색 필터 토글 버튼 */}
          <button
            type="button"
            onClick={() => setIsFilterExpanded(!isFilterExpanded)}
            className="flex w-full items-center justify-between rounded-xl bg-muted/50 px-4 py-3 text-sm font-bold text-foreground transition-colors hover:bg-muted"
          >
            <div className="flex items-center gap-2">
              <SlidersHorizontal className="h-4 w-4 text-primary" />
              상세 검색 필터
              {activeFiltersCount > 0 && (
                <span className="ml-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] text-white">
                  {activeFiltersCount}
                </span>
              )}
            </div>
            <motion.div
              animate={{ rotate: isFilterExpanded ? 90 : 0 }}
              transition={{ duration: 0.2 }}
            >
              <ChevronRight className="h-5 w-5 text-muted-foreground" />
            </motion.div>
          </button>

          {/* 펼쳐지는 상세 필터 본문 */}
          <AnimatePresence>
            {isFilterExpanded && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: "auto", opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                transition={{ 
                  height: { duration: 0.3, ease: [0.04, 0.62, 0.23, 0.98] },
                  opacity: { duration: 0.2 }
                }}
                className="overflow-y-auto overflow-x-hidden max-h-[70vh] px-0.5 pb-2 will-change-[height]"
              >
                <div className="rounded-2xl border border-border/70 bg-white p-4 shadow-lg my-1 transform-gpu">
                  <CourseSearchBar
                    key="mobile-search-bar"
                    onSearch={handleSearch}
                    isLoading={isLoading}
                    initialCondition={searchCondition}
                    hideHeader
                  />
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      <motion.main 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="container py-6 lg:py-8"
      >
        <div className="grid gap-6 lg:grid-cols-[340px_minmax(0,1fr)] xl:gap-8">
          <aside className="hidden lg:block">
            <div className="sticky top-20 h-[calc(100vh-6rem)] rounded-3xl border border-border/70 bg-white/85 p-4 shadow-[0_14px_28px_rgba(15,23,42,0.06)] backdrop-blur">
              <CourseSearchBar
                key="desktop-search-bar"
                onSearch={handleSearch}
                isLoading={isLoading}
                initialCondition={searchCondition}
              />
            </div>
          </aside>

          <section className="min-w-0 space-y-6">
            <div className="flex flex-col gap-4">
              <div className="flex items-center justify-between">
                <h1 className="text-xl font-bold text-foreground">
                  {searchCondition.name || searchCondition.professor 
                    ? `"${searchCondition.name || searchCondition.professor}"` 
                    : "강의 목록"}
                </h1>

                <div className="flex items-center gap-2">
                  <Select
                    value={sortOption}
                    onValueChange={(value) => setSortOption(value)}
                  >
                    <SelectTrigger className="h-9 min-w-[120px] rounded-lg border-border/60 bg-transparent text-xs font-medium">
                      <SelectValue placeholder="정렬" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="name">강의명</SelectItem>
                      <SelectItem value="popular">인기(찜)</SelectItem>
                      <SelectItem value="current">현재 신청 인원</SelectItem>
                      <SelectItem value="available">여석 수</SelectItem>
                    </SelectContent>
                  </Select>

                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="h-9 w-9 rounded-lg border-border/60 text-muted-foreground"
                    onClick={() => setSortOrder((prev) => (prev === "asc" ? "desc" : "asc"))}
                    title={sortOrder === "asc" ? "오름차순 (작은 값 우선)" : "내림차순 (큰 값 우선)"}
                  >
                    {sortOrder === "asc" ? (
                      <ListFilter className="h-4 w-4 rotate-180" />
                    ) : (
                      <ListFilter className="h-4 w-4" />
                    )}
                  </Button>
                </div>
              </div>

              <AnimatePresence mode="popLayout">
                {activeFilters.length > 0 && (
                  <motion.div 
                    layout
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    className="flex flex-wrap items-center gap-1.5"
                  >
                    {activeFilters.map((filter) => (
                      <motion.div
                        layout
                        key={filter.id}
                        initial={{ opacity: 0, scale: 0.8 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.8 }}
                        className="flex items-center gap-1 rounded-md border border-border/50 bg-muted/30 px-2 py-1 text-[11px] font-medium text-muted-foreground"
                      >
                        {filter.label}
                        <button
                          type="button"
                          className="ml-0.5 transition-colors hover:text-foreground"
                          onClick={() => clearSingleFilter(filter.patch)}
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </motion.div>
                    ))}
                    <motion.div layout>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-6 px-1.5 text-[11px] font-medium text-muted-foreground/60 hover:text-primary"
                        onClick={resetAllFilters}
                      >
                        초기화
                      </Button>
                    </motion.div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {isLoading ? (
              <CourseTableSkeleton />
            ) : error ? (
              <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-20 text-center">
                <p className="text-sm font-bold text-destructive">강의 검색에 실패했습니다.</p>
              </div>
            ) : (
              <CourseTable
                key={searchConditionKey}
                courses={allCourses}
                onLoadMore={fetchNextPage}
                hasMore={hasNextPage}
                isFetchingNextPage={isFetchingNextPage}
              />
            )}
          </section>
        </div>
      </motion.main>
    </div>
  );
}
