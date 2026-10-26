import api from "@/shared/api/client";
import type { AxiosRequestConfig } from "axios";
import type { User, UserDeviceRequest, UserUpdateRequest, UserSettingsRequest, OnboardingRequest, EmailRequest, EmailVerificationRequest, UserDeviceResponse } from '@/shared/types/api';





export const completeOnboarding = async (request: OnboardingRequest): Promise<User> => {
  const { data } = await api.post('/api/v1/users/onboard', request);
  return data;
};




export const sendVerificationCode = async (request: EmailRequest): Promise<void> => {
  const { data } = await api.post('/api/v1/users/email/code', request);
  return data;
};




export const verifyEmail = async (request: EmailVerificationRequest): Promise<void> => {
  const { data } = await api.post('/api/v1/users/email/verify', request);
  return data;
};

interface MyProfileRequestOptions {
  silentAuthFailure?: boolean;
}

type ProfileRequestMode = 'default' | 'silent';

const profilePromises: Record<ProfileRequestMode, Promise<User> | null> = {
  default: null,
  silent: null,
};

export const clearMyProfileRequestCache = () => {
  profilePromises.default = null;
  profilePromises.silent = null;
};




export const getMyProfile = async (
  options: MyProfileRequestOptions = {},
): Promise<User> => {
  const requestMode: ProfileRequestMode = options.silentAuthFailure ? 'silent' : 'default';
  const activeRequest = profilePromises[requestMode];
  if (activeRequest) return activeRequest;

  const requestPromise = (async () => {
    try {
      const { data } = options.silentAuthFailure
        ? await api.get('/api/v1/users/me', {
            silentAuthFailure: true,
          } as AxiosRequestConfig & { silentAuthFailure: boolean })
        : await api.get('/api/v1/users/me');
      return data;
    } finally {

      setTimeout(() => {
        if (profilePromises[requestMode] === requestPromise) {
          profilePromises[requestMode] = null;
        }
      }, 100);
    }
  })();

  profilePromises[requestMode] = requestPromise;
  return requestPromise;
};




export const logout = async (): Promise<void> => {
  const { data } = await api.post('/api/auth/logout');
  return data;
};




export const withdraw = async (): Promise<void> => {
  const { data } = await api.delete('/api/v1/users/me');
  return data;
};




export const getDevices = async (): Promise<UserDeviceResponse[]> => {
  const { data } = await api.get('/api/v1/users/devices');
  return data;
};




export const registerDevice = async (request: UserDeviceRequest): Promise<void> => {
  const { data } = await api.post('/api/v1/users/devices', request);
  return data;
};




export const deleteDevice = async (id: number): Promise<void> => {
  const { data } = await api.delete(`/api/v1/users/devices/${id}`);
  return data;
};





export const unregisterDevice = async (token: string): Promise<void> => {
  const { data } = await api.delete('/api/v1/users/devices', { data: { token } });
  return data;
};




export const updateProfile = async (request: UserUpdateRequest): Promise<User> => {
  const { data } = await api.patch('/api/v1/users/me', request);
  return data;
};




export const updateSettings = async (request: UserSettingsRequest): Promise<User> => {
  const { data } = await api.patch('/api/v1/users/settings', request);
  return data;
};




export const unlinkDiscord = async (): Promise<void> => {
  const { data } = await api.delete('/api/v1/users/me/discord');
  return data;
};




export const sendTestNotification = async (): Promise<void> => {
  const { data } = await api.post('/api/v1/notifications/test');
  return data;
};
