"use client";

import { useState } from "react";
import { Button } from "@/shared/ui/button";
import { Filter, RotateCcw, Search, SlidersHorizontal, Sparkles } from "lucide-react";
import { toast } from "sonner";
import type { CourseSearchCondition } from "@/shared/types/api";
import { DEFAULT_CONDITION } from "../constants/course-options";
import { CourseBasicFilters } from "./filters/course-basic-filters";
import { CourseDetailFilters } from "./filters/course-detail-filters";
import { CourseSmartFilters } from "./filters/course-smart-filters";
import { FilterSection } from "./filters/filter-section";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
  isLoading?: boolean;
  initialCondition?: CourseSearchCondition;
}

export function CourseSearchBar({
  onSearch,
  isLoading,
  initialCondition,
}: CourseSearchBarProps) {
  const [condition, setCondition] = useState<CourseSearchCondition>(
    () => ({ ...(initialCondition ?? DEFAULT_CONDITION) }),
  );
  
  // UI 상태: 접기/펼치기
  const [smartOpen, setSmartOpen] = useState(true);
  const [basicOpen, setBasicOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(true);
  const [scheduleOpen, setScheduleOpen] = useState(false);

  // UI 상태: 계층형 필터
  const [classificationType, setClassificationType] = useState<string | undefined>();
  const [gradingType, setGradingType] = useState<string | undefined>();

  const handleSearch = () => {
    // 유효성 검사
    if (!condition.academicYear || !condition.semester) {
      toast.error("학년도와 학기를 선택해주세요.");
      return;
    }
    onSearch(condition);
  };

  const handleReset = () => {
    setCondition({ ...DEFAULT_CONDITION });
    setClassificationType(undefined);
    setGradingType(undefined);
    onSearch({ ...DEFAULT_CONDITION });
    toast.success("검색 조건을 초기화했습니다.");
  };

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="space-y-3 pb-4">
        <Button
          type="button"
          onClick={handleSearch}
          disabled={isLoading}
          className="h-12 w-full gap-2 rounded-2xl bg-primary text-sm font-bold text-white shadow-lg shadow-primary/20 hover:bg-primary/90"
        >
          {isLoading ? (
            <span className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
          ) : (
            <Search className="h-4 w-4" />
          )}
          검색 적용하기
        </Button>

        <Button
          type="button"
          variant="outline"
          onClick={handleReset}
          disabled={isLoading}
          className="h-10 w-full gap-2 rounded-xl border-border/40 bg-white/50 text-xs font-medium text-muted-foreground hover:bg-white/80 hover:text-foreground"
        >
          <RotateCcw className="h-3.5 w-3.5" />
          초기화
        </Button>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto pr-1 scrollbar-hide">
        {/* 스마트 필터 섹션 */}
        <FilterSection
          title="스마트 필터"
          icon={<Sparkles className="h-4 w-4 text-violet-500" />}
          open={smartOpen}
          onOpenChange={setSmartOpen}
        >
          <CourseSmartFilters
            condition={condition}
            setCondition={setCondition}
            scheduleOpen={scheduleOpen}
            setScheduleOpen={setScheduleOpen}
          />
        </FilterSection>

        {/* 기본 정보 섹션 */}
        <FilterSection
          title="기본 정보"
          icon={<Filter className="h-4 w-4 text-primary/80" />}
          open={basicOpen}
          onOpenChange={setBasicOpen}
        >
          <CourseBasicFilters condition={condition} setCondition={setCondition} />
        </FilterSection>

        {/* 강의 상세 섹션 */}
        <FilterSection
          title="강의 상세"
          icon={<SlidersHorizontal className="h-4 w-4 text-primary/80" />}
          open={detailOpen}
          onOpenChange={setDetailOpen}
        >
          <CourseDetailFilters
            condition={condition}
            setCondition={setCondition}
            classificationType={classificationType}
            setClassificationType={setClassificationType}
            gradingType={gradingType}
            setGradingType={setGradingType}
          />
        </FilterSection>
      </div>
    </div>
  );
}
