import api from "@/shared/api/client";
import type { Subscription, SubscriptionRequest } from '@/shared/types/api';

export const getMySubscriptions = async (): Promise<Subscription[]> => {
  const { data } = await api.get('/api/v1/subscriptions');
  return data;
};

export const subscribe = async (request: SubscriptionRequest): Promise<Subscription> => {
  const { data } = await api.post('/api/v1/subscriptions', request);
  return data;
};

export const unsubscribe = async (id: number): Promise<void> => {
  const { data } = await api.delete(`/api/v1/subscriptions/${id}`);
  return data;
};

export const unsubscribeAll = async (): Promise<void> => {
  const { data } = await api.delete('/api/v1/subscriptions');
  return data;
};
