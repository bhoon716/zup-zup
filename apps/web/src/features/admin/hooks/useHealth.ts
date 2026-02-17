import { useQuery } from '@tanstack/react-query';
import * as healthApi from '@/features/admin/api/health.api';

export const useHealth = () => {
  return useQuery({
    queryKey: ['health'],
    queryFn: async () => {
      const response = await healthApi.checkHealth();
      return response.data;
    },
    refetchInterval: 30000,
  });
};
