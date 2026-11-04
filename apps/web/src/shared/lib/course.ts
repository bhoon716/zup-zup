import { formatClassroom } from "./formatters";
import type { Course } from "@/shared/types/api";

export interface NormalizedCourse extends Course {
  capacity: number;
  current: number;
  available: number;
  professor: string;
}





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




export function hasIdentifiableProfessor(course: Partial<Course>) {
  const professor = (course.professor ?? course.professorName ?? "").trim();

  return professor !== "" && professor !== "교수 미지정";
}






export function getReviewScopeKey(course: Partial<Course>) {
  const subjectCode = course.subjectCode ?? "";
  const professor = (course.professor ?? course.professorName ?? "").trim();

  if (!hasIdentifiableProfessor(course)) {
    return `course:${course.courseKey ?? ""}`;
  }

  return `${subjectCode}::${professor}`;
}
