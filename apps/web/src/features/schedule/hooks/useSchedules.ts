import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as scheduleApi from '@/features/schedule/api/schedule.api';
import type { ScheduleResponse } from '@/shared/types/api';
import type { PublicPageData } from '@/shared/lib/public-metadata';

const SCHEDULE_QUERY_KEY = ['schedules'] as const;
const PUBLIC_SCHEDULE_STALE_TIME_MS = 5 * 60 * 1000;

export const useUpcomingSchedules = (enabled = true, initialData?: PublicPageData<ScheduleResponse[]> | null) => useQuery({
  queryKey: [...SCHEDULE_QUERY_KEY, 'upcoming'],
  queryFn: scheduleApi.getUpcomingSchedules,
  staleTime: PUBLIC_SCHEDULE_STALE_TIME_MS,
  initialData: initialData?.data,
  initialDataUpdatedAt: initialData?.updatedAt,
  enabled,
});

export const useScheduleDdays = (enabled = true, initialData?: PublicPageData<ScheduleResponse[]> | null) => useQuery({
  queryKey: [...SCHEDULE_QUERY_KEY, 'ddays'],
  queryFn: scheduleApi.getDdaySchedules,
  staleTime: PUBLIC_SCHEDULE_STALE_TIME_MS,
  initialData: initialData?.data,
  initialDataUpdatedAt: initialData?.updatedAt,
  enabled,
});

export const useAdminSchedules = () => useQuery({
  queryKey: [...SCHEDULE_QUERY_KEY, 'admin'],
  queryFn: scheduleApi.getAdminSchedules,
});

function useScheduleInvalidation() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: SCHEDULE_QUERY_KEY });
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
