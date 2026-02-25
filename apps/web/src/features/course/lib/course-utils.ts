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

/**
 * "HH:mm" 또는 "HH:mm:ss" 형식의 문자열을 분(minutes) 단위 숫자로 변환
 */
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


/**
 * 분 단위 숫자를 "HH:mm:00" 형식의 시간 문자열로 변환
 */
export function toTimeText(minutes: number): string {
  const hour = Math.floor(minutes / 60);
  const minute = minutes % 60;
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}:00`;
}

/**
 * 시간대 정보를 식별하기 위한 고유 키 생성
 */
export function toScheduleKey(schedule: ScheduleCondition): string {
  return `${schedule.dayOfWeek}-${schedule.startTime}-${schedule.endTime}`;
}

/**
 * 기존 시간표 정보를 분석하여 비어있는 시간대(공강) 목록을 생성
 */
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

/**
 * 검색 조건 객체에서 유효하지 않은 값(undefined, 빈 문자열 등)을 제거하여 정제
 */
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
