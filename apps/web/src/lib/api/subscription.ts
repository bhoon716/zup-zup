import api from './index';
import type { CommonResponse, Subscription, SubscriptionRequest } from '@/types/api';

export const getMySubscriptions = async (): Promise<CommonResponse<Subscription[]>> => {
  const { data } = await api.get('/api/v1/subscriptions');
  return data;
};

export const subscribe = async (request: SubscriptionRequest): Promise<CommonResponse<Subscription>> => {
  const { data } = await api.post('/api/v1/subscriptions', request);
  return data;
};

export const unsubscribe = async (id: number): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/subscriptions/${id}`);
  return data;
};

