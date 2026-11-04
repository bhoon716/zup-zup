import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as scheduleApi from '@/features/schedule/api/schedule.api';

export const useUpcomingSchedules = (enabled = true) => useQuery({
  queryKey: ['schedules', 'upcoming'],
  queryFn: scheduleApi.getUpcomingSchedules,
  staleTime: 5 * 60 * 1000,
  enabled,
});

export const useScheduleDdays = (enabled = true) => useQuery({
  queryKey: ['schedules', 'ddays'],
  queryFn: scheduleApi.getDdaySchedules,
  staleTime: 5 * 60 * 1000,
  enabled,
});

export const useAdminSchedules = () => useQuery({
  queryKey: ['schedules', 'admin'],
  queryFn: scheduleApi.getAdminSchedules,
});

function useScheduleInvalidation() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ['schedules'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard', 'snapshot'] });
  };
}

export const useCreateSchedule = () => {
  const onSuccess = useScheduleInvalidation();
  return useMutation({ mutationFn: scheduleApi.createSchedule, onSuccess });
};

export const useUpdateSchedule = () => {
  const onSuccess = useScheduleInvalidation();
  return useMutation({ mutationFn: scheduleApi.updateSchedule, onSuccess });
};

export const useDeleteSchedule = () => {
  const onSuccess = useScheduleInvalidation();
  return useMutation({ mutationFn: scheduleApi.deleteSchedule, onSuccess });
};

export const useRestoreSchedule = () => {
  const onSuccess = useScheduleInvalidation();
  return useMutation({ mutationFn: scheduleApi.restoreSchedule, onSuccess });
};
