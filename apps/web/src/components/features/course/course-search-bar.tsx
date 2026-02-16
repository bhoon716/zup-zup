"use client";

import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { Switch } from "@/components/ui/switch";
import { TimeTableSelector } from "./time-table-selector";
import { useUser } from "@/hooks/useUser";
import {
  ChevronDown,
  Filter,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Sparkles,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import type {
  CourseClassification,
  CourseSearchCondition,
  GradingMethod,
  LectureLanguage,
  ScheduleCondition,
} from "@/types/api";

interface CourseSearchBarProps {
  onSearch: (condition: CourseSearchCondition) => void;
  isLoading?: boolean;
  initialCondition?: CourseSearchCondition;
}

const DEFAULT_CONDITION: CourseSearchCondition = {
  academicYear: "2026",
  semester: "U211600010",
};

const CLASSIFICATIONS: CourseClassification[] = [
  "계열공통",
  "교양",
  "교직(대)",
  "교직",
  "군사학",
  "기초필수",
  "선수",
  "일반선택",
  "전공",
  "전공선택",
  "전공필수",
];

const GRADING_METHODS: GradingMethod[] = [
  "Pass/Fail",
  "기타(법전원)",
  "상대평가Ⅰ",
  "상대평가Ⅱ",
  "상대평가Ⅲ",
  "절대평가",
];

const LANGUAGES: LectureLanguage[] = [
  "한국어",
  "영어",
  "독일어",
  "스페인어",
  "일본어",
  "중국어",
  "프랑스어",
];

const CREDITS = ["0.5", "1", "2", "3", "4"];
const LECTURE_HOURS = [...Array.from({ length: 9 }, (_, i) => `${i + 1}`), "10+"];

const YEARS = ["2026", "2025", "2024"];

const SEMESTERS = [
  { label: "1학기", value: "U211600010" },
  { label: "계절학기(하기)", value: "U010200021" },
  { label: "2학기", value: "U211600020" },
  { label: "계절학기(동기)", value: "U010200022" },
  { label: "특별학기(여름)", value: "S1" },
  { label: "특별학기(겨울)", value: "S2" },
  { label: "특별학기(신입생)", value: "S3" },
  { label: "특별학기(SW)", value: "S4" },
];

const GE_CATEGORIES: Record<string, string[]> = {
  기초: ["공통기초", "이공계기초"],
  일반: ["경력개발", "사회과학", "수리/정보", "언어/문학/문화", "역사/철학", "예술/체육", "자연과학"],
  핵심: ["과학적사고의기반", "사회이해의기반", "인문적사고의기반"],
};

function sanitizeCondition(condition: CourseSearchCondition): CourseSearchCondition {
  const sanitized: CourseSearchCondition = {};

  Object.entries(condition).forEach(([key, value]) => {
    if (value === undefined || value === null) {
      return;
    }

    if (typeof value === "string" && value.trim() === "") {
      return;
    }

    if (Array.isArray(value) && value.length === 0) {
      return;
    }

    (sanitized as Record<string, unknown>)[key] = value;
  });

  return sanitized;
}

interface FilterSectionProps {
  title: string;
  icon: ReactNode;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: ReactNode;
}

function FilterSection({ title, icon, open, onOpenChange, children }: FilterSectionProps) {
  return (
    <Collapsible open={open} onOpenChange={onOpenChange}>
      <div className="overflow-hidden rounded-2xl border border-border bg-white shadow-[0_6px_20px_rgba(15,23,42,0.03)]">
        <CollapsibleTrigger asChild>
          <button
            type="button"
            className="flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-muted/60"
          >
            <span className="flex items-center gap-2 text-sm font-bold text-foreground">
              {icon}
              {title}
            </span>
            <ChevronDown
              className={cn(
                "h-4 w-4 text-muted-foreground transition-transform duration-200",
                open && "rotate-180",
              )}
            />
          </button>
        </CollapsibleTrigger>
        <CollapsibleContent className="border-t border-border/70 px-4 pb-4 pt-3 data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up">
          {children}
        </CollapsibleContent>
      </div>
    </Collapsible>
  );
}

export function CourseSearchBar({
  onSearch,
  isLoading,
  initialCondition,
}: CourseSearchBarProps) {
  const [condition, setCondition] = useState<CourseSearchCondition>(
    () => ({ ...(initialCondition ?? DEFAULT_CONDITION) }),
  );
  const [smartOpen, setSmartOpen] = useState(true);
  const [basicOpen, setBasicOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(true);

  const { data: user } = useUser();

  const availableDetails = useMemo(() => {
    if (!condition.generalCategory) {
      return [];
    }
    return GE_CATEGORIES[condition.generalCategory] || [];
  }, [condition.generalCategory]);

  const handleSearch = () => {
    onSearch(sanitizeCondition(condition));
  };

  const handleReset = () => {
    setCondition({ ...DEFAULT_CONDITION });
    onSearch({ ...DEFAULT_CONDITION });
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    handleSearch();
  };

  const handleWishedOnlyChange = (checked: boolean) => {
    if (checked && !user) {
      toast.error("찜한 강의 필터는 로그인 후 사용할 수 있습니다.");
      return;
    }

    setCondition((prev) => ({ ...prev, isWishedOnly: checked || undefined }));
  };

  const handleSchedulesChange = (selected: ScheduleCondition[]) => {
    setCondition((prev) => ({
      ...prev,
      selectedSchedules: selected.length > 0 ? selected : undefined,
    }));
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

        <div className="flex items-center justify-between px-1">
          <h2 className="text-lg font-black tracking-tight text-foreground">상세 필터</h2>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="h-8 gap-1 rounded-lg px-2 text-xs font-bold text-muted-foreground hover:text-primary"
            onClick={handleReset}
          >
            <RotateCcw className="h-3.5 w-3.5" />
            초기화
          </Button>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="flex min-h-0 flex-1 flex-col">
        <div className="space-y-3 overflow-y-auto pr-1">
          <FilterSection
            title="스마트 필터"
            icon={<Sparkles className="h-4 w-4 text-primary/80" />}
            open={smartOpen}
            onOpenChange={setSmartOpen}
          >
            <div className="space-y-4">
              <div className="rounded-xl border border-border bg-muted/30 p-3">
                <div className="mb-2 flex items-center justify-between">
                  <p className="text-xs font-bold text-muted-foreground">가능한 시간대 선택</p>
                  <span className="text-[10px] font-semibold text-primary/70">
                    {condition.selectedSchedules?.length ?? 0}칸 선택
                  </span>
                </div>
                <TimeTableSelector
                  selected={condition.selectedSchedules || []}
                  onChange={handleSchedulesChange}
                />
              </div>

              <div className="space-y-3 rounded-xl border border-border bg-white p-3">
                <div className="flex items-center justify-between">
                  <Label htmlFor="isAvailableOnly" className="text-sm font-medium text-foreground">
                    여석 있는 강의만
                  </Label>
                  <Switch
                    id="isAvailableOnly"
                    checked={condition.isAvailableOnly ?? false}
                    onCheckedChange={(checked) =>
                      setCondition((prev) => ({
                        ...prev,
                        isAvailableOnly: checked || undefined,
                      }))
                    }
                  />
                </div>

                <div className="flex items-center justify-between">
                  <Label htmlFor="isWishedOnly" className="text-sm font-medium text-foreground">
                    관심 강의만 보기
                  </Label>
                  <Switch
                    id="isWishedOnly"
                    checked={condition.isWishedOnly ?? false}
                    onCheckedChange={handleWishedOnlyChange}
                  />
                </div>
              </div>
            </div>
          </FilterSection>

          <FilterSection
            title="기본 정보"
            icon={<Filter className="h-4 w-4 text-primary/80" />}
            open={basicOpen}
            onOpenChange={setBasicOpen}
          >
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">년도</Label>
                  <Select
                    value={condition.academicYear || "2026"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({ ...prev, academicYear: value }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="년도" />
                    </SelectTrigger>
                    <SelectContent>
                      {YEARS.map((year) => (
                        <SelectItem key={year} value={year}>
                          {year}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">학기</Label>
                  <Select
                    value={condition.semester || "U211600010"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({ ...prev, semester: value }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="학기" />
                    </SelectTrigger>
                    <SelectContent>
                      {SEMESTERS.map((semester) => (
                        <SelectItem key={semester.value} value={semester.value}>
                          {semester.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label className="text-[11px] font-bold text-muted-foreground">강의명 / 강의 코드</Label>
                <Input
                  value={condition.name || ""}
                  onChange={(e) => setCondition((prev) => ({ ...prev, name: e.target.value }))}
                  placeholder="예: 자료구조, CLTR.0031"
                  className="h-10 rounded-xl bg-muted/30"
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-[11px] font-bold text-muted-foreground">교수명</Label>
                <Input
                  value={condition.professor || ""}
                  onChange={(e) =>
                    setCondition((prev) => ({ ...prev, professor: e.target.value }))
                  }
                  placeholder="교수님 성함"
                  className="h-10 rounded-xl bg-muted/30"
                />
              </div>

              <div className="space-y-1.5">
                <Label className="text-[11px] font-bold text-muted-foreground">학과 검색</Label>
                <Input
                  value={condition.department || ""}
                  onChange={(e) =>
                    setCondition((prev) => ({ ...prev, department: e.target.value }))
                  }
                  placeholder="예: 컴퓨터공학부, 경영학과"
                  className="h-10 rounded-xl bg-muted/30"
                />
              </div>
            </div>
          </FilterSection>

          <FilterSection
            title="강의 상세"
            icon={<SlidersHorizontal className="h-4 w-4 text-primary/80" />}
            open={detailOpen}
            onOpenChange={setDetailOpen}
          >
            <div className="space-y-4">
              <div className="space-y-1.5">
                <Label className="text-[11px] font-bold text-muted-foreground">이수 구분</Label>
                <Select
                  value={condition.classification || "all"}
                  onValueChange={(value) =>
                    setCondition((prev) => ({
                      ...prev,
                      classification:
                        value === "all" ? undefined : (value as CourseClassification),
                      generalCategory: value === "교양" ? prev.generalCategory : undefined,
                      generalDetail: value === "교양" ? prev.generalDetail : undefined,
                    }))
                  }
                >
                  <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                    <SelectValue placeholder="전체" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체</SelectItem>
                    {CLASSIFICATIONS.map((classification) => (
                      <SelectItem key={classification} value={classification}>
                        {classification}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {condition.classification === "교양" && (
                <div className="space-y-3 rounded-xl border border-primary/20 bg-primary/5 p-3">
                  <div className="space-y-1.5">
                    <Label className="text-[11px] font-bold text-primary/80">교양 영역</Label>
                    <Select
                      value={condition.generalCategory || "all"}
                      onValueChange={(value) =>
                        setCondition((prev) => ({
                          ...prev,
                          generalCategory: value === "all" ? undefined : value,
                          generalDetail: undefined,
                        }))
                      }
                    >
                      <SelectTrigger className="h-10 rounded-xl bg-white/80 text-sm">
                        <SelectValue placeholder="전체" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">전체</SelectItem>
                        {Object.keys(GE_CATEGORIES).map((category) => (
                          <SelectItem key={category} value={category}>
                            {category}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-1.5">
                    <Label className="text-[11px] font-bold text-primary/80">상세 영역</Label>
                    <Select
                      value={condition.generalDetail || "all"}
                      disabled={!condition.generalCategory}
                      onValueChange={(value) =>
                        setCondition((prev) => ({
                          ...prev,
                          generalDetail: value === "all" ? undefined : value,
                        }))
                      }
                    >
                      <SelectTrigger className="h-10 rounded-xl bg-white/80 text-sm">
                        <SelectValue
                          placeholder={
                            condition.generalCategory ? "상세 영역 선택" : "교양 영역을 먼저 선택하세요"
                          }
                        />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="all">전체</SelectItem>
                        {availableDetails.map((detail) => (
                          <SelectItem key={detail} value={detail}>
                            {detail}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">강의 언어</Label>
                  <Select
                    value={condition.lectureLanguage || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        lectureLanguage:
                          value === "all" ? undefined : (value as LectureLanguage),
                      }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {LANGUAGES.map((language) => (
                        <SelectItem key={language} value={language}>
                          {language}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">성적 평가</Label>
                  <Select
                    value={condition.gradingMethod || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        gradingMethod: value === "all" ? undefined : (value as GradingMethod),
                      }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {GRADING_METHODS.map((grading) => (
                        <SelectItem key={grading} value={grading}>
                          {grading}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">학점</Label>
                  <Select
                    value={condition.credits || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        credits: value === "all" ? undefined : value,
                      }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {CREDITS.map((credit) => (
                        <SelectItem key={credit} value={credit}>
                          {credit}학점
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">시수</Label>
                  <Select
                    value={
                      condition.minLectureHours
                        ? "10+"
                        : condition.lectureHours
                          ? `${condition.lectureHours}`
                          : "all"
                    }
                    onValueChange={(value) => {
                      if (value === "all") {
                        setCondition((prev) => ({
                          ...prev,
                          lectureHours: undefined,
                          minLectureHours: undefined,
                        }));
                        return;
                      }

                      if (value === "10+") {
                        setCondition((prev) => ({
                          ...prev,
                          lectureHours: undefined,
                          minLectureHours: 10,
                        }));
                        return;
                      }

                      setCondition((prev) => ({
                        ...prev,
                        lectureHours: Number(value),
                        minLectureHours: undefined,
                      }));
                    }}
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {LECTURE_HOURS.map((hour) => (
                        <SelectItem key={hour} value={hour}>
                          {hour === "10+" ? "10시간 이상" : `${hour}시간`}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-1.5">
                <Label className="text-[11px] font-bold text-muted-foreground">과목 코드</Label>
                <Input
                  value={condition.subjectCode || ""}
                  onChange={(e) =>
                    setCondition((prev) => ({ ...prev, subjectCode: e.target.value }))
                  }
                  placeholder="예: CLTR.0031"
                  className="h-10 rounded-xl bg-muted/30"
                />
              </div>
            </div>
          </FilterSection>
        </div>

        <div className="mt-4 border-t border-border/70 pt-3">
          <Button
            type="submit"
            disabled={isLoading}
            className="h-11 w-full gap-2 rounded-xl bg-primary text-sm font-bold text-white hover:bg-primary/90"
          >
            <Search className="h-4 w-4" />
            필터로 검색하기
          </Button>
        </div>
      </form>
    </div>
  );
}
