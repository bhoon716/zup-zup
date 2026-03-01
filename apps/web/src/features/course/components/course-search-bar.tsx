"use client";

import { useState, useEffect, useRef } from "react";
import { Button } from "@/shared/ui/button";
import { Filter, RotateCcw, Search, SlidersHorizontal, Sparkles } from "lucide-react";
import { toast } from "sonner";
import { cn } from "@/shared/lib/utils";
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
  hideHeader?: boolean;
}

export function CourseSearchBar({
  onSearch,
  isLoading,
  initialCondition,
  hideHeader,
}: CourseSearchBarProps) {
  const [condition, setCondition] = useState<CourseSearchCondition>(
    () => ({ ...(initialCondition ?? DEFAULT_CONDITION) }),
  );

  /**
   * 외부에서 전달된 초기 검색 조건이 변경될 경우(예: 필터 칩 삭제)
   * 현재 입력 중인 상태와 동기화합니다.
   */
  const lastInitialConditionRef = useRef<string>(JSON.stringify(initialCondition));

  /**
   * 외부에서 전달된 초기 검색 조건이 실질적으로 변경될 경우(예: 필터 칩 삭제)
   * 현재 입력 중인 로컬 상태와 동기화합니다.
   * 로컬 변경 사항이 외부 상태에 의해 덮어씌워지지 않도록 변경된 경우에만 동기화합니다.
   */
  useEffect(() => {
    if (!initialCondition) return;
    
    const serializedInitial = JSON.stringify(initialCondition);
    if (serializedInitial !== lastInitialConditionRef.current) {
      setCondition({ ...initialCondition });
      lastInitialConditionRef.current = serializedInitial;
    }
  }, [initialCondition]);
  
  // UI 상태: 접기/펼치기
  const [smartOpen, setSmartOpen] = useState(true);
  const [basicOpen, setBasicOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(true);
  const [scheduleOpen, setScheduleOpen] = useState(false);

  // UI 상태: 계층형 필터
  const [classificationType, setClassificationType] = useState<string | undefined>();
  const [gradingType, setGradingType] = useState<string | undefined>();

  /**
   * 검색 버튼 클릭 시 호출되며, 선택된 검색 조건을 부모 컴포넌트로 전달합니다.
   * 학년도와 학기 선택 여부를 검증합니다.
   */
  const handleSearch = () => {
    if (!condition.academicYear || !condition.semester) {
      toast.error("학년도와 학기를 선택해주세요.");
      return;
    }
    onSearch(condition);
  };

  /**
   * 모든 검색 조건을 기본값으로 초기화하고 검색을 수행합니다.
   * 초기화 성공 시 토스트 메시지를 표시합니다.
   */
  const handleReset = () => {
    setCondition({ ...DEFAULT_CONDITION });
    setClassificationType(undefined);
    setGradingType(undefined);
    onSearch({ ...DEFAULT_CONDITION });
    toast.success("검색 조건을 초기화했습니다.");
  };

  return (
    <div className={cn("flex min-h-0 flex-col", !hideHeader && "h-full")}>
      {!hideHeader && (
        <div className="space-y-3 pb-4">
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="강의명 또는 코드"
                value={condition.name || ""}
                onChange={(e) => setCondition((prev) => ({ ...prev, name: e.target.value }))}
                onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                className="h-12 w-full rounded-2xl border-none bg-muted/50 pl-10 pr-4 text-sm font-bold placeholder:text-muted-foreground/60 focus:bg-white focus:ring-2 focus:ring-primary/20"
              />
              <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            </div>
            <Button
              type="button"
              onClick={handleSearch}
              disabled={isLoading}
              className="h-12 rounded-2xl bg-primary px-5 font-bold text-white shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all active:scale-95"
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
            className="h-10 w-full gap-2 rounded-xl border-border/40 bg-white/50 text-xs font-medium text-muted-foreground hover:bg-white/80 hover:text-foreground"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            검색 조건 초기화
          </Button>
        </div>
      )}

      <div className={cn("space-y-3 pr-1 scrollbar-hide", !hideHeader ? "flex-1 overflow-y-auto" : "h-auto overflow-visible")}>
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
