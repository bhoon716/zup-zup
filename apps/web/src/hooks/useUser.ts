import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as userApi from '@/lib/api/user';
import { useAuthStore } from '@/store/useAuthStore';
import { toast } from 'sonner';

export const useUser = () => {
  const setUser = useAuthStore((state) => state.setUser);

  return useQuery({
    queryKey: ['user', 'me'],
    queryFn: async () => {
      const response = await userApi.getMyProfile();
      return response.data;
    },
    // useUser should refer to the already fetched user if possible
    // but here it acts as a data source
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
    onError: (error: any) => {
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
    onError: (error: any) => {
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
      queryClient.invalidateQueries({ queryKey: ['user', 'me'] });
      toast.success(response.message || '프로필이 수정되었습니다');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || '프로필 수정에 실패했습니다';
      toast.error(message);
    },
  });
};
