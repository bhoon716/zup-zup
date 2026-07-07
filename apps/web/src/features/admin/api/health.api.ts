import api from "@/shared/api/client";
import type { CommonResponse } from '@/shared/types/api';

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
