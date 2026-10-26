import api from "@/shared/api/client";


export interface HealthStatus {
  status: string;
  version: string;
  buildTime: string;
  timestamp: string;
}

export const checkHealth = async (): Promise<HealthStatus> => {
  const { data } = await api.get('/health');
  return data;
};
