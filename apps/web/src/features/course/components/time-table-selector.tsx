"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type PointerEvent } from "react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import type { CourseDayOfWeek, ScheduleCondition } from "@/shared/types/api";

const DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SLOT_NUMBERS = Array.from({ length: 13 }, (_, i) => i);
const GRID_COLUMNS_CLASS = "grid-cols-[34px_repeat(6,minmax(0,1fr))]";

type DragMode = "select" | "deselect";

interface DragCell {
  dayIndex: number;
  slot: number;
}

interface DragState {
  start: DragCell;
  current: DragCell;
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

/**
 * 드래그 상태를 기반으로 다음 선택된 스케줄 목록을 계산합니다.
 */
function buildNextSchedulesFromDrag(dragState: DragState): ScheduleCondition[] {
  const dragCells = getDragCells(dragState.start, dragState.current);
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
  const [hoveredDay, setHoveredDay] = useState<CourseDayOfWeek | null>(null);
  const [hoveredSlot, setHoveredSlot] = useState<number | null>(null);
  const activePointerIdRef = useRef<number | null>(null);

  // 선택된 시간대 키 세트
  const selectedKeySet = useMemo(
    () => new Set(selected.map((s) => toScheduleKey(s))),
    [selected],
  );

  // 현재 드래그 영역에 포함된 시간대 키 세트
  const dragRectKeySet = useMemo(() => {
    if (!dragState) return new Set<string>();
    return new Set(
      getDragCells(dragState.start, dragState.current).map(({ dayIndex, slot }) =>
        toScheduleKey(toSchedule(DAYS[dayIndex], slot)),
      ),
    );
  }, [dragState]);

  // 화면에 표시될 최종 선택 상태 (드래그 중인 미실행 변경사항 포함)
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

  /**
   * 드래그 선택을 시작합니다.
   */
  const startDrag = useCallback((day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) return;

    const mode: DragMode = selectedKeySet.has(toScheduleKey(toSchedule(day, slot)))
      ? "deselect"
      : "select";

    setDragState({
      start: { dayIndex, slot },
      current: { dayIndex, slot },
      mode,
      snapshot: selected,
    });
    setHoveredDay(day);
    setHoveredSlot(slot);
  }, [selected, selectedKeySet]);

  /**
   * 드래그 중인 현재 위치를 업데이트합니다.
   */
  const updateDragCell = useCallback((day: CourseDayOfWeek, slot: number) => {
    const dayIndex = DAYS.indexOf(day);
    if (dayIndex < 0) return;

    setHoveredDay(day);
    setHoveredSlot(slot);
    setDragState((prev) => {
      if (!prev) return prev;
      if (prev.current.dayIndex === dayIndex && prev.current.slot === slot) return prev;
      return { ...prev, current: { dayIndex, slot } };
    });
  }, []);

  /**
   * 드래그 작업을 완료하고 변경사항을 적용합니다.
   */
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

  /**
   * 포인터 이동 이벤트 처리 핸들러
   */
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

  /**
   * 셀 포인터 다운 이벤트 처리 핸들러
   */
  const handlePointerDownCell = (event: PointerEvent<HTMLButtonElement>, day: CourseDayOfWeek, slot: number) => {
    if (event.pointerType === "mouse" && event.button !== 0) return;
    activePointerIdRef.current = event.pointerId;
    startDrag(day, slot);
    try {
      event.currentTarget.setPointerCapture(event.pointerId);
    } catch {
      // 캡처 실패 무시
    }
  };

  /**
   * 특정 요일의 모든 시간을 선택하거나 해제합니다.
   */
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
      className="w-full max-w-full select-none overflow-hidden rounded-xl border border-border/20 bg-card/20 p-3 backdrop-blur-md"
      onMouseLeave={() => {
        setHoveredDay(null);
        setHoveredSlot(null);
      }}
      onPointerMove={handlePointerMove}
      onPointerUp={(e) => finishDrag(e.pointerId)}
      onPointerCancel={(e) => finishDrag(e.pointerId)}
      style={{ touchAction: "none" }}
    >
      <div className="mb-3 flex justify-end gap-2">
        <Button variant="outline" size="xs" onClick={handleSelectAll} className="h-7 border-border/40 bg-background/50 text-[11px] hover:bg-muted">
          전체 선택
        </Button>
        <Button variant="outline" size="xs" onClick={handleDeselectAll} className="h-7 border-border/40 bg-background/50 text-[11px] hover:bg-muted">
          전체 해제
        </Button>
      </div>

      <div className="w-full">
        {/* 요일 헤더 */}
        <div className={cn("mb-1 grid gap-1", GRID_COLUMNS_CLASS)}>
          <div className="flex items-center justify-center rounded-sm border border-border/60 bg-muted text-[9px] font-black text-foreground">시간</div>
          {DAYS.map((day) => (
            <button
              key={day}
              type="button"
              onClick={() => handleSelectAllByDay(day)}
              onMouseEnter={() => setHoveredDay(day)}
              className={cn(
                "rounded-sm border border-border/60 bg-muted py-1 text-center text-[11px] font-bold text-foreground transition-all",
                hoveredDay === day ? "border-border/60 bg-muted text-foreground" : "text-muted-foreground/60 hover:bg-muted/50 hover:text-foreground/80",
              )}
            >
              {day}
            </button>
          ))}
        </div>

        {/* 시간표 격자 */}
        <div className="space-y-1">
          {SLOT_NUMBERS.map((slot) => {
            const slotRange = getSlotRange(slot);
            return (
              <div key={slot} className={cn("grid gap-1 rounded-sm transition-colors", GRID_COLUMNS_CLASS, slot % 2 === 1 && "bg-muted/5", hoveredSlot === slot && "bg-muted/10")}>
                <div className={cn("flex flex-col items-center justify-center rounded-sm border border-border/60 bg-muted transition-colors", hoveredSlot === slot && "bg-muted/80")}>
                  <span className="text-[10px] font-black leading-none text-foreground">{slotRange.label}</span>
                  <span className="text-[8px] font-bold text-foreground/80">{slot + 1}교시</span>
                </div>

                {DAYS.map((day) => {
                  const key = toScheduleKey(toSchedule(day, slot));
                  const isSelected = displayKeySet.has(key);
                  const isDraggingCell = dragState && dragRectKeySet.has(key);

                  let cellClassName = "border border-border/30 bg-background/40 hover:bg-muted/30";
                  if (dragState && isDraggingCell) {
                    cellClassName = dragState.mode === "select" ? "border-primary/20 bg-primary/30 ring-2 ring-primary/40" : "border-border/20 bg-muted/30";
                  } else if (isSelected) {
                    cellClassName = "border-primary/20 bg-primary/70 text-primary-foreground shadow-sm hover:bg-primary/60";
                  } else if (hoveredDay === day) {
                    cellClassName += " border-border/40 bg-muted/50";
                  }

                  return (
                    <button
                      key={`${day}-${slot}`}
                      type="button"
                      data-day={day}
                      data-slot={slot}
                      aria-label={`${day} ${slot + 1}교시`}
                      className={cn("h-10 rounded-sm transition-all duration-100", cellClassName)}
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
