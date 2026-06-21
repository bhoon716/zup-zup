import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AxiosError, type AxiosRequestConfig } from 'axios';
import api from '@/shared/api/client';
import type { DdaySettingRequest, DdaySettingResponse } from '@/shared/types/api';

/**
 * 활성 D-Day 정보를 조회하는 훅 (유저용 - 비로그인도 사용 가능)
 */
export const useActiveDday = (enabled = true) => {
  return useQuery({
    queryKey: ['dday', 'active'],
    queryFn: async () => {
      try {
        const config = { skipAuthRefresh: true } as AxiosRequestConfig & { skipAuthRefresh: boolean };
        const response = await api.get<DdaySettingResponse | null>('/api/v1/ddays/active', config);
        // 204 No Content일 경우 response.data가 비어있을 수 있으므로 null 처리
        return response.status === 204 ? null : response.data;
      } catch (error) {
        const axiosError = error as AxiosError;
        if (axiosError.response?.status === 401) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 5 * 60 * 1000, // 5분
    enabled,
  });
};

/**
 * 모든 D-Day 설정 목록을 조회하는 훅 (관리자용)
 */
export const useAdminDdays = () => {
  return useQuery({
    queryKey: ['adminDdays'],
    queryFn: async () => {
      const response = await api.get<DdaySettingResponse[]>('/api/v1/admin/ddays');
      return response.data;
    },
  });
};

/**
 * 신규 D-Day 설정을 생성하는 훅
 */
export const useCreateDday = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: DdaySettingRequest) => {
      const response = await api.post<DdaySettingResponse>('/api/v1/admin/ddays', request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminDdays'] });
      queryClient.invalidateQueries({ queryKey: ['dday', 'active'] });
    },
  });
};

/**
 * 기존 D-Day 설정을 수정하는 훅
 */
export const useUpdateDday = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ id, request }: { id: number; request: DdaySettingRequest }) => {
      const response = await api.put<DdaySettingResponse>(`/api/v1/admin/ddays/${id}`, request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminDdays'] });
      queryClient.invalidateQueries({ queryKey: ['dday', 'active'] });
    },
  });
};

/**
 * 특정 D-Day 설정을 삭제하는 훅
 */
export const useDeleteDday = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/api/v1/admin/ddays/${id}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminDdays'] });
      queryClient.invalidateQueries({ queryKey: ['dday', 'active'] });
    },
  });
};
