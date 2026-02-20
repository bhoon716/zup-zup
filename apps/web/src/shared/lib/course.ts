import type { Course } from "@/shared/types/api";

export interface NormalizedCourse extends Course {
  capacity: number;
  current: number;
  available: number;
  professor: string;
}

/**
 * 서버에서 넘어온 다양한 형태의 강의 데이터를 일관된 형식으로 정규화합니다.
 * (예: capacity vs totalSeats, current vs currentSeats 등)
 */
export function normalizeCourse(course: Partial<Course>): NormalizedCourse {
  const capacity = Number(course.capacity ?? (course as any).totalSeats ?? 0);
  const current = Number(course.current ?? (course as any).currentSeats ?? 0);
  const available = Number(course.available ?? Math.max(capacity - current, 0));

  return {
    ...course,
    capacity,
    current,
    available,
    professor: course.professor || (course as any).professorName || "교수 미지정",
  } as NormalizedCourse;
}
