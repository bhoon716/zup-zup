import { formatClassTime, formatDayOfWeek } from "./formatters";
import type { Course } from "@/shared/types/api";

const DAYS = ["월", "화", "수", "목", "금", "토", "일"];
const FIRST_PERIOD_START = 8 * 60;
const HALF_PERIOD_MINUTES = 30;
const LAST_PERIOD = 15;
const LAST_PERIOD_END = 23 * 60 + 59;
const HALF_PERIOD = /^(\d{1,2})-?([AB])$/i;
const HALF_PERIOD_RANGE = /^(\d{1,2}-?[AB])\s*[-~–]\s*(\d{1,2}-?[AB])$/i;
const FULL_PERIOD_RANGE = /^(\d{1,2})(?:\s*[-~–]\s*(\d{1,2}))?$/;
const CLOCK_RANGE = /^(\d{1,2}:\d{2})\s*[-~–]\s*(\d{1,2}:\d{2})$/;

interface TimeRange {
  start: number;
  end: number;
}

interface ScheduleRange extends TimeRange {
  day: string;
}

function readClock(value: string): number {
  const match = /^(\d{1,2}):(\d{2})(?::00(?:\.0+)?)?$/.exec(value);
  if (!match) return NaN;

  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  return hours < 24 && minutes < 60 ? hours * 60 + minutes : NaN;
}

function readHalfPeriod(value: string): TimeRange | null {
  const match = HALF_PERIOD.exec(value);
  if (!match) return null;

  const period = Number(match[1]);
  if (period > LAST_PERIOD) return null;

  const start = FIRST_PERIOD_START + period * 60 + (match[2].toUpperCase() === "B" ? HALF_PERIOD_MINUTES : 0);
  return { start, end: Math.min(start + HALF_PERIOD_MINUTES, LAST_PERIOD_END) };
}

function readTimeRange(value: string): TimeRange | null {
  const clock = CLOCK_RANGE.exec(value);
  if (clock) return { start: readClock(clock[1]), end: readClock(clock[2]) };

  const halfPeriods = HALF_PERIOD_RANGE.exec(value);
  const fullPeriods = FULL_PERIOD_RANGE.exec(value);
  const first = readHalfPeriod(halfPeriods?.[1] ?? (fullPeriods ? `${fullPeriods[1]}A` : value));
  const last = readHalfPeriod(halfPeriods?.[2] ?? (fullPeriods ? `${fullPeriods[2] ?? fullPeriods[1]}B` : value));
  return first && last ? { start: first.start, end: last.end } : null;
}

function readClassTime(value: string): ScheduleRange[] {
  return value.split(",").flatMap((token) => {
    const match = /^(\S+)\s+(.+)$/.exec(token.trim());
    if (!match) return [];

    const day = formatDayOfWeek(match[1]);
    const range = readTimeRange(match[2]);
    return range ? [{ day, ...range }] : [];
  });
}

function isValidRange({ day, start, end }: ScheduleRange): boolean {
  return DAYS.includes(day) && start < end;
}

function mergeRanges(ranges: ScheduleRange[]): ScheduleRange[] {
  ranges.sort((a, b) => DAYS.indexOf(a.day) - DAYS.indexOf(b.day) || a.start - b.start);
  const merged: ScheduleRange[] = [];

  for (const range of ranges) {
    const previous = merged.at(-1);
    if (previous?.day === range.day && previous.end >= range.start) {
      previous.end = Math.max(previous.end, range.end);
    } else {
      merged.push(range);
    }
  }

  return merged;
}

function formatClock(minutes: number): string {
  return `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;
}

function formatPeriodSlot(slot: number): string {
  return `${Math.floor(slot / 2)}-${slot % 2 === 0 ? "A" : "B"}`;
}

function formatPeriodRange({ start, end }: TimeRange): string | null {
  const firstSlot = (start - FIRST_PERIOD_START) / HALF_PERIOD_MINUTES;
  const endSlot = end === LAST_PERIOD_END
    ? (LAST_PERIOD + 1) * 2
    : (end - FIRST_PERIOD_START) / HALF_PERIOD_MINUTES;

  if (firstSlot < 0 || !Number.isInteger(firstSlot) || !Number.isInteger(endSlot)) return null;

  const count = endSlot - firstSlot;
  if (count > 3) return `${formatPeriodSlot(firstSlot)}–${formatPeriodSlot(endSlot - 1)}`;

  return Array.from({ length: count }, (_, offset) => formatPeriodSlot(firstSlot + offset)).join(" · ");
}

export function getCourseSchedule(course: Pick<Course, "classTime" | "schedules">) {
  const rawTime = formatClassTime(course.classTime);
  const structured = (course.schedules ?? []).map(({ dayOfWeek, startTime, endTime }) => ({
    day: formatDayOfWeek(dayOfWeek),
    start: readClock(startTime),
    end: readClock(endTime),
  })).filter(isValidRange);
  const ranges = structured.length > 0 ? structured : readClassTime(rawTime).filter(isValidRange);

  return {
    blocks: mergeRanges(ranges).map((range) => ({
      label: `${range.day} ${formatClock(range.start)}–${formatClock(range.end)}`,
      periodLabel: formatPeriodRange(range),
    })),
    fallback: rawTime || "시간 미배정",
  };
}
