"use client";

import { cn } from "@/shared/lib/utils";
import type { CourseDayOfWeek, ScheduleCondition } from "@/shared/types/api";
import { useCallback, useEffect, useMemo, useState } from "react";

const DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SLOT_NUMBERS = Array.from({ length: 13 }, (_, i) => i);
const GRID_COLUMNS_CLASS = "grid-cols-[56px_repeat(6,minmax(0,1fr))]";

interface DragCell {
  dayIndex: number;
  slot: number;
}

interface TimeTableSelectorProps {
  selected: ScheduleCondition[];
  onChange: (selected: ScheduleCondition[]) => void;
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
 * 슬롯 인덱스를 실제 시간 범위 정보로 변환
 */
function getSlotRange(slot: number): { startTime: string; endTime: string; label: string } {
  const startMinutes = 9 * 60 + slot * 60;
  const endMinutes = startMinutes + 60;

  return {
    startTime: toTimeText(startMinutes),
    endTime: toTimeText(endMinutes),
    label: `${String(Math.floor(startMinutes / 60)).padStart(2, "0")}:00`,
  };
}

/**
 * 요일과 슬롯 정보를 검색 조건 형식으로 변환
 */
function toSchedule(day: CourseDayOfWeek, slot: number): ScheduleCondition {
  const { startTime, endTime } = getSlotRange(slot);
  return { dayOfWeek: day, startTime, endTime };
}

/**
 * 시간대 정보를 식별하기 위한 고유 키 생성
 */
function toScheduleKey(schedule: ScheduleCondition): string {
  return `${schedule.dayOfWeek}-${schedule.startTime}-${schedule.endTime}`;
}

/**
 * 드래그 영역 내의 모든 셀 좌표 계산
 */
function getDragCells(start: DragCell, end: DragCell): DragCell[] {
  const minDay = Math.min(start.dayIndex, end.dayIndex);
  const maxDay = Math.max(start.dayIndex, end.dayIndex);
  const minSlot = Math.min(start.slot, end.slot);
  const maxSlot = Math.max(start.slot, end.slot);
  const cells: DragCell[] = [];

  for (let dayIndex = minDay; dayIndex <= maxDay; dayIndex += 1) {
    for (let slot = minSlot; slot <= maxSlot; slot += 1) {
      cells.push({ dayIndex, slot });
    }
  }

  return cells;
}

/**
 * 선택된 시간대 리스트를 요일/시간 순으로 정렬
 */
function sortSchedules(schedules: ScheduleCondition[]): ScheduleCondition[] {
  return [...schedules].sort((a, b) => {
    const dayOrderDiff = DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek);
    if (dayOrderDiff !== 0) {
      return dayOrderDiff;
    }
    return a.startTime.localeCompare(b.startTime);
  });
}

/**
 * 시간표 형식의 시간대 선택기 컴포넌트 (드래그 지원)
 */
