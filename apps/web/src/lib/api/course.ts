import api from './index';
import type { CommonResponse, PageResponse, Course, CourseSearchCondition, CourseSeatHistory } from '@/types/api';

export const searchCourses = async (
  condition: CourseSearchCondition,
  page = 0,
  size = 20
): Promise<CommonResponse<PageResponse<Course>>> => {
  const { data } = await api.get('/api/v1/courses', {
    params: { ...condition, page, size },
  });
  return data;
};

export const getCourseHistory = async (courseKey: string): Promise<CommonResponse<CourseSeatHistory[]>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/history`);
  return data;
};
