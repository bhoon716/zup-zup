import {
  TimetableDetailResponse,
  CourseDayOfWeek,
  ScheduleCondition,
  CourseSearchCondition,
} from "@/shared/types/api";
import {
  SMART_FILTER_DAYS,
  SMART_FILTER_START_MINUTES,
  SMART_FILTER_SLOT_MINUTES,
  SMART_FILTER_SLOT_COUNT,
} from "../constants/course-options";
import { formatDayOfWeek } from "@/shared/lib/formatters";

const SEARCH_DRAFT_BASE_KEYS: (keyof CourseSearchCondition)[] = [
  "academicYear",
  "semester",
  "disclosure",
  "sortBy",
  "sortOrder",
];




export function toMinutes(value?: string): number | null {
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





export function toTimeText(minutes: number): string {
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00`;
}




export function toScheduleKey(schedule: ScheduleCondition): string {
  return `${schedule.dayOfWeek}-${schedule.startTime}-${schedule.endTime}`;
}




export function buildFreeSchedulesFromTimetable(
  timetable: TimetableDetailResponse,
): ScheduleCondition[] {
  const schedules = [
    ...(timetable?.courses?.flatMap((course) => course.schedules ?? []) ?? []),
    ...(timetable?.customSchedules?.flatMap((schedule) => schedule.schedules ?? []) ?? []),
  ];

  const allSlots: ScheduleCondition[] = [];

  SMART_FILTER_DAYS.forEach((day: CourseDayOfWeek) => {
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

  const occupiedSlotKeys = new Set<string>();

  schedules.forEach((schedule) => {
    const day = formatDayOfWeek(schedule.dayOfWeek) as CourseDayOfWeek;
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
      
      occupiedSlotKeys.add(toScheduleKey(slotSchedule));
    }
  });

  return allSlots.filter((slot) => !occupiedSlotKeys.has(toScheduleKey(slot)));
}




export function sanitizeCondition(
  condition: CourseSearchCondition,
): CourseSearchCondition {
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

    sanitized[key as keyof CourseSearchCondition] = value;
  });

  return sanitized;
}




export function resolveVisibleSearchCondition(
  draftCondition: CourseSearchCondition,
  resolvedDefaultCondition: CourseSearchCondition,
  fallbackCondition: CourseSearchCondition,
): CourseSearchCondition {
  const sanitized = sanitizeCondition(draftCondition);
  const keys = Object.keys(sanitized) as (keyof CourseSearchCondition)[];
  const hasOnlyBaseKeys = keys.every((key) => SEARCH_DRAFT_BASE_KEYS.includes(key));
  if (!hasOnlyBaseKeys) {
    return draftCondition;
  }

  const isFallbackBaseCondition = SEARCH_DRAFT_BASE_KEYS.every(
    (key) => draftCondition[key] === fallbackCondition[key],
  );

  return isFallbackBaseCondition ? resolvedDefaultCondition : draftCondition;
}
