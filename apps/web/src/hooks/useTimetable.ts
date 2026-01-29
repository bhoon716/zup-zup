import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { timetableApi } from '@/lib/api/timetable';
import { toast } from 'sonner';
import { AxiosError } from 'axios';

export const useTimetables = () => {
  return useQuery({
    queryKey: ['timetables'],
    queryFn: async () => {
      const response = await timetableApi.getTimetables();
      return response.data ?? null;
    },
  });
};

export const useTimetableDetail = (id: number | null) => {
  return useQuery({
    queryKey: ['timetable', id],
    queryFn: async () => {
      if (!id) return null;
      const response = await timetableApi.getTimetable(id);
      return response.data ?? null;
    },
    enabled: !!id,
  });
};

export const usePrimaryTimetable = () => {
  return useQuery({
    queryKey: ['timetable', 'primary'],
    queryFn: async () => {
      const response = await timetableApi.getPrimaryTimetable();
      return response.data ?? null;
    },
  });
};

export const useAddCourseToTimetable = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, courseKey }: { timetableId: number; courseKey: string }) => 
      timetableApi.addCourse(timetableId, courseKey),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['timetable', variables.timetableId] });
      queryClient.invalidateQueries({ queryKey: ['timetable', 'primary'] });
      toast.success('시간표에 강의가 추가되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '강의 추가에 실패했습니다.';
      toast.error(message);
    },
  });
};

export const useRemoveCourseFromTimetable = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ timetableId, courseKey }: { timetableId: number; courseKey: string }) => 
      timetableApi.removeCourse(timetableId, courseKey),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['timetable', variables.timetableId] });
      queryClient.invalidateQueries({ queryKey: ['timetable', 'primary'] });
      toast.success('시간표에서 강의가 삭제되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '강의 삭제에 실패했습니다.';
      toast.error(message);
    },
  });
};
