import api from './index';
import type { CommonResponse, User, UserDeviceRequest, UserUpdateRequest } from '@/types/api';

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

export const registerDevice = async (request: UserDeviceRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/devices', request);
  return data;
};

export const unregisterDevice = async (token: string): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/users/devices/${encodeURIComponent(token)}`);
  return data;
};

export const updateProfile = async (request: UserUpdateRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.patch('/api/v1/users/me', request);
  return data;
};
