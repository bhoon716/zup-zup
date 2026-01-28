import api from './index';
import { 
  CommonResponse, 
  TimetableResponse, 
  TimetableListResponse, 
  TimetableRequest, 
  CustomScheduleRequest 
} from '@/types/api';

const BASE_URL = '/api/v1/timetables';

export const timetableApi = {
  // 시간표 목록 조회
  getTimetables: async () => {
    const { data } = await api.get<CommonResponse<TimetableListResponse[]>>(BASE_URL);
    return data;
  },

  // 시간표 상세 조회
  getTimetable: async (id: number) => {
    const { data } = await api.get<CommonResponse<TimetableResponse>>(`${BASE_URL}/${id}`);
    return data;
  },

  // 대표 시간표 조회
  getPrimaryTimetable: async () => {
    const { data } = await api.get<CommonResponse<TimetableResponse>>(`${BASE_URL}/primary`);
    return data;
  },

  // 시간표 생성
  createTimetable: async (data: TimetableRequest) => {
    const { data: responseData } = await api.post<CommonResponse<TimetableResponse>>(BASE_URL, data);
    return responseData;
  },

  // 대표 시간표 설정
  setPrimary: async (id: number) => {
    const { data } = await api.patch<CommonResponse<void>>(`${BASE_URL}/${id}/primary`);
    return data;
  },

  // 시간표 삭제
  deleteTimetable: async (id: number) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${id}`);
    return data;
  },

  // 시간표에 강좌 추가
  addCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.post<CommonResponse<void>>(`${BASE_URL}/${timetableId}/courses`, { courseKey });
    return data;
  },

  // 시간표에서 강좌 삭제
  removeCourse: async (timetableId: number, courseKey: string) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${timetableId}/courses/${courseKey}`);
    return data;
  },

  // 시간표에 커스텀 일정 추가
  addCustomSchedule: async (timetableId: number, data: CustomScheduleRequest) => {
    const { data: responseData } = await api.post<CommonResponse<void>>(`${BASE_URL}/${timetableId}/custom-schedules`, data);
    return responseData;
  },

  // 시간표에서 커스텀 일정 삭제
  removeCustomSchedule: async (timetableId: number, scheduleId: number) => {
    const { data } = await api.delete<CommonResponse<void>>(`${BASE_URL}/${timetableId}/custom-schedules/${scheduleId}`);
    return data;
  },
};
