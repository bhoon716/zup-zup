import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import * as courseApi from '@/lib/api/course';
import type { CourseSearchCondition, Course, SliceResponse } from '@/types/api';

export const useCourses = (condition: CourseSearchCondition) => {
  return useInfiniteQuery({
    queryKey: ['courses', condition],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await courseApi.searchCourses(condition, pageParam as number);
      const sliceData = response.data;
      
      // Handle slice response properly
      let courses: Course[] = [];
      if ('content' in sliceData) {
          courses = sliceData.content;
      } else if (Array.isArray(sliceData)) {
          // Fallback if API returns array directly (unlikely with new change but safe)
          courses = sliceData; 
      }

      // Normalize fields
      const normalizedCourses = courses.map(course => ({
        ...course,
        capacity: course.capacity ?? course.totalSeats ?? 0,
        current: course.current ?? course.currentSeats ?? 0,
        available: course.available ?? ((course.capacity ?? course.totalSeats ?? 0) - (course.current ?? course.currentSeats ?? 0)),
        professor: course.professor ?? course.professorName ?? "교수 미지정"
      }));

      return {
          content: normalizedCourses,
          last: 'last' in sliceData ? sliceData.last : true, 
          number: 'number' in sliceData ? sliceData.number : pageParam
      };
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return (lastPage.number as number) + 1;
    },
    initialPageParam: 0,
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
