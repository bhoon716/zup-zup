import { localizedErrorMessage } from "@/shared/lib/error-message";
import { queryOptions, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { timetableApi } from '@/features/timetable/api/timetable.api';
import { toast } from 'sonner';
import { isAxiosError, type AxiosError } from 'axios';
import type { CustomScheduleRequest, User } from '@/shared/types/api';

import { useAuthStore } from '@/features/auth/store/useAuthStore';

export const useTimetables = (enabled = true, initialUser?: User | null) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const canFetch = initialUser !== undefined ? !!initialUser : isAuthenticated;
  return useQuery({
    queryKey: ['timetables'],
    queryFn: async () => {
      const response = await timetableApi.getTimetables();
      return response ?? null;
    },
    enabled: enabled && canFetch,
  });
};

export const timetableDetailOptions = (id: number | null) =>
  queryOptions({
    queryKey: ['timetable', id],
    queryFn: async () => {
      if (!id) return null;
      const response = await timetableApi.getTimetable(id);
      return response ?? null;
    },
    staleTime: 60_000,
  });

export const useTimetableDetail = (id: number | null, enabled = true, initialUser?: User | null) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const canFetch = initialUser !== undefined ? !!initialUser : isAuthenticated;
  return useQuery({
    ...timetableDetailOptions(id),
    enabled: !!id && enabled && canFetch,
  });
};

export const useAddCourseToTimetable = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, courseKey }: { timetableId: number; courseKey: string }) => 
      timetableApi.addCourse(timetableId, courseKey),

    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['timetable', variables.timetableId] });
      toast.success('시간표에 강의가 추가되었습니다.');
    },

    onError: (error: AxiosError<{ message: string }>) => {
      const message = localizedErrorMessage(error.response?.data?.message, '강의 추가에 실패했습니다.');
      toast.error(message);
    },
  });
};

export const useRemoveCourseFromTimetable = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, courseKey }: { timetableId: number; courseKey: string }) =>
      timetableApi.removeCourse(timetableId, courseKey),

    onSuccess: async (_, { timetableId, courseKey }) => {
      const { queryKey } = timetableDetailOptions(timetableId);
      await queryClient.cancelQueries({ queryKey, exact: true });
      queryClient.setQueryData(queryKey, (detail) => {
        if (!detail) return detail;

        const courses = detail.courses.filter((course) => course.courseKey !== courseKey);
        const totalCredits = courses.reduce((sum, course) => {
          const credits = Number(course.credits);
          return sum + (Number.isFinite(credits) ? credits : 0);
        }, 0);
        return { ...detail, courses, totalCredits: String(totalCredits) };
      });
      void queryClient.invalidateQueries({ queryKey, exact: true });
      toast.success('시간표에서 강의가 삭제되었습니다.');
    },

    onError: (error) => {
      const message = isAxiosError<{ message?: unknown }>(error) ? error.response?.data?.message : undefined;
      toast.error(localizedErrorMessage(message, '강의 삭제에 실패했습니다. 다시 시도해 주세요.'));
    },
  });
};

export const useAddCustomSchedule = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, data }: { timetableId: number; data: CustomScheduleRequest }) => 
      timetableApi.addCustomSchedule(timetableId, data),

    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['timetable', variables.timetableId] });
      toast.success('시간표에 일정이 추가되었습니다.');
    },

    onError: (error: AxiosError<{ message: string }>) => {
      const message = localizedErrorMessage(error.response?.data?.message, '일정 추가에 실패했습니다.');
      toast.error(message);
    },
  });
};

export const useRemoveCustomSchedule = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, scheduleId }: { timetableId: number; scheduleId: number }) => 
      timetableApi.removeCustomSchedule(timetableId, scheduleId),

    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['timetable', variables.timetableId] });
      toast.success('시간표에서 일정이 삭제되었습니다.');
    },

    onError: (error: AxiosError<{ message: string }>) => {
      const message = localizedErrorMessage(error.response?.data?.message, '일정 삭제에 실패했습니다.');
      toast.error(message);
    },
  });
};
