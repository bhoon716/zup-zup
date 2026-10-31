import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as userApi from '@/features/user/api/user.api';
import { useAuthStore } from '@/features/auth/store/useAuthStore';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';

import { AxiosError } from 'axios';
import { trackOnboardingCompleted, trackOnboardingFailed } from '@/shared/analytics/onboarding-events';

export const useUser = (options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: async () => {
      try {
        const response = await userApi.getMyProfile();
        return response ?? null;
      } catch (error: unknown) {
        const axiosError = error as AxiosError<{ message?: string }>;
        if (axiosError.response?.status === 401) {
          return null;
        }
        throw error;
      }
    },
    enabled: options?.enabled ?? true,
    retry: false,
    staleTime: 1000 * 60 * 5,
  });
};

export const useLogout = () => {
  const logout = useAuthStore((state) => state.logout);
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: userApi.logout,
    onSuccess: () => {
      logout();
      queryClient.clear();
      router.replace('/login');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '로그아웃에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useWithdraw = () => {
  const logout = useAuthStore((state) => state.logout);
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: userApi.withdraw,
    onSuccess: () => {
      toast.success('회원 탈퇴가 완료되었습니다');
      logout();
      queryClient.clear();
      router.replace('/login');
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
      const user = response ?? null;
      queryClient.setQueryData(['user', 'me'], user);
      useAuthStore.getState().setUser(user);
      toast.success('프로필이 수정되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '프로필 수정에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useCompleteOnboarding = (accountId?: number) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (request: Parameters<typeof userApi.completeOnboarding>[0]) => {
      try {
        const response = await userApi.completeOnboarding(request);
        // 화면 이탈이나 인증 스토어 갱신에 따른 언마운트에도 실제 저장 결과를 기록한다.
        trackOnboardingCompleted(accountId);
        return response;
      } catch (error) {
        trackOnboardingFailed(accountId, error);
        throw error;
      }
    },
    onSuccess: (response) => {
      const user = response ?? null;
      queryClient.setQueryData(['user', 'me'], user);
      useAuthStore.getState().setUser(user);
      toast.success('설정이 완료되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '설정 저장에 실패했습니다';
      toast.error(message);
    },
  });
};
