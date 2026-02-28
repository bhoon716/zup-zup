import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from '@/shared/api/client';
import type { ScheduleRequest, ScheduleResponse } from '@/shared/types/api';

/**
 * 모든 일정 목록을 조회하는 훅 (관리자용)
 */
export const useAdminSchedules = () => {
  return useQuery({
    queryKey: ['adminSchedules'],
    queryFn: async () => {
      const response = await api.get<ScheduleResponse[]>('/api/v1/admin/schedules');
      return response.data;
    },
  });
};

/**
 * 신규 일정을 생성하는 훅
 */
export const useCreateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: ScheduleRequest) => {
      const response = await api.post<ScheduleResponse>('/api/v1/admin/schedules', request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminSchedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', 'upcoming'] });
    },
  });
};

/**
 * 기존 일정을 수정하는 훅
 */
export const useUpdateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, request }: { id: number; request: ScheduleRequest }) => {
      const response = await api.put<ScheduleResponse>(`/api/v1/admin/schedules/${id}`, request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminSchedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', 'upcoming'] });
    },
  });
};

/**
 * 특정 일정을 삭제하는 훅
 */
export const useDeleteSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/v1/admin/schedules/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminSchedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', 'upcoming'] });
    },
  });
};
