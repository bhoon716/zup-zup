import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as userApi from '@/lib/api/user';
import { useAuthStore } from '@/store/useAuthStore';
import { toast } from 'sonner';

import { AxiosError } from 'axios';

export const useUser = () => {
  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: async () => {
      try {
        const response = await userApi.getMyProfile();
        return response.data;
      } catch (error: any) {
        if (error.response?.status === 401) {
          return null; // Guest user
        }
        throw error;
      }
    },
    retry: false,
    staleTime: 1000 * 60 * 5, // 5분간 캐시 유지
  });
};

export const useLogout = () => {
  const logout = useAuthStore((state) => state.logout);

  return useMutation({
    mutationFn: userApi.logout,
    onSuccess: () => {
      logout();
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '로그아웃에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useWithdraw = () => {
  const logout = useAuthStore((state) => state.logout);

  return useMutation({
    mutationFn: userApi.withdraw,
    onSuccess: (response) => {
      toast.success(response.message || '회원 탈퇴가 완료되었습니다');
      logout();
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '회원 탈퇴에 실패했습니다';
      toast.error(message);
    },
  });
};
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: Parameters<typeof userApi.updateProfile>[0]) => userApi.updateProfile(request),
    onSuccess: (response) => {
      queryClient.setQueryData(['user', 'me'], response.data);
      toast.success(response.message || '프로필이 수정되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '프로필 수정에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useCompleteOnboarding = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: Parameters<typeof userApi.completeOnboarding>[0]) => userApi.completeOnboarding(request),
    onSuccess: (response) => {
      queryClient.setQueryData(['user', 'me'], response.data);
      toast.success(response.message || '설정이 완료되었습니다');
    },
    onError: (error: AxiosError<{ message: string }, any>) => {
      const message = error.response?.data?.message || '설정 저장에 실패했습니다';
      toast.error(message);
    },
  });
};
