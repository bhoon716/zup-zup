import { AxiosError, type AxiosRequestConfig } from 'axios';
import api from '@/shared/api/client';
import type { ScheduleRequest, ScheduleResponse } from '@/shared/types/api';

export const getUpcomingSchedules = async () => {
  const response = await api.get<ScheduleResponse[]>('/api/v1/schedules');
  return response.data;
};

export const getAdminSchedules = async () => {
  const response = await api.get<ScheduleResponse[]>('/api/v1/admin/schedules');
  return response.data;
};

export const createSchedule = async (request: ScheduleRequest) => {
  const response = await api.post<ScheduleResponse>('/api/v1/admin/schedules', request);
  return response.data;
};

export const updateSchedule = async ({ id, request }: { id: number; request: ScheduleRequest }) => {
  const response = await api.put<ScheduleResponse>(`/api/v1/admin/schedules/${id}`, request);
  return response.data;
};

export const deleteSchedule = async (id: number) => {
  await api.delete(`/api/v1/admin/schedules/${id}`);
};

export const getDdaySchedules = async () => {
  try {
    const config = { skipAuthRefresh: true } as AxiosRequestConfig & { skipAuthRefresh: boolean };
    const response = await api.get<ScheduleResponse[]>('/api/v1/schedules/ddays', config);
    return response.data;
  } catch (error) {
    if ((error as AxiosError).response?.status === 401) return [];
    throw error;
  }
};

export const restoreSchedule = async (id: number) => {
  const response = await api.patch<ScheduleResponse>(`/api/v1/admin/schedules/${id}/restore`);
  return response.data;
};
