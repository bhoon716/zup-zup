"use client";

import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import type { CourseDayOfWeek, ScheduleCondition } from "@/shared/types/api";
import { useCallback, useEffect, useMemo, useState } from "react";

const DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SLOT_NUMBERS = Array.from({ length: 13 }, (_, i) => i);
const GRID_COLUMNS_CLASS = "grid-cols-[34px_repeat(6,minmax(0,1fr))]";

interface DragCell {
  dayIndex: number;
  slot: number;
}

interface TimeTableSelectorProps {
  selected: ScheduleCondition[];
  onChange: (selected: ScheduleCondition[]) => void;
}

function toTimeText(minutes: number): string {
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00`;
}

function getSlotRange(slot: number): { startTime: string; endTime: string; label: string } {
  const startMinutes = 9 * 60 + slot * 60;
  const endMinutes = startMinutes + 60;

  return {
    startTime: toTimeText(startMinutes),
    endTime: toTimeText(endMinutes),
    label: `${String(Math.floor(startMinutes / 60)).padStart(2, "0")}:00`,
  };
}

function toSchedule(day: CourseDayOfWeek, slot: number): ScheduleCondition {
  const { startTime, endTime } = getSlotRange(slot);
  return { dayOfWeek: day, startTime, endTime };
}

function toScheduleKey(schedule: ScheduleCondition): string {
  return `${schedule.dayOfWeek}-${schedule.startTime}-${schedule.endTime}`;
}

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

function sortSchedules(schedules: ScheduleCondition[]): ScheduleCondition[] {
  return [...schedules].sort((a, b) => {
    const dayOrderDiff = DAYS.indexOf(a.dayOfWeek) - DAYS.indexOf(b.dayOfWeek);
    if (dayOrderDiff !== 0) {
      return dayOrderDiff;
    }
    return a.startTime.localeCompare(b.startTime);
  });
}

export function TimeTableSelector({ selected, onChange }: TimeTableSelectorProps) {
  const isDraggingRef = useCallback((val: boolean) => {
    (window as any)._isDragging = val;
  }, []);
  const getIsDragging = () => (window as any)._isDragging || false;

  const [isDragging, _setIsDragging] = useState(false);
  const setIsDragging = (val: boolean) => {
    _setIsDragging(val);
    isDraggingRef(val);
  };
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

  const startDrag = (day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) return;

    const schedule = toSchedule(day, slot);
    const scheduleKey = toScheduleKey(schedule);
    const mode = selectedKeySet.has(scheduleKey) ? "deselect" : "select";

    setDragSnapshot(selected);
    setIsDragging(true);
    setDragMode(mode);
    setDragStart({ dayIndex, slot });
    setDragCurrent({ dayIndex, slot });
  };

  const onMouseEnterCell = useCallback((day: CourseDayOfWeek, slot: number) => {
    setHoveredDay(day);
    setHoveredSlot(slot);
    if (!getIsDragging()) return;
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex >= 0) {
      setDragCurrent({ dayIndex, slot });
    }
  }, []);

  const finishDragSelection = useCallback(() => {
    if (!getIsDragging() || !dragMode || !dragStart || !dragCurrent) {
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
  }, [dragCurrent, dragMode, dragRectSchedules, dragSnapshot, dragStart, onChange]);

  useEffect(() => {
    const handleUp = () => finishDragSelection();
    window.addEventListener("mouseup", handleUp);
    window.addEventListener("touchend", handleUp);
    window.addEventListener("touchcancel", handleUp);
    return () => {
      window.removeEventListener("mouseup", handleUp);
      window.removeEventListener("touchend", handleUp);
      window.removeEventListener("touchcancel", handleUp);
    };
  }, [finishDragSelection]);

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
    if (!isDragging) return false;
    const key = toScheduleKey(toSchedule(day, slot));
    return dragRectKeySet.has(key);
  };

  // 모든 요일/교시를 선택 상태로 변경
  const handleSelectAll = () => {
    const allSlots: ScheduleCondition[] = [];
    DAYS.forEach((day) => {
      SLOT_NUMBERS.forEach((slot) => {
        allSlots.push(toSchedule(day, slot));
      });
    });
    onChange(sortSchedules(allSlots));
  };

  // 모든 선택을 해제
  const handleDeselectAll = () => {
    onChange([]);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    if (!getIsDragging()) return;
    const touch = e.touches[0];
    const element = document.elementFromPoint(touch.clientX, touch.clientY);
    if (!element) return;

    const cell = element.closest("[data-day][data-slot]");
    if (!cell) return;

    const day = cell.getAttribute("data-day") as CourseDayOfWeek;
    const slot = Number(cell.getAttribute("data-slot"));

    if (day && !Number.isNaN(slot)) {
      onMouseEnterCell(day, slot);
    }
  };

  return (
    <div
      className="w-full max-w-full select-none overflow-hidden rounded-xl border border-border/20 bg-card/20 p-3 backdrop-blur-md"
      onMouseLeave={() => {
        setHoveredDay(null);
        setHoveredSlot(null);
      }}
      style={{ touchAction: isDragging ? "none" : "auto" }}
    >
      <div className="mb-3 flex justify-end gap-2">
        <Button
          variant="outline"
          size="xs"
          onClick={handleSelectAll}
          className="h-7 border-border/40 bg-background/50 text-[11px] hover:bg-muted"
        >
          전체 선택
        </Button>
        <Button
          variant="outline"
          size="xs"
          onClick={handleDeselectAll}
          className="h-7 border-border/40 bg-background/50 text-[11px] hover:bg-muted"
        >
          전체 해제
        </Button>
      </div>

      <div className="w-full">
        <div className={cn("mb-1 grid gap-1", GRID_COLUMNS_CLASS)}>
          <div className="flex items-center justify-center rounded-sm border border-border/60 bg-muted text-[9px] font-black text-foreground">
            시간
          </div>
          {DAYS.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => handleSelectAllByDay(day)}
              onMouseEnter={() => setHoveredDay(day)}
              className={cn(
                "rounded-sm border border-border/60 py-1 text-center text-[11px] font-bold transition-all bg-muted text-foreground",
                hoveredDay === day
                  ? "bg-muted text-foreground border-border/60"
                  : "text-muted-foreground/60 hover:text-foreground/80 hover:bg-muted/50",
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
                  "grid gap-1 rounded-sm transition-colors",
                  GRID_COLUMNS_CLASS,
                  slot % 2 === 1 && "bg-muted/5",
                  hoveredSlot === slot && "bg-muted/10",
                )}
              >
                <div
                  className={cn(
                    "flex flex-col items-center justify-center rounded-sm border border-border/60 bg-muted transition-colors",
                    hoveredSlot === slot && "bg-muted/80"
                  )}
                >
                  <span className="text-[10px] font-black leading-none text-foreground">
                    {slotRange.label}
                  </span>
                  <span className="text-[8px] font-bold text-foreground/80">{slot + 1}교시</span>
                </div>

                {DAYS.map((day) => {
                  const selected = isCellSelected(day, slot);
                  const inDrag = isCellInDragRect(day, slot);
                  const dragging = isDragging;

                  let cellClassName = "bg-background/40 hover:bg-muted/30 border border-border/30";

                  if (dragging && inDrag) {
                    if (dragMode === "select") {
                      // 선택 드래그 중
                      cellClassName = "bg-primary/30 ring-2 ring-primary/40 border-primary/20";
                    } 
                    else if (dragMode === "deselect") {
                         // 해제 모드
                         cellClassName = "bg-muted/30 border-border/20";
                    }
                  } else if (selected) {
                    // 선택됨 (테마색 + 투명도)
                    cellClassName = "bg-primary/70 text-primary-foreground shadow-sm hover:bg-primary/60 border-primary/20";
                  } else if (hoveredDay === day) {
                    // 마우스 오버
                    cellClassName += " bg-muted/50 border-border/40";
                  }

                  return (
                    <button
                      key={`${day}-${slot}`}
                      type="button"
                      data-day={day}
                      data-slot={slot}
                      onMouseDown={(e) => {
                        e.preventDefault();
                        startDrag(day, slot);
                      }}
                      onMouseEnter={() => onMouseEnterCell(day, slot)}
                      onTouchStart={(e) => {
                        startDrag(day, slot);
                      }}
                      onTouchMove={onTouchMove}
                      aria-label={`${day} ${slot + 1}교시`}
                      className={cn(
                        "h-10 rounded-sm transition-all duration-100",
                        cellClassName
                      )}
                      style={{ touchAction: 'none' }}
                    />
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
