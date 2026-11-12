import { memo, useEffect, useRef, useState } from "react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Filter, RotateCcw, Search, SlidersHorizontal, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/shared/lib/utils";
import type { CourseSearchCondition } from "@/shared/types/api";
import { DEFAULT_CONDITION } from "../constants/course-options";
import { CourseBasicFilters } from "./filters/course-basic-filters";
import { CourseDetailFilters } from "./filters/course-detail-filters";
import { CourseSmartFilters } from "./filters/course-smart-filters";
import { FilterSection } from "./filters/filter-section";
import type { TimetableResponse, User } from "@/shared/types/api";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
  onConditionChange?: (condition: CourseSearchCondition) => void;
  isLoading?: boolean;
  initialCondition?: CourseSearchCondition;
  defaultCondition?: CourseSearchCondition;
  hideHeader?: boolean;
  initialUser?: User | null;
  initialTimetables?: TimetableResponse[];
  skipPersonalFetch?: boolean;
}

export const CourseSearchBar = memo(function CourseSearchBar({
  onSearch,
  onConditionChange,
  isLoading,
  initialCondition,
  defaultCondition,
  hideHeader,
  initialUser,
  initialTimetables,
  skipPersonalFetch,
}: CourseSearchBarProps) {
  const resolvedDefaultCondition: CourseSearchCondition = defaultCondition ?? DEFAULT_CONDITION;
  const initialSnapshot = JSON.stringify(initialCondition ?? resolvedDefaultCondition);
  const [condition, setCondition] = useState<CourseSearchCondition>(
    () => ({ ...(initialCondition ?? resolvedDefaultCondition) }),
  );
  const [keyword, setKeyword] = useState(() => initialCondition?.keyword || "");
  const prevInitialRef = useRef(initialSnapshot);
  const isFocusedRef = useRef(false);
  const isSyncingRef = useRef(false);

  useEffect(() => {
    const nextSnapshot = JSON.stringify(initialCondition ?? resolvedDefaultCondition);
    if (nextSnapshot === prevInitialRef.current) return;

    prevInitialRef.current = nextSnapshot;
    const nextCond: CourseSearchCondition = initialCondition ?? resolvedDefaultCondition;

    isSyncingRef.current = true;
    setCondition({ ...nextCond });
    if (!isFocusedRef.current) {
      setKeyword(nextCond.keyword || "");
    }
  }, [initialCondition, resolvedDefaultCondition]);

  const [smartOpen, setSmartOpen] = useState(true);
  const [basicOpen, setBasicOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(true);
  const [scheduleOpen, setScheduleOpen] = useState(false);

  useEffect(() => {
    if (isSyncingRef.current) {
      isSyncingRef.current = false;
      return;
    }
    onConditionChange?.(condition);
  }, [condition, onConditionChange]);

  const handleSearch = () => {
    if (!condition.academicYear || !condition.semester) {
      toast.error("학년도와 학기를 선택해주세요.");
      return;
    }
    onSearch({ ...condition, keyword: keyword || undefined });
  };

  const handleReset = () => {
    const resetCond = { ...resolvedDefaultCondition };
    setCondition(resetCond);
    setKeyword("");
    onSearch(resetCond);
    toast.success("검색 조건을 초기화했습니다.");
  };

  return (
    <div className={cn("flex min-h-0 flex-col", !hideHeader && "h-full")}>
      {!hideHeader && (
        <div className="space-y-2 pb-3">
          <div className="flex items-center gap-2">
            <div className="relative min-w-0 flex-1">
              <Input
                type="search"
                aria-label="강의명"
                placeholder="강의명"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onFocus={() => { isFocusedRef.current = true; }}
                onBlur={() => { isFocusedRef.current = false; }}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                className="h-10 rounded-lg bg-card pl-9"
              />
              <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            </div>
            <Button
              type="button"
              onClick={handleSearch}
              disabled={isLoading}
              className="h-10 rounded-lg"
            >
              {isLoading ? (
                <span className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
              ) : (
                "검색"
              )}
            </Button>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={handleReset}
            disabled={isLoading}
            className="w-full rounded-lg bg-card text-muted-foreground"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            검색 조건 초기화
          </Button>
        </div>
      )}

      <div className={cn("space-y-2", !hideHeader ? "flex-1 overflow-y-auto" : "h-auto overflow-visible")}>
        {               }
        <FilterSection
          idBase="course-search-smart"
          title="스마트 필터"
          icon={<Sparkles className="size-4 text-primary" />}
          open={smartOpen}
          onOpenChange={setSmartOpen}
        >
          <CourseSmartFilters
            condition={condition}
            setCondition={setCondition}
            scheduleOpen={scheduleOpen}
            setScheduleOpen={setScheduleOpen}
            initialUser={initialUser}
            initialTimetables={initialTimetables}
            skipPersonalFetch={skipPersonalFetch}
          />
        </FilterSection>

        {              }
        <FilterSection
          idBase="course-search-basic"
          title="기본 정보"
          icon={<Filter className="h-4 w-4 text-primary/80" />}
          open={basicOpen}
          onOpenChange={setBasicOpen}
        >
          <CourseBasicFilters
            condition={condition}
            setCondition={setCondition}
            idBasePrefix={hideHeader ? "course-search-mobile-basic" : "course-search-basic"}
          />
        </FilterSection>

        {              }
        <FilterSection
          idBase="course-search-detail"
          title="강의 상세"
          icon={<SlidersHorizontal className="h-4 w-4 text-primary/80" />}
          open={detailOpen}
          onOpenChange={setDetailOpen}
        >
          <CourseDetailFilters
            condition={condition}
            setCondition={setCondition}
          />
        </FilterSection>

        {hideHeader && (
          <div className="grid grid-cols-3 gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={handleReset}
              disabled={isLoading}
              className="col-span-1 h-10 gap-1 rounded-lg px-2 text-muted-foreground"
            >
              <RotateCcw aria-hidden="true" />
              초기화
            </Button>
            <Button
              type="button"
              onClick={handleSearch}
              disabled={isLoading}
              className="col-span-2 h-10 rounded-lg"
            >
              {isLoading ? (
                <span className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
              ) : (
                <>
                  <Search aria-hidden="true" />
                  필터 적용
                </>
              )}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
});
