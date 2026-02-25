"use client";

import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/shared/ui/collapsible";
import { CalendarPlus, ChevronDown } from "lucide-react";
import { toast } from "sonner";
import { TimeTableSelector } from "../time-table-selector";
import { useUser } from "@/features/user/hooks/useUser";
import { useTimetables } from "@/features/timetable/hooks/useTimetable";
import { timetableApi } from "@/features/timetable/api/timetable.api";
import { buildFreeSchedulesFromTimetable } from "../../lib/course-utils";
import type { CourseSearchCondition, ScheduleCondition } from "@/shared/types/api";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";

interface CourseSmartFiltersProps {
  condition: CourseSearchCondition;
  setCondition: React.Dispatch<React.SetStateAction<CourseSearchCondition>>;
  scheduleOpen: boolean;
  setScheduleOpen: (open: boolean) => void;
}

/**
 * 강의 검색 결과에 적용할 스마트 필터 구성을 제공하는 컴포넌트입니다.
 * 찜한 강의, 여석 유무, 공강 시간대 등의 추가 필터링 조건을 관리합니다.
 */
export function CourseSmartFilters({
  condition,
  setCondition,
  scheduleOpen,
  setScheduleOpen,
}: CourseSmartFiltersProps) {
  const { data: user } = useUser();
  const { data: timetables, refetch: refetchTimetables } = useTimetables();

  // 찜한 강의만 보기 토글 핸들러
  const handleWishedOnlyChange = (checked: boolean) => {
    if (checked && !user) {
      toast.error("찜한 강의 필터는 로그인 후 사용할 수 있습니다.");
      return;
    }
    setCondition((prev) => ({ ...prev, isWishedOnly: checked || undefined }));
  };

  const handleAvailableOnlyChange = (checked: boolean) => {
    setCondition((prev) => ({ ...prev, isAvailableOnly: checked || undefined }));
  };

  const handleSchedulesChange = (selected: ScheduleCondition[]) => {
    setCondition((prev) => ({
      ...prev,
      selectedSchedules: selected.length > 0 ? selected : undefined,
    }));
  };

  /**
   * 내 시간표 정보를 가져와서 비어있는 시간대(공강)를 자동으로 선택합니다.
   */
  const handleImportFromTimetable = async (timetableId: number, name: string) => {
    try {
      const { data: detail } = await timetableApi.getTimetable(timetableId);

      if (!detail) {
        toast.error(`'${name}' 정보를 불러올 수 없습니다.`);
        return;
      }

      const importedSchedules = buildFreeSchedulesFromTimetable(detail);
      if (importedSchedules.length === 0) {
        toast.error(`'${name}' 기준 공강 시간대가 없습니다.`);
        return;
      }

      setCondition((prev) => ({
        ...prev,
        selectedSchedules: importedSchedules,
      }));
      setScheduleOpen(true);
      toast.success(`'${name}' 기준 공강 ${importedSchedules.length}칸을 선택했습니다.`);
    } catch {
      toast.error("시간표 정보를 가져오는데 실패했습니다.");
    }
  };

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        {/* 찜한 강의만 보기 */}
        <div className="flex items-center justify-between">
          <Label htmlFor="wished-only" className="text-sm font-medium">
            찜한 강의만 보기
          </Label>
          <Switch
            id="wished-only"
            checked={!!condition.isWishedOnly}
            onCheckedChange={handleWishedOnlyChange}
          />
        </div>

        {/* 여석 있는 강의만 보기 */}
        <div className="flex items-center justify-between">
          <Label htmlFor="available-only" className="text-sm font-medium">
            여석 있는 강의만 보기
          </Label>
          <Switch
            id="available-only"
            checked={!!condition.isAvailableOnly}
            onCheckedChange={handleAvailableOnlyChange}
          />
        </div>
      </div>

      {/* 공강 시간표 설정 */}
      <Collapsible
        open={scheduleOpen}
        onOpenChange={setScheduleOpen}
        className="space-y-2"
      >
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Label className="text-sm font-medium">공강 시간표 설정</Label>
            {condition.selectedSchedules &&
              condition.selectedSchedules.length > 0 && (
                <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-bold text-primary">
                  {condition.selectedSchedules.length}개 선택됨
                </span>
              )}
          </div>
          <div className="flex items-center gap-1">
            <DropdownMenu onOpenChange={(open) => open && refetchTimetables()}>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (!user) {
                      toast.error("내 시간표에서 선택하기는 로그인 후 사용할 수 있습니다.");
                    }
                  }}
                  title="내 시간표에서 공강 불러오기"
                >
                  <CalendarPlus className="h-4 w-4 text-muted-foreground transition-colors hover:text-primary" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuLabel className="text-xs font-semibold">내 시간표 선택</DropdownMenuLabel>
                <DropdownMenuSeparator />
                {!user ? (
                  <div className="px-2 py-3 text-center text-xs text-muted-foreground">
                    로그인이 필요합니다.
                  </div>
                ) : !timetables || timetables.length === 0 ? (
                  <div className="px-2 py-3 text-center text-xs text-muted-foreground">
                    생성된 시간표가 없습니다.
                  </div>
                ) : (
                  timetables.map((timetable) => (
                    <DropdownMenuItem
                      key={timetable.id}
                      onClick={() => handleImportFromTimetable(timetable.id, timetable.name)}
                      className="flex cursor-pointer items-center justify-between py-2"
                    >
                      <span className="truncate text-xs font-medium">{timetable.name}</span>
                      {timetable.primary && (
                        <span className="ml-2 shrink-0 rounded-full bg-primary/10 px-1.5 py-0.5 text-[9px] font-bold text-primary">
                          대표
                        </span>
                      )}
                    </DropdownMenuItem>
                  ))
                )}
              </DropdownMenuContent>
            </DropdownMenu>
            <CollapsibleTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <ChevronDown
                  className={`h-4 w-4 text-muted-foreground transition-transform duration-200 ${
                    scheduleOpen ? "rotate-180" : ""
                  }`}
                />
                <span className="sr-only">Toggle</span>
              </Button>
            </CollapsibleTrigger>
          </div>
        </div>
        <CollapsibleContent>
          <TimeTableSelector
            selected={condition.selectedSchedules ?? []}
            onChange={handleSchedulesChange}
          />
        </CollapsibleContent>
      </Collapsible>
    </div>
  );
}
