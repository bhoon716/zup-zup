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
import { usePrimaryTimetable } from "@/features/timetable/hooks/useTimetable";
import { buildFreeSchedulesFromTimetable } from "../../lib/course-utils";
import type { CourseSearchCondition, ScheduleCondition } from "@/shared/types/api";

interface CourseSmartFiltersProps {
  condition: CourseSearchCondition;
  setCondition: React.Dispatch<React.SetStateAction<CourseSearchCondition>>;
  scheduleOpen: boolean;
  setScheduleOpen: (open: boolean) => void;
}

export function CourseSmartFilters({
  condition,
  setCondition,
  scheduleOpen,
  setScheduleOpen,
}: CourseSmartFiltersProps) {
  const { data: user } = useUser();
  const { data: primaryTimetable, refetch: refetchPrimaryTimetable } = usePrimaryTimetable();

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
            <Button
              variant="ghost"
              size="icon"
              className="h-8 w-8"
              onClick={(e) => {
                e.stopPropagation();
                handleImportFromMyTimetable();
              }}
              title="내 시간표에서 공강 불러오기"
            >
              <CalendarPlus className="h-4 w-4 text-muted-foreground transition-colors hover:text-primary" />
            </Button>
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
