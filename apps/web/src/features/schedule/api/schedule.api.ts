import api from '@/shared/api/client';
import type { CommonResponse, ScheduleRequest, ScheduleResponse } from '@/shared/types/api';

export const getUpcomingSchedules = async () => {
  const response = await api.get<CommonResponse<ScheduleResponse[]>>('/api/v1/schedules');
  return response.data.data;
};

export const getAdminSchedules = async () => {
  const response = await api.get<CommonResponse<ScheduleResponse[]>>('/api/v1/admin/schedules');
  return response.data.data;
};

export const createSchedule = async (request: ScheduleRequest) => {
  const response = await api.post<CommonResponse<ScheduleResponse>>('/api/v1/admin/schedules', request);
  return response.data.data;
};

export const updateSchedule = async ({ id, request }: { id: number; request: ScheduleRequest }) => {
  const response = await api.put<CommonResponse<ScheduleResponse>>(`/api/v1/admin/schedules/${id}`, request);
  return response.data.data;
};

export const deleteSchedule = async (id: number) => {
  await api.delete(`/api/v1/admin/schedules/${id}`);
};
