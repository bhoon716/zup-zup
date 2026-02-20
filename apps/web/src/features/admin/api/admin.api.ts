import api from "@/shared/api/client";
import type { CommonResponse, AdminDashboardResponse, AdminOverviewResponse } from '@/shared/types/api';

export const getDashboardStats = async (): Promise<CommonResponse<AdminDashboardResponse>> => {
  const { data } = await api.get('/api/v1/admin/stats');
  return data;
};

export const getDashboardOverview = async (): Promise<CommonResponse<AdminOverviewResponse>> => {
  const { data } = await api.get('/api/v1/admin/overview');
  return data;
};

export const crawlCourses = async (): Promise<CommonResponse<string>> => {
  const { data } = await api.post('/api/v1/admin/courses/crawl');
  return data;
};

export const sendTestNotification = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/admin/notifications/test');
  return data;
};
