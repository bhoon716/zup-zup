import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as adminApi from '@/features/admin/api/admin.api';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import type { AdminCrawlTargetRequest } from '@/shared/types/api';




export const useCrawlCourses = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => adminApi.crawlCourses(),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard-snapshot'] });
      toast.success(response || '크롤링이 성공적으로 시작되었습니다.');
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
    onSuccess: () => {
      toast.success('테스트 알림이 성공적으로 전송되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '알림 전송 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};




export const useAdminCrawlTarget = () => {
  return useQuery({
    queryKey: ['admin', 'crawl-target'],
    queryFn: async () => {
      const response = await adminApi.getCrawlTarget();
      return response;
    },
  });
};




export const useUpdateAdminCrawlTarget = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AdminCrawlTargetRequest) => adminApi.updateCrawlTarget(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard-snapshot'] });
      queryClient.invalidateQueries({ queryKey: ['courses', 'search-default-semester'] });
      toast.success('기본 크롤링 타겟을 저장했습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '기본 크롤링 타겟 저장 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};




export const useCrawlCoursesByTarget = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AdminCrawlTargetRequest) => adminApi.crawlCoursesByTarget(request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'dashboard-snapshot'] });
      toast.success(response || '특정 타겟 크롤링이 시작되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '특정 타겟 크롤링 요청 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};
