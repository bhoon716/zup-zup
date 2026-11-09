import { useEffect } from 'react';
import { AxiosError, isCancel } from 'axios';
import { isUnauthenticated } from '@/shared/api/auth-error';
import { reportClientError } from '@/shared/telemetry/client-error';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as userApi from '@/features/user/api/user.api';
import { useAuthStore } from '@/features/auth/store/useAuthStore';
import { toast } from 'sonner';
import { useRouter } from 'next/navigation';

import { trackOnboardingCompleted, trackOnboardingFailed } from '@/shared/analytics/onboarding-events';

export const useUser = (options?: { enabled?: boolean }) => {
  const enabled = options?.enabled ?? true;
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);
  const refreshUser = useAuthStore((state) => state.refreshUser);

  useEffect(() => {
    if (!enabled || Date.now() - useAuthStore.getState().lastProfileCheckAt < 5 * 60 * 1000) return;
    void refreshUser().catch((error: unknown) => {
      if (!isCancel(error) && !isUnauthenticated(error)) {
        reportClientError(error, { source: 'auth', operation: 'fetch-profile' });
      }
    });
  }, [enabled, refreshUser]);

  return { data: isAuthenticated ? user : null, isLoading: enabled && !hasCheckedSession };
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
  return useMutation({
    onMutate: () => useAuthStore.getState().sessionVersion,
    mutationFn: (request: Parameters<typeof userApi.updateProfile>[0]) => userApi.updateProfile(request),

    onSuccess: (response, _request, version) => {
      if (version !== useAuthStore.getState().sessionVersion) return;
      const user = response ?? null;
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
  return useMutation({
    onMutate: () => useAuthStore.getState().sessionVersion,
    mutationFn: async (request: Parameters<typeof userApi.completeOnboarding>[0]) => {
      try {
        const response = await userApi.completeOnboarding(request);
        trackOnboardingCompleted(accountId);
        return response;
      } catch (error) {
        trackOnboardingFailed(accountId, error);
        throw error;
      }
    },

    onSuccess: (response, _request, version) => {
      if (version !== useAuthStore.getState().sessionVersion) return;
      const user = response ?? null;
      useAuthStore.getState().setUser(user);
      toast.success('설정이 완료되었습니다');
    },

    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '설정 저장에 실패했습니다';
      toast.error(message);
    },
  });
};