export function TimeTableSelector({ selected, onChange }: TimeTableSelectorProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragMode, setDragMode] = useState<"select" | "deselect" | null>(null);
  const [dragStart, setDragStart] = useState<DragCell | null>(null);
  const [dragCurrent, setDragCurrent] = useState<DragCell | null>(null);
  const [dragSnapshot, setDragSnapshot] = useState<ScheduleCondition[]>([]);
  const [hoveredDay, setHoveredDay] = useState<CourseDayOfWeek | null>(null);
  const [hoveredSlot, setHoveredSlot] = useState<number | null>(null);

  const selectedKeySet = useMemo(
    () => new Set(selected.map((schedule) => toScheduleKey(schedule))),
    [selected],
  );

  const dragSnapshotKeySet = useMemo(
    () => new Set(dragSnapshot.map((schedule) => toScheduleKey(schedule))),
    [dragSnapshot],
  );

  const dragRectSchedules = useMemo(() => {
    if (!dragStart || !dragCurrent) {
      return [];
    }

    return getDragCells(dragStart, dragCurrent).map(({ dayIndex, slot }) =>
      toSchedule(DAYS[dayIndex], slot),
    );
  }, [dragCurrent, dragStart]);

  const dragRectKeySet = useMemo(
    () => new Set(dragRectSchedules.map((schedule) => toScheduleKey(schedule))),
    [dragRectSchedules],
  );

  const displayKeySet = useMemo(() => {
    if (!isDragging || !dragMode) {
      return selectedKeySet;
    }

    const next = new Set(dragSnapshotKeySet);

    dragRectKeySet.forEach((key) => {
      if (dragMode === "select") {
        next.add(key);
      } else {
        next.delete(key);
      }
    });

    return next;
  }, [dragMode, dragRectKeySet, dragSnapshotKeySet, isDragging, selectedKeySet]);

  /**
   * 드래그 시작 핸들러 (선택/해제 모드 결정)
   */
  const startDrag = (day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) {
      return;
    }

    const schedule = toSchedule(day, slot);
    const scheduleKey = toScheduleKey(schedule);
    const mode = selectedKeySet.has(scheduleKey) ? "deselect" : "select";

    setDragSnapshot(selected);
    setIsDragging(true);
    setDragMode(mode);
    setDragStart({ dayIndex, slot });
    setDragCurrent({ dayIndex, slot });
  };

  /**
   * 셀 위에 마우스가 올라갔을 때 드래그 영역 갱신
   */
  const onMouseEnterCell = (day: CourseDayOfWeek, slot: number) => {
    setHoveredDay(day);
    setHoveredSlot(slot);

    if (!isDragging) {
      return;
    }

    const dayIndex = DAYS.indexOf(day);
    if (dayIndex >= 0) {
      setDragCurrent({ dayIndex, slot });
    }
  };

  /**
   * 드래그 종료 시 최종 선택 상태 반영
   */
  const finishDragSelection = useCallback(() => {
    if (!isDragging || !dragMode || !dragStart || !dragCurrent) {
      setIsDragging(false);
      setDragMode(null);
      return;
    }

    const nextMap = new Map(dragSnapshot.map((schedule) => [toScheduleKey(schedule), schedule]));

    dragRectSchedules.forEach((schedule) => {
      const key = toScheduleKey(schedule);
      if (dragMode === "select") {
        nextMap.set(key, schedule);
      } else {
        nextMap.delete(key);
      }
    });

    onChange(sortSchedules(Array.from(nextMap.values())));
    setIsDragging(false);
    setDragMode(null);
    setDragStart(null);
    setDragCurrent(null);
    setDragSnapshot([]);
  }, [dragCurrent, dragMode, dragRectSchedules, dragSnapshot, dragStart, isDragging, onChange]);

  useEffect(() => {
    const handleMouseUp = () => {
      finishDragSelection();
    };

    window.addEventListener("mouseup", handleMouseUp);
    return () => window.removeEventListener("mouseup", handleMouseUp);
  }, [finishDragSelection]);

  /**
   * 특정 요일 전체 선택/해제 핸들러
   */
  const handleSelectAllByDay = (day: CourseDayOfWeek) => {
    const daySlots = SLOT_NUMBERS.map((slot) => toSchedule(day, slot));
    const daySlotKeys = new Set(daySlots.map((schedule) => toScheduleKey(schedule)));
    const selectedByDay = selected.filter((schedule) =>
      daySlotKeys.has(toScheduleKey(schedule)),
    );
    const isFullySelected = selectedByDay.length === daySlots.length;

    const withoutDay = selected.filter((schedule) => !daySlotKeys.has(toScheduleKey(schedule)));
    onChange(sortSchedules(isFullySelected ? withoutDay : [...withoutDay, ...daySlots]));
  };

  const isCellSelected = (day: CourseDayOfWeek, slot: number) => {
    const key = toScheduleKey(toSchedule(day, slot));
    return displayKeySet.has(key);
  };

  const isCellInDragRect = (day: CourseDayOfWeek, slot: number) => {
    if (!isDragging) {
      return false;
    }
    const key = toScheduleKey(toSchedule(day, slot));
    return dragRectKeySet.has(key);
  };

  return (
    <div
      className="w-full max-w-full select-none overflow-hidden rounded-xl border border-white/10 bg-card/20 p-3 backdrop-blur-md"
      onMouseLeave={() => {
        setHoveredDay(null);
        setHoveredSlot(null);
      }}
    >
      <div className="w-full">
        <div className={cn("mb-1 grid gap-1", GRID_COLUMNS_CLASS)}>
          <div className="flex items-center justify-center text-[9px] font-black text-muted-foreground/30">
            시간
          </div>
          {DAYS.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => handleSelectAllByDay(day)}
              onMouseEnter={() => setHoveredDay(day)}
              className={cn(
                "rounded border-t-2 border-transparent py-1 text-center text-[11px] font-bold transition-all",
                hoveredDay === day
                  ? "border-primary bg-primary/10 text-primary"
                  : "text-muted-foreground/60 hover:text-primary",
              )}
            >
              {day}
            </button>
          ))}
        </div>

        <div className="space-y-1">
          {SLOT_NUMBERS.map((slot) => {
            const slotRange = getSlotRange(slot);

            return (
              <div
                key={slot}
                className={cn(
                  "grid gap-1 rounded-lg transition-colors",
                  GRID_COLUMNS_CLASS,
                  slot % 2 === 1 && "bg-white/2",
                  hoveredSlot === slot && "bg-primary/5",
                )}
              >
                <div
                  className={cn(
                    "flex h-10 flex-col items-center justify-center border-r border-white/5 pr-2 transition-colors",
                    hoveredSlot === slot
                      ? "rounded-l-lg bg-primary/10 text-primary"
                      : "text-muted-foreground/40",
                  )}
                >
                  <span
                    className={cn(
                      "mb-0.5 text-[10px] font-black leading-none",
                      hoveredSlot === slot ? "text-primary" : "text-primary/60",
                    )}
                  >
                    {slotRange.label}
                  </span>
                  <span className="text-[8px] font-bold">{slot + 1}교시</span>
                </div>

                {DAYS.map((day) => (
                  <button
                    key={`${day}-${slot}`}
                    type="button"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      startDrag(day, slot);
                    }}
                    onMouseEnter={() => onMouseEnterCell(day, slot)}
                    aria-label={`${day} ${slot + 1}교시`}
                    className={cn(
                      "h-10 rounded-md border border-white/5 transition-all duration-150",
                      hoveredDay === day && "border-primary/20 bg-primary/5",
                      isCellInDragRect(day, slot) && dragMode === "select" && "ring-1 ring-primary/50",
                      isCellInDragRect(day, slot) && dragMode === "deselect" && "bg-destructive/20",
                      isCellSelected(day, slot)
                        ? "border-primary/50 bg-primary text-primary-foreground shadow-[0_0_8px_rgba(var(--primary),0.3)]"
                        : "bg-background/10 hover:bg-primary/20",
                    )}
                  />
                ))}
              </div>
            );
          })}
        </div>
      </div>

      <div className="mt-4 flex items-center justify-between border-t border-white/5 px-1 pt-3">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <div className="h-1.5 w-1.5 rounded-full bg-primary shadow-[0_0_5px_rgba(var(--primary),0.5)]" />
            <span className="text-[9px] font-bold tracking-tighter text-muted-foreground/60">선택됨</span>
          </div>
          <p className="text-[9px] italic text-muted-foreground/40">* 사각형 드래그로 선택/해제</p>
        </div>
        <button
          type="button"
          onClick={() => onChange([])}
          className="text-[9px] font-black tracking-widest text-primary/60 transition-all hover:text-primary hover:underline active:scale-95"
        >
          전체 해제
        </button>
      </div>
    </div>
  );
}
