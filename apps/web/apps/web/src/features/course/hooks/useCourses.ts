import { useInfiniteQuery, useQuery } from '@tanstack/react-query';
import * as courseApi from '@/features/course/api/course.api';
import type { CourseSearchCondition, Course, SearchDefaultSemesterResponse, CourseSearchPageResponse } from '@/shared/types/api';
import { normalizeCourse } from '@/shared/lib/course';

export const useCourses = (
  condition: CourseSearchCondition,
  options?: {
    enabled?: boolean;
    initialPage?: CourseSearchPageResponse;
  },
) => {
  return useInfiniteQuery({
    queryKey: ['courses', condition],
    enabled: options?.enabled ?? true,
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
    initialData: options?.initialPage
      ? {
          pages: [options.initialPage],
          pageParams: [0],
        }
      : undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return (lastPage.number as number) + 1;
    },
    initialPageParam: 0,
    staleTime: options?.initialPage ? 1000 * 60 * 5 : 0,
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
export const useSearchDefaultSemester = () => {
  return useQuery<SearchDefaultSemesterResponse>({
    queryKey: ['courses', 'search-default-semester'],
    queryFn: async () => {
      const response = await courseApi.getSearchDefaultSemester();
      return response.data;
    },
    staleTime: 1000 * 60 * 60,
  });
};

