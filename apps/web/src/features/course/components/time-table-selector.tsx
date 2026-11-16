"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent } from "react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import type { CourseDayOfWeek, ScheduleCondition } from "@/shared/types/api";
import {
  SMART_FILTER_START_MINUTES,
  SMART_FILTER_SLOT_MINUTES,
  SMART_FILTER_SLOT_COUNT,
} from "../constants/course-options";

const DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SLOT_NUMBERS = Array.from({ length: SMART_FILTER_SLOT_COUNT }, (_, i) => i);
const GRID_COLUMNS_CLASS = "grid-cols-[34px_repeat(6,minmax(0,1fr))]";

type DragMode = "select" | "deselect";

interface DragCell {
  dayIndex: number;
  slot: number;
}

interface DragState {
  start: DragCell;
  currentCell: DragCell;
  mode: DragMode;
  snapshot: ScheduleCondition[];
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
  const startMinutes = SMART_FILTER_START_MINUTES + slot * SMART_FILTER_SLOT_MINUTES;
  const endMinutes = startMinutes + SMART_FILTER_SLOT_MINUTES;
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

function buildNextSchedulesFromDrag(dragState: DragState): ScheduleCondition[] {
  const dragCells = getDragCells(dragState.start, dragState.currentCell);
  const nextMap = new Map(dragState.snapshot.map((s) => [toScheduleKey(s), s]));

  dragCells.forEach(({ dayIndex, slot }) => {
    const schedule = toSchedule(DAYS[dayIndex], slot);
    const key = toScheduleKey(schedule);
    if (dragState.mode === "select") {
      nextMap.set(key, schedule);
    } else {
      nextMap.delete(key);
    }
  });

  return sortSchedules(Array.from(nextMap.values()));
}

export function TimeTableSelector({ selected, onChange }: TimeTableSelectorProps) {
  const [dragState, setDragState] = useState<DragState | null>(null);
  const activePointerIdRef = useRef<number | null>(null);

  const selectedKeySet = useMemo(
    () => new Set(selected.map((s) => toScheduleKey(s))),
    [selected],
  );

  const dragRectKeySet = useMemo(() => {
    if (!dragState) return new Set<string>();
    return new Set(
      getDragCells(dragState.start, dragState.currentCell).map(({ dayIndex, slot }) =>
        toScheduleKey(toSchedule(DAYS[dayIndex], slot)),
      ),
    );
  }, [dragState]);

  const displayKeySet = useMemo(() => {
    if (!dragState) return selectedKeySet;
    const previewSet = new Set(dragState.snapshot.map((s) => toScheduleKey(s)));
    dragRectKeySet.forEach((key) => {
      if (dragState.mode === "select") {
        previewSet.add(key);
      } else {
        previewSet.delete(key);
      }
    });
    return previewSet;
  }, [dragState, dragRectKeySet, selectedKeySet]);

  const startDrag = useCallback((day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) return;

    const mode: DragMode = selectedKeySet.has(toScheduleKey(toSchedule(day, slot)))
      ? "deselect"
      : "select";

    setDragState({
      start: { dayIndex, slot },
      currentCell: { dayIndex, slot },
      mode,
      snapshot: selected,
    });
  }, [selected, selectedKeySet]);

