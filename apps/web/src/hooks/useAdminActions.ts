import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as adminApi from '@/lib/api/admin';
import { toast } from 'sonner';
import { AxiosError } from 'axios';

export const useCrawlCourses = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminApi.crawlCourses(),
    onSuccess: (response) => {
      // 대시보드 통계 및 마지막 크롤링 시간 갱신을 위해 쿼리 무효화
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      toast.success(response.message || '크롤링이 성공적으로 시작되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '크롤링 요청 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};
