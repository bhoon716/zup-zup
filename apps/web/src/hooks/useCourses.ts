import { useQuery } from '@tanstack/react-query';
import * as courseApi from '@/lib/api/course';
import type { CourseSearchCondition, Course, PageResponse } from '@/types/api';

export const useCourses = (condition: CourseSearchCondition) => {
  return useQuery({
    queryKey: ['courses', condition],
    queryFn: async () => {
      const response = await courseApi.searchCourses(condition);
      const rawData = response.data;
      let courses: Course[] = [];

      // API 전환기 호환성 및 안정성을 위해 데이터 정규화 (배경: Page 객체 vs List 객체)
      if (Array.isArray(rawData)) {
        courses = rawData;
      } else if (rawData && typeof rawData === 'object' && 'content' in rawData) {
        courses = (rawData as PageResponse<Course>).content;
      }

      // 필드명 정규화 (totalSeats -> capacity, currentSeats -> current, professorName -> professor)
      return courses.map(course => ({
        ...course,
        capacity: course.capacity ?? course.totalSeats ?? 0,
        current: course.current ?? course.currentSeats ?? 0,
        available: course.available ?? ((course.capacity ?? course.totalSeats ?? 0) - (course.current ?? course.currentSeats ?? 0)),
        professor: course.professor ?? course.professorName ?? "교수 미지정"
      }));
    },
  });
};

export const useCourseHistory = (courseKey: string) => {
  return useQuery({
    queryKey: ['course-history', courseKey],
    queryFn: async () => {
      const response = await courseApi.getCourseHistory(courseKey);
      return response.data;
    },
    enabled: !!courseKey,
  });
};

export const useCourseDetail = (courseKey: string) => {
  return useQuery({
    queryKey: ['course-detail', courseKey],
    queryFn: async () => {
      const response = await courseApi.getCourseDetail(courseKey);
      const course = response.data;
      if (!course) return null;

      // 필드명 정규화 (totalSeats -> capacity, currentSeats -> current, professorName -> professor)
      return {
        ...course,
        capacity: course.capacity ?? course.totalSeats ?? 0,
        current: course.current ?? course.currentSeats ?? 0,
        available: course.available ?? ((course.capacity ?? course.totalSeats ?? 0) - (course.current ?? course.currentSeats ?? 0)),
        professor: course.professor ?? course.professorName ?? "교수 미지정"
      };
    },
    enabled: !!courseKey,
  });
};
