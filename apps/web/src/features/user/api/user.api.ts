import api from "@/shared/api/client";
import type { CommonResponse, User, UserDeviceRequest, UserUpdateRequest, UserSettingsRequest, OnboardingRequest, EmailRequest, EmailVerificationRequest, UserDeviceResponse } from '@/shared/types/api';


/**
 * 신규 가입 사용자의 온보딩 설정을 저장합니다.
 */
export const completeOnboarding = async (request: OnboardingRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.post('/api/v1/users/onboard', request);
  return data;
};

/**
 * 변경할 이메일로 인증 코드를 전송합니다.
 */
export const sendVerificationCode = async (request: EmailRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/email/code', request);
  return data;
};

/**
 * 이메일 인증 코드를 검증합니다.
 */
export const verifyEmail = async (request: EmailVerificationRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/email/verify', request);
  return data;
};

let profilePromise: Promise<CommonResponse<User>> | null = null;

/**
 * 현재 로그인한 사용자의 프로필 정보를 조회합니다. (중복 호출 방지 로직 포함)
 */
export const getMyProfile = async (): Promise<CommonResponse<User>> => {
  if (profilePromise) return profilePromise;

  profilePromise = (async () => {
    try {
      const { data } = await api.get('/api/v1/users/me');
      return data;
    } finally {
      // 짧은 간격의 중복 호출만 합치고 곧바로 해제한다.
      setTimeout(() => { profilePromise = null; }, 100); 
    }
  })();

  return profilePromise;
};

/**
 * 로그아웃을 처리합니다.
 */
export const logout = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/auth/logout');
  return data;
};

/**
 * 모든 정보를 삭제하고 회원 탈퇴를 처리합니다.
 */
export const withdraw = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.delete('/api/v1/users/me');
  return data;
};

/**
 * 등록된 알림 수신 기기 목록을 조회합니다.
 */
export const getDevices = async (): Promise<CommonResponse<UserDeviceResponse[]>> => {
  const { data } = await api.get('/api/v1/users/devices');
  return data;
};

/**
 * 새로운 알림 수신 기기를 등록합니다.
 */
export const registerDevice = async (request: UserDeviceRequest): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/users/devices', request);
  return data;
};

/**
 * 특정 ID의 알림 수신 기기를 삭제합니다.
 */
export const deleteDevice = async (id: number): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/users/devices/${id}`);
  return data;
};

/**
 * 사용 중단 예정입니다. 토큰 기반 삭제 대신 숫자 식별자 기반 삭제 함수를 사용하세요.
 */
export const unregisterDevice = async (token: string): Promise<CommonResponse<void>> => {
  const { data } = await api.delete(`/api/v1/users/devices/token/${encodeURIComponent(token)}`);
  return data;
};

/**
 * 사용자의 이름을 수정합니다.
 */
export const updateProfile = async (request: UserUpdateRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.patch('/api/v1/users/me', request);
  return data;
};

/**
 * 사용자의 알림 수신 설정을 수정합니다.
 */
export const updateSettings = async (request: UserSettingsRequest): Promise<CommonResponse<User>> => {
  const { data } = await api.patch('/api/v1/users/settings', request);
  return data;
};

/**
 * 연동된 디스코드 계정을 해제합니다.
 */
export const unlinkDiscord = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.delete('/api/v1/users/me/discord');
  return data;
};

/**
 * 현재 설정된 채널로 테스트 알림을 발송합니다.
 */
export const sendTestNotification = async (): Promise<CommonResponse<void>> => {
  const { data } = await api.post('/api/v1/notifications/test');
  return data;
};
