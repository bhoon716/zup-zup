import { useQuery } from '@tanstack/react-query';
import * as courseApi from '@/lib/api/course';
import type { CourseSearchCondition } from '@/types/api';

export const useCourses = (condition: CourseSearchCondition, page = 0, size = 20) => {
  return useQuery({
    queryKey: ['courses', condition, page, size],
    queryFn: async () => {
      const response = await courseApi.searchCourses(condition, page, size);
      return response.data;
    },
    enabled: !!(condition.name || condition.professor || condition.subjectCode),
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
