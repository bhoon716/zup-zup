import { TimetableResponse, CustomScheduleTimeResponse } from '@/shared/types/api';
import { formatDayOfWeek } from '@/shared/lib/formatters';

export const WEEK_DAYS = ['월', '화', '수', '목', '금', '토', '일'] as const;

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
  classroom?: string;
  classification?: string;
  credits?: string;
  courseKey?: string;
  isOverlap?: boolean;
  overlapRegions?: { 
    startTime: string; 
    endTime: string;
    overlappingBlocks: { title: string; subTitle?: string }[];
  }[];
}

const COURSE_COLORS = [
  '#ef4444', // Red
  '#3b82f6', // Blue
  '#10b981', // Emerald
  '#f59e0b', // Amber
  '#8b5cf6', // Violet
  '#f43f5e', // Rose
  '#06b6d4', // Cyan
  '#84cc16', // Lime
  '#f97316', // Orange
  '#6366f1', // Indigo
  '#14b8a6', // Teal
  '#ec4899', // Pink
  '#eab308', // Yellow
  '#0ea5e9', // Sky
  '#d946ef', // Fuchsia
  '#22c55e', // Green
];

export const getCourseColor = (courseId: string | number): string => {
  // Returns the same color based on the course identifier to maintain consistency on the screen.
  const str = String(courseId);
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % COURSE_COLORS.length;
  return COURSE_COLORS[index];
};

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

  timetable.courses?.forEach((entry) => {
    entry.schedules?.forEach((schedule, idx) => {
      flattened.push({
        key: `course-${entry.courseKey}-${idx}`,
        id: entry.courseKey,
        type: 'course',
        title: entry.name,
        subTitle: entry.professor,
        dayOfWeek: formatDayOfWeek(schedule.dayOfWeek),
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        color: getCourseColor(entry.courseKey),
        classroom: entry.classroom,
        classification: entry.classification,
        credits: entry.credits,
        courseKey: entry.courseKey,
      });
    });
  });

  timetable.customSchedules?.forEach((schedule) => {
    schedule.schedules?.forEach((slot) => {
      flattened.push({
        key: `custom-${schedule.id}-${slot.id}`,
        id: schedule.id,
        type: 'custom',
        title: schedule.title,
        subTitle: schedule.professor,
        dayOfWeek: formatDayOfWeek(slot.dayOfWeek),
        startTime: slot.startTime,
        endTime: slot.endTime,
        classroom: slot.classroom,
        color: getCourseColor(schedule.title),
      });
    });
  });

  const merged: RenderingBlock[] = [];
  const processedKeys = new Set<string>();
  const sorted = [...flattened].sort((a, b) => {
    if (a.dayOfWeek !== b.dayOfWeek) {
      return WEEK_DAYS.indexOf(a.dayOfWeek as any) - WEEK_DAYS.indexOf(b.dayOfWeek as any);
    }
    return getTimeInMinutes(a.startTime) - getTimeInMinutes(b.startTime);
  });

  for (let i = 0; i < sorted.length; i++) {
    const current = sorted[i];

    if (processedKeys.has(current.key)) continue;

    // 같은 강의가 연속 시간대로 붙어 있으면 하나의 블록으로 합친다.
    const consecutive = [current];
    processedKeys.add(current.key);
    
    for (let j = i + 1; j < sorted.length; j++) {
      const next = sorted[j];
      const last = consecutive[consecutive.length - 1];
      
      if (
        next.id === current.id &&
        next.dayOfWeek === current.dayOfWeek &&
        next.type === current.type &&
        getTimeInMinutes(next.startTime) === getTimeInMinutes(last.endTime)
      ) {
        consecutive.push(next);
        processedKeys.add(next.key);
      }
    }

    if (consecutive.length > 1) {
      merged.push({
        ...current,
        key: `merged-${current.id}-${current.dayOfWeek}-${current.startTime}`,
        endTime: consecutive[consecutive.length - 1].endTime,
      });
    } else {
      merged.push(current);
    }
  }

  return merged.map((block, _, all) => {
    const overlappingBlocks = all.filter((other) => {
      if (block.key === other.key) return false;
      if (block.dayOfWeek !== other.dayOfWeek) return false;
      return isOverlapping(block.startTime, block.endTime, other.startTime, other.endTime);
    });

    const overlapRegions: { 
      startTime: string; 
      endTime: string;
      overlappingBlocks: { title: string; subTitle?: string }[];
    }[] = [];
    
    if (overlappingBlocks.length > 0) {
      overlappingBlocks.forEach((other) => {
        const blockStartMin = getTimeInMinutes(block.startTime);
        const blockEndMin = getTimeInMinutes(block.endTime);
        const otherStartMin = getTimeInMinutes(other.startTime);
        const otherEndMin = getTimeInMinutes(other.endTime);

        const overlapStart = Math.max(blockStartMin, otherStartMin);
        const overlapEnd = Math.min(blockEndMin, otherEndMin);

        if (overlapStart < overlapEnd) {
          const startHour = Math.floor(overlapStart / 60);
          const startMin = overlapStart % 60;
          const endHour = Math.floor(overlapEnd / 60);
          const endMin = overlapEnd % 60;

          const regionKey = `${startHour}:${startMin}-${endHour}:${endMin}`;
          const existingRegion = overlapRegions.find(
            r => `${r.startTime}-${r.endTime}` === regionKey
          );

          if (existingRegion) {
            existingRegion.overlappingBlocks.push({
              title: other.title,
              subTitle: other.subTitle,
            });
          } else {
            // 겹침 안내 다이얼로그에서 시간대별 충돌 목록을 보여주기 위해 구간을 모은다.
            overlapRegions.push({
              startTime: `${String(startHour).padStart(2, '0')}:${String(startMin).padStart(2, '0')}`,
              endTime: `${String(endHour).padStart(2, '0')}:${String(endMin).padStart(2, '0')}`,
              overlappingBlocks: [{
                title: other.title,
                subTitle: other.subTitle,
              }],
            });
          }
        }
      });
    }

    return { 
      ...block, 
      isOverlap: overlappingBlocks.length > 0,
      overlapRegions: overlapRegions.length > 0 ? overlapRegions : undefined,
    };
  });
};
/**
 * 연속된 시간대의 스케줄 정보를 하나로 병합합니다.
 */
export function mergeAdjacentSchedules(schedules: CustomScheduleTimeResponse[]): CustomScheduleTimeResponse[] {
  const merged: CustomScheduleTimeResponse[] = [];
  const sorted = [...schedules].sort((a, b) => {
    const dayA = formatDayOfWeek(a.dayOfWeek);
    const dayB = formatDayOfWeek(b.dayOfWeek);
    const dayDiff = WEEK_DAYS.indexOf(dayA as any) - WEEK_DAYS.indexOf(dayB as any);
    if (dayDiff !== 0) return dayDiff;
    return a.startTime.localeCompare(b.startTime);
  });

  for (const slot of sorted) {
    const last = merged[merged.length - 1];
    if (last && formatDayOfWeek(last.dayOfWeek) === formatDayOfWeek(slot.dayOfWeek) && last.endTime === slot.startTime) {
      merged[merged.length - 1] = { ...last, endTime: slot.endTime };
    } else {
      merged.push({ ...slot });
    }
  }

  return merged;
}
