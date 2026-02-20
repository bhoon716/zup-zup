import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as adminApi from '@/features/admin/api/admin.api';
import { toast } from 'sonner';
import { AxiosError } from 'axios';

export const useCrawlCourses = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminApi.crawlCourses(),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] });
      toast.success(response.message || '크롤링이 성공적으로 시작되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '크롤링 요청 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};

export const useSendTestNotification = () => {
  return useMutation({
    mutationFn: () => adminApi.sendTestNotification(),
    onSuccess: (response) => {
      toast.success(response.message || '테스트 알림이 성공적으로 전송되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '알림 전송 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};
