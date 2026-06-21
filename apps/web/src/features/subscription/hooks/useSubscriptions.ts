import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as subscriptionApi from '@/features/subscription/api/subscription.api';
import { toast } from 'sonner';
import type { SubscriptionRequest } from '@/shared/types/api';
import { AxiosError } from 'axios';

import { useUser } from "@/features/user/hooks/useUser";
import type { User } from "@/shared/types/api";

export const useSubscriptions = (enabled = true, initialUser?: User | null) => {
  const { data: user } = useUser({ enabled: enabled && initialUser === undefined });
  const resolvedUser = initialUser !== undefined ? initialUser : user;
  
  return useQuery({
    queryKey: ['subscriptions'],
    queryFn: async () => {
      const response = await subscriptionApi.getMySubscriptions();
      return response.data ?? null;
    },
    enabled: enabled && !!resolvedUser,
  });
};

export const useSubscribe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: SubscriptionRequest) => subscriptionApi.subscribe(request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
      toast.success(response.message || '구독이 완료되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '구독 신청에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useUnsubscribe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => subscriptionApi.unsubscribe(id),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
      toast.success(response.message || '구독이 취소되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '구독 취소에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useUnsubscribeAll = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => subscriptionApi.unsubscribeAll(),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
      toast.success(response.message || '모든 구독이 취소되었습니다');
    },
    onError: (error: AxiosError<{ message: string }>) => {
      const message = error.response?.data?.message || '모든 구독 취소에 실패했습니다';
      toast.error(message);
    },
  });
};
