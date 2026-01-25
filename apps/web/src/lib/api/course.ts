import api from './index';
import type { CommonResponse, PageResponse, Course, CourseSearchCondition, CourseSeatHistory } from '@/types/api';

export const searchCourses = async (
  condition: CourseSearchCondition
): Promise<CommonResponse<Course[]>> => {
  const { data } = await api.get('/api/v1/courses', {
    params: { ...condition },
  });
  return data;
};

export const getCourseHistory = async (courseKey: string): Promise<CommonResponse<CourseSeatHistory[]>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/history`);
  return data;
};

export const getCourseDetail = async (courseKey: string): Promise<CommonResponse<Course>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}`);
  return data;
};
