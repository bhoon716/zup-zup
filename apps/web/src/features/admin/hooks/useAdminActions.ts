import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as adminApi from '@/features/admin/api/admin.api';
import { toast } from 'sonner';
import { AxiosError } from 'axios';
import type { AdminCrawlTargetRequest } from '@/shared/types/api';

/**
 * 기본 타겟으로 강의 크롤링을 실행하는 훅입니다.
 */
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

/**
 * 관리자용 테스트 알림을 발송하는 훅입니다.
 */
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

/**
 * 현재 설정된 기본 크롤링 타겟 정보를 조회하는 훅입니다.
 */
export const useAdminCrawlTarget = () => {
  return useQuery({
    queryKey: ['admin', 'crawl-target'],
    queryFn: async () => {
      const response = await adminApi.getCrawlTarget();
      return response.data;
    },
  });
};

/**
 * 기본 크롤링 타겟을 수정하고 저장하는 훅입니다.
 */
export const useUpdateAdminCrawlTarget = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AdminCrawlTargetRequest) => adminApi.updateCrawlTarget(request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'crawl-target'] });
      toast.success(response.message || '기본 크롤링 타겟을 저장했습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '기본 크롤링 타겟 저장 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};

/**
 * 특정 년도와 학기를 대상으로 강의 크롤링을 실행하는 훅입니다.
 */
export const useCrawlCoursesByTarget = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: AdminCrawlTargetRequest) => adminApi.crawlCoursesByTarget(request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'overview'] });
      toast.success(response.message || '특정 타겟 크롤링이 시작되었습니다.');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const errorMessage = error.response?.data?.message || '특정 타겟 크롤링 요청 중 오류가 발생했습니다.';
      toast.error(errorMessage);
    },
  });
};
