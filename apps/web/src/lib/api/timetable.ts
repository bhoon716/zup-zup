import api from './index';
import { 
  CommonResponse, 
  TimetableResponse, 
  TimetableDetailResponse,
  TimetableListResponse, 
  TimetableRequest, 
  CustomScheduleRequest 
} from '@/types/api';

const BASE_URL = '/api/v1/timetables';

export const timetableApi = {
  getTimetables: async () => {
    const { data } = await api.get<CommonResponse<TimetableListResponse[]>>(BASE_URL);
    return data;
  },

  getTimetable: async (id: number) => {
    const { data } = await api.get<CommonResponse<TimetableDetailResponse>>(`${BASE_URL}/${id}`);
    return data;
  },

  getPrimaryTimetable: async () => {
    const { data } = await api.get<CommonResponse<TimetableDetailResponse>>(`${BASE_URL}/primary`);
    return data;
  },

  createTimetable: async (data: TimetableRequest) => {
    const { data: responseData } = await api.post<CommonResponse<TimetableResponse>>(BASE_URL, data);
    return responseData;
  },

  setPrimary: async (id: number) => {
    const { data } = await api.patch<CommonResponse<void>>(`${BASE_URL}/${id}/primary`);
    return data;
  },

  deleteTimetable: async (id: number) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${id}`);
    return data;
  },

  addCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.post<CommonResponse<void>>(`${BASE_URL}/${timetableId}/courses`, { courseKey });
    return data;
  },

  removeCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${timetableId}/courses/${courseKey}`);
    return data;
  },

  addCustomSchedule: async (timetableId: number, data: CustomScheduleRequest) => {
    const { data: responseData } = await api.post<CommonResponse<void>>(`${BASE_URL}/${timetableId}/custom-schedules`, data);
    return responseData;
  },

  removeCustomSchedule: async (timetableId: number, scheduleId: number) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${timetableId}/custom-schedules/${scheduleId}`);
    return data;
  },
};
