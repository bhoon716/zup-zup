import api from './index';
import type { CommonResponse, Course, CourseSearchCondition, CourseSeatHistory, CourseCategoryResponse, SliceResponse } from '@/types/api';

export const searchCourses = async (
  condition: CourseSearchCondition,
  page: number = 0,
  size: number = 30
): Promise<CommonResponse<SliceResponse<Course>>> => {
  const { data } = await api.get('/api/v1/courses', {
    params: { ...condition, page, size },
  });
  return data;
};

export const getCourseHistory = async (courseKey: string): Promise<CommonResponse<CourseSeatHistory[]>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/history`);
  return data;
};


export const getCourseCategories = async (): Promise<CommonResponse<CourseCategoryResponse[]>> => {
  const { data } = await api.get('/api/v1/courses/categories');
  return data;
};

export const getCourseDetail = async (courseKey: string): Promise<CommonResponse<Course>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}`);
  return data;
};
