import { TimetableResponse } from '@/types/api';

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
  overlapRegions?: { 
    startTime: string; 
    endTime: string;
    overlappingBlocks: { title: string; subTitle?: string }[];
  }[];
}

const COURSE_COLORS = [
  '#3b82f6',
  '#8b5cf6',
  '#ec4899',
  '#f59e0b',
  '#10b981',
  '#06b6d4',
  '#f97316',
  '#6366f1',
  '#14b8a6',
  '#a855f7',
];

export const getCourseColor = (courseId: string | number): string => {
  // 강의 식별자 기준으로 항상 같은 색을 반환해 화면 일관성을 유지한다.
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

const DAY_MAP: Record<string, string> = {
  'MONDAY': '월',
  'TUESDAY': '화',
  'WEDNESDAY': '수',
  'THURSDAY': '목',
  'FRIDAY': '금',
  'SATURDAY': '토',
  'SUNDAY': '일',
};

export const mapDayOfWeek = (englishDay: string): string => {
  return DAY_MAP[englishDay] || englishDay;
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
        dayOfWeek: mapDayOfWeek(schedule.dayOfWeek),
        startTime: schedule.startTime,
        endTime: schedule.endTime,
        color: getCourseColor(entry.courseKey),
      });
    });
  });

  timetable.customSchedules?.forEach((schedule) => {
    flattened.push({
      key: `custom-${schedule.id}`,
      id: schedule.id,
      type: 'custom',
      title: schedule.title,
      dayOfWeek: mapDayOfWeek(schedule.dayOfWeek),
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      color: schedule.color,
    });
  });

  const merged: RenderingBlock[] = [];
  const processedKeys = new Set<string>();
  const sorted = [...flattened].sort((a, b) => {
    if (a.dayOfWeek !== b.dayOfWeek) {
      const dayOrder = ['월', '화', '수', '목', '금', '토', '일'];
      return dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek);
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
