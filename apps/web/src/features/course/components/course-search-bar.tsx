"use client";

import { useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/shared/ui/collapsible";
import { Switch } from "@/shared/ui/switch";
import { TimeTableSelector } from "./time-table-selector";
import { useUser } from "@/features/user/hooks/useUser";
import { usePrimaryTimetable } from "@/features/timetable/hooks/useTimetable";
import {
  ChevronDown,
  Download,
  Filter,
  RotateCcw,
  Search,
  SlidersHorizontal,
  Sparkles,
} from "lucide-react";
import { cn } from "@/shared/lib/utils";
import { toast } from "sonner";
import type {
  CourseDayOfWeek,
  CourseClassification,
  CourseSearchCondition,
  GradingMethod,
  LectureLanguage,
  ScheduleCondition,
  TimetableDetailResponse,
} from "@/shared/types/api";

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

const CREDITS = ["0.5", "1", "2", "3", "4+"];
const TARGET_GRADES = ["1", "2", "3", "4", "5", "6", "없음"];
const DISCLOSURES = ["공개", "비공개"];
const COURSE_DIRECTIONS = [
  "일반",
  "원격강좌(콘텐츠)",
  "원격강좌(실시간)",
  "플립러닝",
  "블렌디드러닝",
  "온·오프라인강좌",
  "현장실습",
  "사회봉사",
  "논문연구",
  "화상강의",
  "특별(영어)",
];

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

const SMART_FILTER_DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SMART_FILTER_START_MINUTES = 9 * 60;
const SMART_FILTER_SLOT_MINUTES = 60;
const SMART_FILTER_SLOT_COUNT = 13;

/**
 * "HH:mm" 또는 "HH:mm:ss" 형식의 문자열을 분(minutes) 단위 숫자로 변환
 */
function toMinutes(value?: string): number | null {
  if (!value) {
    return null;
  }

  const [hourText, minuteText] = value.split(":");
  const hour = Number(hourText);
  const minute = Number(minuteText);
  if (Number.isNaN(hour) || Number.isNaN(minute)) {
    return null;
  }

  return hour * 60 + minute;
}

/**
 * 다양한 형식의 요일 문자열을 내부 공통 형식('월', '화' 등)으로 변환
 */
function toDayLabel(dayOfWeek?: string): CourseDayOfWeek | null {
  const dayMap: Record<string, CourseDayOfWeek> = {
    MONDAY: "월",
    TUESDAY: "화",
    WEDNESDAY: "수",
    THURSDAY: "목",
    FRIDAY: "금",
    SATURDAY: "토",
    SUNDAY: "일",
    MO: "월",
    TU: "화",
    WE: "수",
    TH: "목",
    FR: "금",
    SA: "토",
    SU: "일",
    월요일: "월",
    화요일: "화",
    수요일: "수",
    목요일: "목",
    금요일: "금",
    토요일: "토",
    일요일: "일",
    월: "월",
    화: "화",
    수: "수",
    목: "목",
    금: "금",
    토: "토",
    일: "일",
  };

  if (!dayOfWeek) {
    return null;
  }

  return dayMap[dayOfWeek] ?? null;
}

/**
 * 분 단위 숫자를 "HH:mm:00" 형식의 시간 문자열로 변환
 */
function toTimeText(minutes: number): string {
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00`;
}

/**
 * 기존 시간표 정보를 분석하여 비어있는 시간대(공강) 목록을 생성
 */
function buildFreeSchedulesFromTimetable(
  timetable: TimetableDetailResponse,
): ScheduleCondition[] {
  const schedules = [
    ...(timetable?.courses?.flatMap((course) => course.schedules ?? []) ?? []),
    ...(timetable?.customSchedules ?? []),
  ];

  const allSlots: ScheduleCondition[] = [];
  const occupiedSlotKeys = new Set<string>();

  SMART_FILTER_DAYS.forEach((day) => {
    for (let slot = 0; slot < SMART_FILTER_SLOT_COUNT; slot += 1) {
      const slotStart = SMART_FILTER_START_MINUTES + slot * SMART_FILTER_SLOT_MINUTES;
      const slotEnd = slotStart + SMART_FILTER_SLOT_MINUTES;

      allSlots.push({
        dayOfWeek: day,
        startTime: toTimeText(slotStart),
        endTime: toTimeText(slotEnd),
      });
    }
  });

  schedules.forEach((schedule) => {
    const day = toDayLabel(schedule.dayOfWeek);
    if (!day || !SMART_FILTER_DAYS.includes(day)) {
      return;
    }

    const startMinutes = toMinutes(schedule.startTime);
    const endMinutes = toMinutes(schedule.endTime);
    if (startMinutes === null || endMinutes === null || endMinutes <= startMinutes) {
      return;
    }

    for (let slot = 0; slot < SMART_FILTER_SLOT_COUNT; slot += 1) {
      const slotStart = SMART_FILTER_START_MINUTES + slot * SMART_FILTER_SLOT_MINUTES;
      const slotEnd = slotStart + SMART_FILTER_SLOT_MINUTES;
      const overlaps = startMinutes < slotEnd && slotStart < endMinutes;

      if (!overlaps) {
        continue;
      }

      const slotSchedule: ScheduleCondition = {
        dayOfWeek: day,
        startTime: toTimeText(slotStart),
        endTime: toTimeText(slotEnd),
      };
      const key = `${slotSchedule.dayOfWeek}-${slotSchedule.startTime}-${slotSchedule.endTime}`;
      occupiedSlotKeys.add(key);
    }
  });

  return allSlots.filter((slot) => !occupiedSlotKeys.has(toScheduleKey(slot)));
}

/**
 * 시간대 정보를 식별하기 위한 고유 키 생성
 */
function toScheduleKey(schedule: ScheduleCondition): string {
  return `${schedule.dayOfWeek}-${schedule.startTime}-${schedule.endTime}`;
}

/**
 * 검색 조건 객체에서 유효하지 않은 값(undefined, 빈 문자열 등)을 제거하여 정제
 */
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

/**
 * 상세 필터의 각 섹션 레이아웃 컴포넌트
 */
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

/**
 * 강의 검색바 컴포넌트 (스마트 필터, 기본 정보, 상세 필터 포함)
 */
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
  const [scheduleOpen, setScheduleOpen] = useState(false);

  const { data: user } = useUser();
  const {
    data: primaryTimetable,
    isFetching: isFetchingTimetable,
    refetch: refetchPrimaryTimetable,
  } = usePrimaryTimetable();

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

  const handleImportFromMyTimetable = async () => {
    if (!user) {
      toast.error("내 시간표에서 선택하기는 로그인 후 사용할 수 있습니다.");
      return;
    }

    const fetched = await refetchPrimaryTimetable();
    const timetable = fetched.data ?? primaryTimetable;

    if (!timetable) {
      toast.error("대표 시간표가 없습니다. 시간표를 먼저 만들어주세요.");
      return;
    }

    const importedSchedules = buildFreeSchedulesFromTimetable(timetable);
    if (importedSchedules.length === 0) {
      toast.error("시간표 기준 공강 시간대가 없습니다.");
      return;
    }

    setCondition((prev) => ({
      ...prev,
      selectedSchedules: importedSchedules,
    }));
    setScheduleOpen(true);
    toast.success(`내 시간표 기준 공강 ${importedSchedules.length}칸을 선택했습니다.`);
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
        <div className="space-y-3 overflow-y-auto pr-1 [scrollbar-gutter:stable_both-edges]">
          <FilterSection
            title="스마트 필터"
            icon={<Sparkles className="h-4 w-4 text-primary/80" />}
            open={smartOpen}
            onOpenChange={setSmartOpen}
          >
            <div className="space-y-4">
              <Collapsible open={scheduleOpen} onOpenChange={setScheduleOpen}>
                <div className="rounded-xl border border-border bg-muted/30 p-3">
                  <div className="mb-2 flex items-center justify-between">
                    <p className="text-xs font-bold text-muted-foreground">가능한 시간대 선택</p>
                    <span className="text-[10px] font-semibold text-primary/70">
                      {condition.selectedSchedules?.length ?? 0}칸 선택
                    </span>
                  </div>

                  <div className="mb-2 flex items-center gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      className="h-8 rounded-lg border-border/80 bg-white text-xs font-semibold"
                      disabled={isFetchingTimetable}
                      onClick={handleImportFromMyTimetable}
                    >
                      {isFetchingTimetable ? (
                        <span className="h-3.5 w-3.5 rounded-full border-2 border-primary/20 border-t-primary animate-spin" />
                      ) : (
                        <Download className="h-3.5 w-3.5" />
                      )}
                      내 시간표에서 선택
                    </Button>

                    <CollapsibleTrigger asChild>
                      <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        className="ml-auto h-8 gap-1 rounded-lg px-2 text-xs font-semibold text-muted-foreground"
                      >
                        {scheduleOpen ? "접기" : "펼치기"}
                        <ChevronDown
                          className={cn("h-3.5 w-3.5 transition-transform", scheduleOpen && "rotate-180")}
                        />
                      </Button>
                    </CollapsibleTrigger>
                  </div>

                  <CollapsibleContent className="data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up">
                    <TimeTableSelector
                      selected={condition.selectedSchedules || []}
                      onChange={handleSchedulesChange}
                    />
                  </CollapsibleContent>
                </div>
              </Collapsible>

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
                    value={condition.minCredits ? "4+" : condition.credits || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        credits: value === "all" || value === "4+" ? undefined : value,
                        minCredits: value === "4+" ? 4 : undefined,
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
                  <Label className="text-[11px] font-bold text-muted-foreground">강의 방식</Label>
                  <Select
                    value={condition.status || "all"}
                    onValueChange={(value) => {
                      setCondition((prev) => ({
                        ...prev,
                        status: value === "all" ? undefined : value,
                      }));
                    }}
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {COURSE_DIRECTIONS.map((direction) => (
                        <SelectItem key={direction} value={direction}>
                          {direction}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">대상 학년</Label>
                  <Select
                    value={condition.targetGrade || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        targetGrade: value === "all" ? undefined : value,
                      }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {TARGET_GRADES.map((grade) => (
                        <SelectItem key={grade} value={grade}>
                          {grade === "없음" ? grade : `${grade}학년`}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-1.5">
                  <Label className="text-[11px] font-bold text-muted-foreground">공개 여부</Label>
                  <Select
                    value={condition.disclosure || "all"}
                    onValueChange={(value) =>
                      setCondition((prev) => ({
                        ...prev,
                        disclosure: value === "all" ? undefined : value,
                      }))
                    }
                  >
                    <SelectTrigger className="h-10 rounded-xl bg-muted/30 text-sm">
                      <SelectValue placeholder="전체" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">전체</SelectItem>
                      {DISCLOSURES.map((status) => (
                        <SelectItem key={status} value={status}>
                          {status}
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

      </form>
    </div>
  );
}
