import { useQuery } from '@tanstack/react-query';
import api from '@/shared/api/client';
import type { ScheduleResponse } from '@/shared/types/api';

/**
 * 예정된 일정 목록을 조회하는 훅 (유저용)
 */
export const useUpcomingSchedules = () => {
  return useQuery({
    queryKey: ['schedules', 'upcoming'],
    queryFn: async () => {
      const response = await api.get<ScheduleResponse[]>('/api/v1/schedules');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5분
  });
};
