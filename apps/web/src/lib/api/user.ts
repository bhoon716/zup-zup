import api from './index';
import type { CommonResponse, User } from '@/types/api';

export const getMyProfile = async (): Promise<CommonResponse<User>> => {
  const { data } = await api.get('/api/v1/users/me');
  return data;
};

export const logout = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/auth/logout');
  return data;
};

export const withdraw = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.delete('/api/v1/users/me');
  return data;
};