  const updateDragCell = useCallback((day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) return;

    setDragState((prev) => {
      if (!prev) return prev;
      if (prev.currentCell.dayIndex === dayIndex && prev.currentCell.slot === slot) return prev;
      return { ...prev, currentCell: { dayIndex, slot } };
    });
  }, []);

  const finishDrag = useCallback((pointerId?: number) => {
    if (pointerId !== undefined && activePointerIdRef.current !== pointerId) return;
    if (!dragState) return;

    onChange(buildNextSchedulesFromDrag(dragState));
    setDragState(null);
    activePointerIdRef.current = null;
  }, [dragState, onChange]);

  useEffect(() => {
    if (!dragState) return;

    const handlePointerUp = (event: globalThis.PointerEvent) => finishDrag(event.pointerId);

    const handlePointerCancel = (event: globalThis.PointerEvent) => finishDrag(event.pointerId);

    window.addEventListener("pointerup", handlePointerUp);
    window.addEventListener("pointercancel", handlePointerCancel);
    return () => {
      window.removeEventListener("pointerup", handlePointerUp);
      window.removeEventListener("pointercancel", handlePointerCancel);
    };
  }, [dragState, finishDrag]);

  const handlePointerMove = (event: PointerEvent<HTMLDivElement>) => {
    if (!dragState || activePointerIdRef.current !== event.pointerId) return;

    const hoveredElement = document.elementFromPoint(event.clientX, event.clientY);
    const cell = hoveredElement?.closest("[data-day][data-slot]") as HTMLElement | null;
    if (!cell) return;

    const day = cell.dataset.day as CourseDayOfWeek | undefined;
    const slot = Number(cell.dataset.slot);
    if (!day || Number.isNaN(slot)) return;

    updateDragCell(day, slot);
  };

  const handlePointerDownCell = (event: PointerEvent<HTMLButtonElement>, day: CourseDayOfWeek, slot: number) => {
    if (event.pointerType === "mouse" && event.button !== 0) return;
    activePointerIdRef.current = event.pointerId;
    startDrag(day, slot);
    try {
      event.currentTarget.setPointerCapture(event.pointerId);
    } catch {
    }
  };

  const handleToggleCell = (day: CourseDayOfWeek, slot: number) => {
    const schedule = toSchedule(day, slot);
    const key = toScheduleKey(schedule);
    const next = selectedKeySet.has(key)
      ? selected.filter((entry) => toScheduleKey(entry) !== key)
      : [...selected, schedule];
    onChange(sortSchedules(next));
  };

  const handleSelectAllByDay = (day: CourseDayOfWeek) => {
    const daySchedules = SLOT_NUMBERS.map((slot) => toSchedule(day, slot));
    const dayScheduleKeys = new Set(daySchedules.map((s) => toScheduleKey(s)));
    const isAllSelected = daySchedules.every((s) => selectedKeySet.has(toScheduleKey(s)));

    if (isAllSelected) {
      onChange(sortSchedules(selected.filter((s) => !dayScheduleKeys.has(toScheduleKey(s)))));
    } else {
      const nextMap = new Map(selected.map((s) => [toScheduleKey(s), s]));
      daySchedules.forEach((s) => nextMap.set(toScheduleKey(s), s));
      onChange(sortSchedules(Array.from(nextMap.values())));
    }
  };

  const handleSelectAll = () => {
    const all: ScheduleCondition[] = [];
    DAYS.forEach((day) => SLOT_NUMBERS.forEach((slot) => all.push(toSchedule(day, slot))));
    onChange(sortSchedules(all));
  };

  const handleDeselectAll = () => onChange([]);

  return (
    <div
      className="w-full select-none"
      onPointerMove={handlePointerMove}
      onPointerUp={(e) => finishDrag(e.pointerId)}
      onPointerCancel={(e) => finishDrag(e.pointerId)}
    >
      <div className="mb-2 flex justify-end gap-2">
        <Button type="button" variant="outline" size="xs" onClick={handleSelectAll} className="h-7">
          전체 선택
        </Button>
        <Button type="button" variant="outline" size="xs" onClick={handleDeselectAll} className="h-7">
          전체 해제
        </Button>
      </div>

      <div className="w-full">
        <div className={cn("mb-1 grid gap-1", GRID_COLUMNS_CLASS)}>
          <div className="flex items-center justify-center text-[11px] font-medium text-muted-foreground">시간</div>
          {DAYS.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => handleSelectAllByDay(day)}
              className="rounded-[6px] bg-neutral-100 py-1 text-center text-xs font-medium text-foreground hover:bg-neutral-200 focus-visible:outline-2 focus-visible:outline-primary dark:bg-white/8 dark:hover:bg-white/12"
            >
              {day}
            </button>
          ))}
        </div>

        <div className="space-y-[3px]">
          {SLOT_NUMBERS.map((slot) => {
            const slotRange = getSlotRange(slot);
            return (
              <div key={slot} className={cn("grid gap-1", GRID_COLUMNS_CLASS)}>
                <div className="flex flex-col items-center justify-center gap-0.5">
                  <span className="text-[11px] font-medium leading-none tabular-nums">{slotRange.label}</span>
                  <span className="text-[9px] text-muted-foreground">
                    {Math.floor(SMART_FILTER_START_MINUTES / 60) - 8 + slot}교시
                  </span>
                </div>

                {DAYS.map((day) => {
                  const key = toScheduleKey(toSchedule(day, slot));
                  const isSelected = displayKeySet.has(key);

                  return (
                    <button
                      key={`${day}-${slot}`}
                      type="button"
                      data-day={day}
                      data-slot={slot}
                      aria-label={`${day} ${Math.floor(SMART_FILTER_START_MINUTES / 60) - 8 + slot}교시`}
                      aria-pressed={selectedKeySet.has(key)}
                      className={cn(
                        "h-[30px] touch-none rounded-[6px] border transition-colors focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-primary",
                        isSelected
                          ? "border-teal-700/30 bg-teal-700/12 hover:bg-teal-700/18 dark:border-teal-400/40 dark:bg-teal-400/15 dark:hover:bg-teal-400/25"
                          : "border-neutral-200 bg-card hover:border-teal-700/25 hover:bg-teal-700/5 dark:border-white/12 dark:hover:border-teal-400/35 dark:hover:bg-teal-400/8",
                      )}
                      onClick={(event) => {
                        if (event.detail === 0) handleToggleCell(day, slot);
                      }}
                      onPointerDown={(e) => handlePointerDownCell(e, day, slot)}
                      onPointerEnter={(e) => activePointerIdRef.current === e.pointerId && updateDragCell(day, slot)}
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
