import api from "@/shared/api/client";
import { TimetableListResponse, TimetableDetailResponse, TimetableRequest, CustomScheduleRequest } from '@/shared/types/api';

const BASE_URL = '/api/v1/timetables';

export const timetableApi = {
  getTimetables: async () => {
    const { data } = await api.get<TimetableListResponse[]>(BASE_URL);
    return data;
  },

  getTimetable: async (id: number) => {
    const { data } = await api.get<TimetableDetailResponse>(`${BASE_URL}/${id}`);
    return data;
  },

  getPrimaryTimetable: async () => {
    const { data } = await api.get<TimetableDetailResponse | null | "">(`${BASE_URL}/primary`);
    return data || null;
  },

  createTimetable: async (data: TimetableRequest) => {
    const payload = {
      name: data.name,
      isPrimary: data.isPrimary ?? data.primary ?? false,
    };
    const { data: responseData } = await api.post<TimetableListResponse>(BASE_URL, payload);
    return responseData;
  },

  setPrimary: async (id: number) => {
    const { data } = await api.patch<void>(`${BASE_URL}/${id}/primary`);
    return data;
  },

  deleteTimetable: async (id: number) => {
    const { data } = await api.delete<void>(`${BASE_URL}/${id}`);
    return data;
  },

  addCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.post<void>(`${BASE_URL}/${timetableId}/courses`, { courseKey });
    return data;
  },

  removeCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.delete<void>(`${BASE_URL}/${timetableId}/courses/${encodeURIComponent(courseKey)}`);
    return data;
  },

  addCustomSchedule: async (timetableId: number, data: CustomScheduleRequest) => {
    const { data: responseData } = await api.post<void>(`${BASE_URL}/${timetableId}/custom-schedules`, data);
    return responseData;
  },

  removeCustomSchedule: async (timetableId: number, scheduleId: number) => {
    const { data } = await api.delete<void>(`${BASE_URL}/${timetableId}/custom-schedules/${scheduleId}`);
    return data;
  },
};
