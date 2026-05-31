import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/shared/api/client';
import type { ScheduleResponse, ScheduleRequest } from '@/shared/types/api';

/**
 * 예정된 일정 목록을 조회하는 훅 (유저용 - 비로그인 유저도 허용)
 */
export const useUpcomingSchedules = (enabled = true) => {
  return useQuery({
    queryKey: ['schedules', 'upcoming'],
    queryFn: async () => {
      const response = await api.get<ScheduleResponse[]>('/api/v1/schedules');
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5분
    enabled,
  });
};

/**
 * 모든 일정 목록 조회하는 어드민 훅 (만료된 일정 포함)
 */
export const useAdminAllSchedules = () => {
  return useQuery({
    queryKey: ['schedules', 'admin-all'],
    queryFn: async () => {
      const response = await api.get<ScheduleResponse[]>('/api/v1/admin/schedules');
      return response.data;
    },
    staleTime: 1 * 60 * 1000,
  });
};

/**
 * 신규 일정 생성 어드민 훅
 */
export const useCreateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: ScheduleRequest) => {
      const response = await api.post<ScheduleResponse>('/api/v1/admin/schedules', request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
};

/**
 * 기존 일정 수정 어드민 훅
 */
export const useUpdateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, request }: { id: number; request: ScheduleRequest }) => {
      const response = await api.put<ScheduleResponse>(`/api/v1/admin/schedules/${id}`, request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
};

/**
 * 기존 일정 삭제 어드민 훅
 */
export const useDeleteSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/v1/admin/schedules/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
};
