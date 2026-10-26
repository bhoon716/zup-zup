import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as scheduleApi from '@/features/schedule/api/schedule.api';

/**
 * 예정된 일정 목록을 조회하는 훅 (유저용 - 비로그인 유저도 허용)
 */
export const useUpcomingSchedules = (enabled = true) => {
  return useQuery({
    queryKey: ['schedules', 'upcoming'],
    queryFn: scheduleApi.getUpcomingSchedules,
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
    queryFn: scheduleApi.getAdminSchedules,
    staleTime: 1 * 60 * 1000,
  });
};

/**
 * 신규 일정 생성 어드민 훅
 */
export const useCreateSchedule = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: scheduleApi.createSchedule,
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
    mutationFn: scheduleApi.updateSchedule,
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
    mutationFn: scheduleApi.deleteSchedule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedules'] });
    },
  });
};
