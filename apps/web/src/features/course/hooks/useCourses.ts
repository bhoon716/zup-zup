import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import * as courseApi from '@/features/course/api/course.api';
import type { CourseSearchCondition, Course } from '@/shared/types/api';
import { normalizeCourse } from '@/shared/lib/course';

export const useCourses = (condition: CourseSearchCondition) => {
  return useInfiniteQuery({
    queryKey: ['courses', condition],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await courseApi.searchCourses(condition, pageParam as number);
      const sliceData = response.data;

      let courses: Course[] = [];
      if ('content' in sliceData) {
        courses = sliceData.content;
      } else if (Array.isArray(sliceData)) {
        courses = sliceData;
      }

      // 백엔드 응답 필드명이 달라도 화면에서 동일한 속성으로 다루도록 정규화한다.
      const normalizedCourses = courses.map(course => normalizeCourse(course));

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

      return normalizeCourse(course);
    },
    enabled: !!courseKey,
  });
};
