import api from "@/shared/api/client";
import type { CommonResponse, AdminDashboardResponse } from '@/shared/types/api';

export const getDashboardStats = async (): Promise<CommonResponse<AdminDashboardResponse>> => {
  const { data } = await api.get('/api/v1/admin/stats');
  return data;
};

export const crawlCourses = async (): Promise<CommonResponse<string>> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl');
  return data;
};

export const sendTestNotification = async (request: { email: string; channels: string[] }): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/admin/notifications/test', request);
  return data;
};
