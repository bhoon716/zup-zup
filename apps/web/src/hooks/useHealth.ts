import { useQuery } from '@tanstack/react-query';
import * as healthApi from '@/lib/api/health';

export const useHealth = () => {
  return useQuery({
    queryKey: ['health'],
    queryFn: async () => {
      const response = await healthApi.checkHealth();
      return response.data;
    },
    refetchInterval: 30000, // Check every 30 seconds
  });
};
