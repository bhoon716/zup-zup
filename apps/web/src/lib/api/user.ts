import api from './index';
import type { CommonResponse, User, UserDeviceRequest, UserUpdateRequest, UserSettingsRequest, OnboardingRequest, EmailRequest, EmailVerificationRequest, UserDeviceResponse } from '@/types/api';


export const completeOnboarding = async (request: OnboardingRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.post('/api/v1/users/onboard', request);
  return data;
};

export const sendVerificationCode = async (request: EmailRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/email/code', request);
  return data;
};

export const verifyEmail = async (request: EmailVerificationRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/email/verify', request);
  return data;
};

let profilePromise: Promise<CommonResponse<User>> | null = null;

export const getMyProfile = async (): Promise<CommonResponse<User>> => {
  if (profilePromise) return profilePromise;

  profilePromise = (async () => {
    try {
      const { data } = await api.get('/api/v1/users/me');
      return data;
    } finally {
      // 요청이 완료되면 캐시 초기화 (다음번 호출은 다시 네트워크 요청)
      // 짧은 간격의 동시 호출만 묶어줌
      setTimeout(() => { profilePromise = null; }, 100); 
    }
  })();

  return profilePromise;
};

export const logout = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/auth/logout');
  return data;
};

export const withdraw = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.delete('/api/v1/users/me');
  return data;
};

export const getDevices = async (): Promise<CommonResponse<UserDeviceResponse[]>> => {
  const { data } = await api.get('/api/v1/users/devices');
  return data;
};

export const registerDevice = async (request: UserDeviceRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/devices', request);
  return data;
};

export const deleteDevice = async (id: number): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/users/devices/${id}`);
  return data;
};

/**
 * @deprecated Use deleteDevice(id) instead
 */
export const unregisterDevice = async (token: string): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/users/devices/token/${encodeURIComponent(token)}`);
  return data;
};

export const updateProfile = async (request: UserUpdateRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.patch('/api/v1/users/me', request);
  return data;
};

export const updateSettings = async (request: UserSettingsRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.patch('/api/v1/users/settings', request);
  return data;
};

export const unlinkDiscord = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.delete('/api/v1/users/me/discord');
  return data;
};
