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
  period?: string;
  overlapRegions?: { 
    startTime: string; 
    endTime: string;
    overlappingBlocks: { title: string; subTitle?: string }[];
  }[];
}

// Generate consistent color for each course based on its ID
const COURSE_COLORS = [
  '#3b82f6', // blue
  '#8b5cf6', // purple
  '#ec4899', // pink
  '#f59e0b', // amber
  '#10b981', // emerald
  '#06b6d4', // cyan
  '#f97316', // orange
  '#6366f1', // indigo
  '#14b8a6', // teal
  '#a855f7', // violet
];

export const getCourseColor = (courseId: string | number): string => {
  const str = String(courseId);
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = str.charCodeAt(i) + ((hash << 5) - hash);
  }
  const index = Math.abs(hash) % COURSE_COLORS.length;
  return COURSE_COLORS[index];
};

// Format period string (e.g., "PERIOD_6A" -> "6-A교시")
export const formatPeriod = (period?: string): string => {
  if (!period) return '';
  
  // Remove "PERIOD_" prefix if exists
  const cleaned = period.replace(/^PERIOD_/, '');
  
  // Match pattern like "6A" or "10B"
  const match = cleaned.match(/^(\d+)([A-Z]?)$/);
  if (match) {
    const [, number, letter] = match;
    if (letter) {
      return `${number}-${letter}교시`;
    }
    return `${number}교시`;
  }
  
  return period;
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

// Map English day names from API to Korean day names used in UI
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

  // 1. 강의 데이터 변환
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
        period: schedule.period,
        color: getCourseColor(entry.courseKey),
      });
    });
  });

  // 2. 커스텀 일정 데이터 변환
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

  // 3. 같은 강의의 연속된 블록 병합
  const merged: RenderingBlock[] = [];
  const processedKeys = new Set<string>(); // Track which blocks have been processed
  const sorted = [...flattened].sort((a, b) => {
    if (a.dayOfWeek !== b.dayOfWeek) {
      const dayOrder = ['월', '화', '수', '목', '금', '토', '일'];
      return dayOrder.indexOf(a.dayOfWeek) - dayOrder.indexOf(b.dayOfWeek);
    }
    return getTimeInMinutes(a.startTime) - getTimeInMinutes(b.startTime);
  });

  for (let i = 0; i < sorted.length; i++) {
    const current = sorted[i];
    
    // 이미 처리된 블록이면 스킵
    if (processedKeys.has(current.key)) continue;
    
    // 같은 강의의 연속된 블록 찾기
    const consecutive = [current];
    processedKeys.add(current.key);
    
    for (let j = i + 1; j < sorted.length; j++) {
      const next = sorted[j];
      const last = consecutive[consecutive.length - 1];
      
      // 같은 강의, 같은 요일, 연속된 시간인지 확인
      if (
        next.id === current.id &&
        next.dayOfWeek === current.dayOfWeek &&
        next.type === current.type &&
        getTimeInMinutes(next.startTime) === getTimeInMinutes(last.endTime)
      ) {
        consecutive.push(next);
        processedKeys.add(next.key); // Mark as processed
      }
    }
    
    // 병합된 블록 생성
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

  // 4. 충돌 체크 및 겹치는 영역 계산
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

        // Calculate intersection
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
