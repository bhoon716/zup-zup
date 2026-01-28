import { TimetableResponse, TimetableEntryResponse, CustomScheduleResponse, CourseDayOfWeek } from '@/types/api';

export interface RenderingBlock {
  key: string;
  id: number | string;
  type: 'course' | 'custom';
  title: string;
  subTitle?: string;
  dayOfWeek: string;
  startTime: string;
  endTime: string;
  color?: string;
  isOverlap?: boolean;
}

export const getTimeInMinutes = (timeStr: string): number => {
  const [h, m] = timeStr.split(':').map(Number);
  return h * 60 + m;
};

export const isOverlapping = (
  s1: string, e1: string,
  s2: string, e2: string
): boolean => {
  const start1 = getTimeInMinutes(s1);
  const end1 = getTimeInMinutes(e1);
  const start2 = getTimeInMinutes(s2);
  const end2 = getTimeInMinutes(e2);

  return start1 < end2 && start2 < end1;
};

export const getRenderingBlocks = (timetable: TimetableResponse): RenderingBlock[] => {
  const flattened: RenderingBlock[] = [];

  // 1. 강좌 데이터 변환
  timetable.entries.forEach((entry) => {
    entry.schedules.forEach((schedule, idx) => {
      flattened.push({
        key: `course-${entry.courseKey}-${idx}`,
        id: entry.courseKey,
        type: 'course',
        title: entry.courseName,
        subTitle: entry.professor,
        dayOfWeek: schedule.dayOfWeek,
        startTime: schedule.startTime,
        endTime: schedule.endTime,
      });
    });
  });

  // 2. 커스텀 일정 데이터 변환
  timetable.customSchedules.forEach((schedule) => {
    flattened.push({
      key: `custom-${schedule.id}`,
      id: schedule.id,
      type: 'custom',
      title: schedule.title,
      dayOfWeek: schedule.dayOfWeek,
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      color: schedule.color,
    });
  });

  // 3. 충돌 체크 포함 반환
  return flattened.map((block, _, all) => {
    const overlap = all.some((other) => {
      if (block.key === other.key) return false;
      if (block.dayOfWeek !== other.dayOfWeek) return false;
      return isOverlapping(block.startTime, block.endTime, other.startTime, other.endTime);
    });

    return { ...block, isOverlap: overlap };
  });
};
