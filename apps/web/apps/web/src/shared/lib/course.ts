import { formatClassroom } from "./formatters";
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
  const capacity = Number(course.capacity ?? course.totalSeats ?? 0);
  const current = Number(course.current ?? course.currentSeats ?? 0);
  const available = Number(course.available ?? Math.max(capacity - current, 0));

  return {
    courseKey: course.courseKey ?? "",
    subjectCode: course.subjectCode ?? "",
    name: course.name ?? "",
    classNumber: course.classNumber ?? "",
    ...course,
    capacity,
    current,
    available,
    professor: course.professor || course.professorName || "교수 미지정",
    classroom: formatClassroom(course.classroom),
  };
}

/**
 * 리뷰를 묶는 공유 캐시 키를 생성합니다.
 * 같은 과목코드와 교수 조합이면 학기와 관계없이 같은 키를 사용합니다.
 */
export function getReviewScopeKey(course: Partial<Course>) {
  const subjectCode = course.subjectCode ?? "";
  const professor = (course.professor ?? course.professorName ?? "").trim();
  const normalizedProfessor = professor === "교수 미지정" ? "" : professor;

  return `${subjectCode}::${normalizedProfessor}`;
}
