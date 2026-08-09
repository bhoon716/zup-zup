import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { redirectToLogin } from "@/shared/lib/navigation";
import { isDefinitiveAuthFailure } from "@/shared/api/auth-error";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL || "";

const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

/**
 * 액세스 토큰을 수동으로 설정하는 함수 (현재 쿠키 방식 사용 중으로 구조만 유지)
 */
export const setAccessToken = (token: string | null) => {
  // 이전 인터페이스 호환을 위해 함수 형태만 유지한다.
  void token;
};

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  skipAuthRefresh?: boolean;
  silentAuthFailure?: boolean;
}

const TRANSIENT_REFRESH_BACKOFF_MS = [250, 1000, 2000] as const;
const MAX_TRANSIENT_REFRESH_ATTEMPTS = TRANSIENT_REFRESH_BACKOFF_MS.length;
const HALF_OPEN_REFRESH_COOLDOWN_MS = 30000;

interface TransientRefreshFailure {
  generation: number;
  attempt: number;
  retryAt: number;
  error: unknown;
}

let authGeneration = 0;
let transientRefreshFailure: TransientRefreshFailure | null = null;
let refreshPromise: Promise<void> | null = null;
let refreshPromiseGeneration = 0;
let definitiveFailureHandledGeneration: number | null = null;
type AuthFailureHandler = () => void;

let authFailureHandler: AuthFailureHandler | null = null;

export const registerAuthFailureHandler = (handler: AuthFailureHandler | null) => {
  authFailureHandler = handler;
};

export const resetAuthRefreshState = () => {
  authGeneration += 1;
  transientRefreshFailure = null;
};

const getBlockedRefreshError = () => {
  if (!transientRefreshFailure || transientRefreshFailure.generation !== authGeneration) {
    return null;
  }
  if (Date.now() < transientRefreshFailure.retryAt) {
    return transientRefreshFailure.error;
  }
  return null;
};

const recordTransientRefreshFailure = (error: unknown) => {
  const previousAttempt = transientRefreshFailure?.generation === authGeneration
    ? transientRefreshFailure.attempt
    : 0;
  const attempt = previousAttempt + 1;
  const delayIndex = Math.min(attempt - 1, TRANSIENT_REFRESH_BACKOFF_MS.length - 1);
  const retryDelay = attempt > MAX_TRANSIENT_REFRESH_ATTEMPTS
    ? HALF_OPEN_REFRESH_COOLDOWN_MS
    : TRANSIENT_REFRESH_BACKOFF_MS[delayIndex];
  transientRefreshFailure = {
    generation: authGeneration,
    attempt,
    retryAt: Date.now() + retryDelay,
    error,
  };
};

interface RefreshAttempt {
  generation: number;
  promise: Promise<void>;
}

const startRefresh = (): RefreshAttempt => {
  if (refreshPromise) {
    return { generation: refreshPromiseGeneration, promise: refreshPromise };
  }

  const blockedError = getBlockedRefreshError();
  if (blockedError) {
    return { generation: authGeneration, promise: Promise.reject(blockedError) };
  }

  const requestGeneration = authGeneration;
  refreshPromiseGeneration = requestGeneration;
  refreshPromise = api.post("/api/auth/refresh")
    .then(() => {
      if (requestGeneration === authGeneration) {
        transientRefreshFailure = null;
        definitiveFailureHandledGeneration = null;
      }
    })
    .catch((refreshError: unknown) => {
      if (requestGeneration === authGeneration) {
        if (isDefinitiveAuthFailure(refreshError)) {
          transientRefreshFailure = null;
        } else {
          recordTransientRefreshFailure(refreshError);
        }
      }
      throw refreshError;
    })
    .finally(() => {
      refreshPromise = null;
    });
  return { generation: requestGeneration, promise: refreshPromise };
};

const handleDefinitiveAuthFailure = (error: unknown, request: RetryableRequestConfig,
  failureGeneration: number) => {
  if (!isDefinitiveAuthFailure(error) || request.silentAuthFailure
    || failureGeneration !== authGeneration
    || definitiveFailureHandledGeneration === failureGeneration) {
    return;
  }
  definitiveFailureHandledGeneration = failureGeneration;
  authFailureHandler?.();
  redirectToLogin();
};

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;
    if (!originalRequest) {
      return Promise.reject(error);
    }

    // 리프레시 요청 자체가 실패했거나 이미 재시도한 요청인 경우
    if (originalRequest.url === "/api/auth/refresh" || originalRequest._retry || originalRequest.skipAuthRefresh) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401) {
      originalRequest._retry = true;

      const refreshAttempt = startRefresh();
      try {
        await refreshAttempt.promise;
        return api(originalRequest);
      } catch (refreshError) {
        handleDefinitiveAuthFailure(refreshError, originalRequest, refreshAttempt.generation);
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
