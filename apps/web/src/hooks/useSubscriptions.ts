import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as subscriptionApi from '@/lib/api/subscription';
import { toast } from 'sonner';
import type { SubscriptionRequest } from '@/types/api';

export const useSubscriptions = () => {
  return useQuery({
    queryKey: ['subscriptions'],
    queryFn: async () => {
      const response = await subscriptionApi.getMySubscriptions();
      return response.data;
    },
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
    onError: (error: any) => {
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
    onError: (error: any) => {
      const message = error.response?.data?.message || '구독 취소에 실패했습니다';
      toast.error(message);
    },
  });
};

export const useToggleSubscription = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => subscriptionApi.toggleSubscription(id),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] });
      toast.success(response.message || '알림 상태가 변경되었습니다');
    },
    onError: (error: any) => {
      const message = error.response?.data?.message || '알림 상태 변경에 실패했습니다';
      toast.error(message);
    },
  });
};
