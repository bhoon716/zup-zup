import api from './index';
import type { CommonResponse } from '@/types/api';

export interface HealthStatus {
  status: string;
  version: string;
  buildTime: string;
  timestamp: string;
}

export const checkHealth = async (): Promise<CommonResponse<HealthStatus>> => {
  const { data } = await api.get('/health');
  return data;
};
