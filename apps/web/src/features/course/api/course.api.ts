import api from "@/shared/api/client";
import type { Course, CourseSearchCondition, CourseSeatHistory, SliceResponse, SearchDefaultSemesterResponse } from '@/shared/types/api';

export const searchCourses = async (
  condition: CourseSearchCondition,
  page: number = 0,
  size: number = 30
): Promise<SliceResponse<Course>> => {
  const { data } = await api.post('/api/v1/courses/search', condition, {
    params: { page, size },
  });
  return data;
};

export const getCourseHistory = async (courseKey: string, page = 0, size = 30): Promise<SliceResponse<CourseSeatHistory>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/history`, {
    params: { page, size },
  });
  return data;
};

export const getCourseDetail = async (courseKey: string): Promise<Course> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}`);
  return data;
};

export const getSearchDefaultSemester = async (): Promise<SearchDefaultSemesterResponse> => {
  const { data } = await api.get('/api/v1/courses/search-default-semester');
  return data;
};
