import { useQuery } from '@tanstack/react-query';
import * as courseApi from '@/lib/api/course';
import type { CourseSearchCondition, Course, PageResponse } from '@/types/api';

export const useCourses = (condition: CourseSearchCondition) => {
  return useQuery({
    queryKey: ['courses', condition],
    queryFn: async () => {
      const response = await courseApi.searchCourses(condition);
      const rawData = response.data;
      // API 전환기 호환성 및 안정성을 위해 데이터 정규화 (배경: Page 객체 vs List 객체)
      if (Array.isArray(rawData)) return rawData;
      if (rawData && typeof rawData === 'object' && 'content' in rawData) {
        return (rawData as PageResponse<Course>).content;
      }
      return [];
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
      return response.data;
    },
    enabled: !!courseKey,
  });
};
