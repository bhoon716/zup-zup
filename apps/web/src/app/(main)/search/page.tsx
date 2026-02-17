"use client";

import { useCallback, useMemo, useState } from "react";
import { CourseSearchBar } from "@/features/course/components/course-search-bar";
import { CourseTable } from "@/features/course/components/course-table";
import { CourseTableSkeleton } from "@/features/course/components/course-table-skeleton";
import { useCourses } from "@/features/course/hooks/useCourses";
import type { Course, CourseSearchCondition } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
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
import { Filter, SlidersHorizontal, X } from "lucide-react";

export const dynamic = "force-dynamic";

type SortOption = "recommended" | "name" | "credits" | "available";

const DEFAULT_CONDITION: CourseSearchCondition = {
  academicYear: "2026",
  semester: "U211600010",
};

interface FilterChip {
  id: string;
  label: string;
  patch: Partial<CourseSearchCondition>;
}

function sortCourses(courses: Course[], sortOption: SortOption) {
  const sorted = [...courses];

  if (sortOption === "name") {
    sorted.sort((a, b) => a.name.localeCompare(b.name, "ko"));
    return sorted;
  }

  if (sortOption === "credits") {
    sorted.sort((a, b) => (Number(b.credits) || 0) - (Number(a.credits) || 0));
    return sorted;
  }

  if (sortOption === "available") {
    sorted.sort((a, b) => (b.available || 0) - (a.available || 0));
    return sorted;
  }

  return sorted;
}

export default function SearchPage() {
  const [searchCondition, setSearchCondition] = useState<CourseSearchCondition>(DEFAULT_CONDITION);
  const [sortOption, setSortOption] = useState<SortOption>("recommended");
  const [isFilterSheetOpen, setIsFilterSheetOpen] = useState(false);

  const {
    data,
    isLoading,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useCourses(searchCondition);

  const handleSearch = useCallback((condition: CourseSearchCondition) => {
    setSearchCondition(condition);
  }, []);

  const handleSearchFromSheet = useCallback((condition: CourseSearchCondition) => {
    setSearchCondition(condition);
    setIsFilterSheetOpen(false);
  }, []);

  const allCourses = useMemo(
    () => data?.pages.flatMap((page) => page.content) || [],
    [data],
  );

  const sortedCourses = useMemo(
    () => sortCourses(allCourses, sortOption),
    [allCourses, sortOption],
  );
  const searchConditionKey = useMemo(
    () => JSON.stringify(searchCondition),
    [searchCondition],
  );

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

  const clearSingleFilter = useCallback((patch: Partial<CourseSearchCondition>) => {
    setSearchCondition((prev) => ({ ...prev, ...patch }));
  }, []);

  const resetAllFilters = useCallback(() => {
    setSearchCondition(DEFAULT_CONDITION);
  }, []);

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
      <main className="container py-6 lg:py-8">
        <div className="grid gap-6 lg:grid-cols-[340px_minmax(0,1fr)] xl:gap-8">
          <aside className="hidden lg:block">
            <div className="sticky top-20 h-[calc(100vh-6rem)] rounded-3xl border border-border/70 bg-white/85 p-4 shadow-[0_14px_28px_rgba(15,23,42,0.06)] backdrop-blur">
              <CourseSearchBar
                key={`desktop-${searchConditionKey}`}
                onSearch={handleSearch}
                isLoading={isLoading}
                initialCondition={searchCondition}
              />
            </div>
          </aside>

          <section className="min-w-0 space-y-4">
            <div className="rounded-2xl border border-border/70 bg-white px-4 py-4 shadow-sm md:px-5">
              <div className="mb-3 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h1 className="text-2xl font-black tracking-tight text-foreground md:text-3xl">
                    검색 결과
                  </h1>
                  <p className="mt-1 text-sm text-muted-foreground">
                    조건에 맞는 <span className="font-bold text-primary">{sortedCourses.length}</span>개 강의가 표시되고 있습니다.
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <Select
                    value={sortOption}
                    onValueChange={(value) => setSortOption(value as SortOption)}
                  >
                    <SelectTrigger className="h-10 min-w-[160px] rounded-xl bg-white text-sm font-medium">
                      <SelectValue placeholder="정렬" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="recommended">추천순</SelectItem>
                      <SelectItem value="name">강의명 (가나다)</SelectItem>
                      <SelectItem value="credits">학점 (높은순)</SelectItem>
                      <SelectItem value="available">여석순</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {activeFilters.length > 0 && (
                <div className="flex flex-wrap items-center gap-2 border-t border-border/70 pt-3">
                  {activeFilters.map((filter) => (
                    <span
                      key={filter.id}
                      className="inline-flex items-center gap-1 rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-semibold text-primary"
                    >
                      {filter.label}
                      <button
                        type="button"
                        className="rounded-full p-0.5 transition-colors hover:bg-primary/20"
                        onClick={() => clearSingleFilter(filter.patch)}
                        aria-label={`${filter.label} 필터 해제`}
                      >
                        <X className="h-3.5 w-3.5" />
                      </button>
                    </span>
                  ))}

                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 rounded-full px-2 text-xs font-semibold text-muted-foreground underline-offset-4 hover:text-primary hover:underline"
                    onClick={resetAllFilters}
                  >
                    필터 초기화
                  </Button>
                </div>
              )}
            </div>

            {isLoading ? (
              <CourseTableSkeleton />
            ) : error ? (
              <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-20 text-center">
                <p className="text-sm font-bold text-destructive">강의 검색에 실패했습니다.</p>
              </div>
            ) : (
              <CourseTable
                courses={sortedCourses}
                onLoadMore={fetchNextPage}
                hasMore={hasNextPage}
                isFetchingNextPage={isFetchingNextPage}
              />
            )}
          </section>
        </div>
      </main>

      <Sheet open={isFilterSheetOpen} onOpenChange={setIsFilterSheetOpen}>
        <SheetTrigger asChild>
          <Button
            type="button"
            size="icon"
            className="fixed bottom-6 right-5 z-40 h-14 w-14 rounded-full bg-primary text-white shadow-xl shadow-primary/30 hover:bg-primary/90 lg:hidden"
            aria-label="필터 열기"
          >
            <Filter className="h-6 w-6" />
          </Button>
        </SheetTrigger>

        <SheetContent side="right" className="w-[92vw] border-l border-border bg-[#f8fafc] p-0 sm:max-w-md">
          <div className="flex h-full min-h-0 flex-col p-4">
            <SheetHeader className="pb-3 text-left">
              <SheetTitle className="flex items-center gap-2 text-lg font-black">
                <SlidersHorizontal className="h-5 w-5 text-primary" />
                검색 필터
              </SheetTitle>
            </SheetHeader>

            <div className="min-h-0 flex-1 overflow-hidden rounded-2xl border border-border/70 bg-white p-3">
              <CourseSearchBar
                key={`mobile-${searchConditionKey}`}
                onSearch={handleSearchFromSheet}
                isLoading={isLoading}
                initialCondition={searchCondition}
              />
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
