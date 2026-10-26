import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as scheduleApi from '@/features/schedule/api/schedule.api';

/**
 * 모든 일정 목록을 조회하는 훅 (관리자용)
 */
export const useAdminSchedules = () => {
  return useQuery({
    queryKey: ['adminSchedules'],
    queryFn: scheduleApi.getAdminSchedules,
  });
};

/**
 * 신규 일정을 생성하는 훅
 */
export const useCreateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: scheduleApi.createSchedule,
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
    mutationFn: scheduleApi.updateSchedule,
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
    mutationFn: scheduleApi.deleteSchedule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminSchedules'] });
      queryClient.invalidateQueries({ queryKey: ['schedules', 'upcoming'] });
    },
  });
};
