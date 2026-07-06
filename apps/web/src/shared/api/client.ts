import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { redirectToLogin } from "@/shared/lib/navigation";

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
}

interface FailedQueueItem {
  resolve: (value?: unknown) => void;
  reject: (error: unknown) => void;
}

let isRefreshing = false;
let failedQueue: FailedQueueItem[] = [];
type AuthFailureHandler = () => void;

let authFailureHandler: AuthFailureHandler | null = null;

export const registerAuthFailureHandler = (handler: AuthFailureHandler | null) => {
  authFailureHandler = handler;
};

/**
 * 대기 중인 실패 요청들을 순차적으로 처리하거나 거절합니다.
 */
const processQueue = (error: unknown) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
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
      if (originalRequest.url === "/api/auth/refresh") {
        isRefreshing = false;
        processQueue(error);
        // 리프레시 실패 시 로그아웃 처리를 위해 상태 초기화가 필요할 수도 있음 (여기서는 단순히 거절)
      }
      return Promise.reject(error);
    }

    if (error.response?.status === 401) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => api(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await api.post("/api/auth/refresh");
        isRefreshing = false;
        processQueue(undefined);
        return api(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        processQueue(refreshError);
        authFailureHandler?.();
        redirectToLogin();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
