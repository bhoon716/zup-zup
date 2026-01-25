import api from './index';
import type { CommonResponse, AdminDashboardResponse } from '@/types/api';

export const getDashboardStats = async (): Promise<CommonResponse<AdminDashboardResponse>> => {
  const { data } = await api.get('/api/v1/admin/stats');
  return data;
};
