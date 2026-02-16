"use client";

import { cn } from "@/lib/utils";
import type { CourseDayOfWeek, ScheduleCondition } from "@/types/api";
import { useCallback, useEffect, useState } from "react";

const DAYS: CourseDayOfWeek[] = ["월", "화", "수", "목", "금", "토"];
const SLOT_NUMBERS = Array.from({ length: 16 }, (_, i) => i);

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
  const startMinutes = 8 * 60 + slot * 60;
  const endMinutes = slot === 15 ? 23 * 60 + 59 : startMinutes + 60;

  return {
    startTime: toTimeText(startMinutes),
    endTime: toTimeText(endMinutes),
    label: `${String(Math.floor(startMinutes / 60)).padStart(2, "0")}:00`,
  };
}

export function TimeTableSelector({ selected, onChange }: TimeTableSelectorProps) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragMode, setDragMode] = useState<"select" | "deselect" | null>(null);
  const [hoveredDay, setHoveredDay] = useState<CourseDayOfWeek | null>(null);
  const [hoveredSlot, setHoveredSlot] = useState<number | null>(null);

  const isSelected = useCallback(
    (day: CourseDayOfWeek, slot: number) => {
      const { startTime, endTime } = getSlotRange(slot);
      return selected.some(
        (schedule) =>
          schedule.dayOfWeek === day &&
          schedule.startTime === startTime &&
          schedule.endTime === endTime,
      );
    },
    [selected],
  );

  const handleSlotAction = useCallback(
    (day: CourseDayOfWeek, slot: number, mode: "select" | "deselect") => {
      const { startTime, endTime } = getSlotRange(slot);
      const exists = selected.some(
        (schedule) =>
          schedule.dayOfWeek === day &&
          schedule.startTime === startTime &&
          schedule.endTime === endTime,
      );

      if (mode === "select" && !exists) {
        onChange([...selected, { dayOfWeek: day, startTime, endTime }]);
        return;
      }

      if (mode === "deselect" && exists) {
        onChange(
          selected.filter(
            (schedule) =>
              !(
                schedule.dayOfWeek === day &&
                schedule.startTime === startTime &&
                schedule.endTime === endTime
              ),
          ),
        );
      }
    },
    [onChange, selected],
  );

  const onMouseDown = (day: CourseDayOfWeek, slot: number) => {
    const mode = isSelected(day, slot) ? "deselect" : "select";
    setIsDragging(true);
    setDragMode(mode);
    handleSlotAction(day, slot, mode);
  };

  const onMouseEnter = (day: CourseDayOfWeek, slot: number) => {
    setHoveredDay(day);
    setHoveredSlot(slot);

    if (isDragging && dragMode) {
      handleSlotAction(day, slot, dragMode);
    }
  };

  useEffect(() => {
    const handleMouseUp = () => {
      setIsDragging(false);
      setDragMode(null);
    };

    window.addEventListener("mouseup", handleMouseUp);
    return () => window.removeEventListener("mouseup", handleMouseUp);
  }, []);

  const handleSelectAllByDay = (day: CourseDayOfWeek) => {
    const daySlots = SLOT_NUMBERS.map((slot) => ({ dayOfWeek: day, ...getSlotRange(slot) }));
    const selectedByDay = selected.filter((schedule) => schedule.dayOfWeek === day);
    const isFullySelected = daySlots.every((slot) =>
      selectedByDay.some(
        (schedule) =>
          schedule.startTime === slot.startTime && schedule.endTime === slot.endTime,
      ),
    );

    const withoutDay = selected.filter((schedule) => schedule.dayOfWeek !== day);
    onChange(isFullySelected ? withoutDay : [...withoutDay, ...daySlots]);
  };

  return (
    <div
      className="w-fit select-none overflow-hidden rounded-xl border border-white/10 bg-card/20 p-3 backdrop-blur-md"
      onMouseLeave={() => {
        setHoveredDay(null);
        setHoveredSlot(null);
      }}
    >
      <div className="w-full">
        <div className="mb-1 grid grid-cols-[60px_repeat(6,36px)] gap-1">
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
                  "grid grid-cols-[60px_repeat(6,36px)] gap-1 rounded-lg transition-colors",
                  slot % 2 === 1 && "bg-white/[0.02]",
                  hoveredSlot === slot && "bg-primary/[0.05]",
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
                  <span className="text-[8px] font-bold">{slot}교시</span>
                </div>

                {DAYS.map((day) => (
                  <button
                    key={`${day}-${slot}`}
                    type="button"
                    onMouseDown={() => onMouseDown(day, slot)}
                    onMouseEnter={() => onMouseEnter(day, slot)}
                    className={cn(
                      "h-10 rounded-md border border-white/5 transition-all duration-150",
                      hoveredDay === day && "border-primary/[0.2] bg-primary/[0.05]",
                      isSelected(day, slot)
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
          <p className="text-[9px] italic text-muted-foreground/40">* 드래그해서 선택</p>
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
