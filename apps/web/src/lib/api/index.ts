import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

// BFF 패턴에서는 브라우저가 직접 토큰을 다루지 않음. 모든 인증은 세션 쿠키로 처리됨.
export const setAccessToken = (_token: string | null) => {
  // 사용하지 않음 (호환성 위해 빈 함수 유지)
};

// Response Interceptor - 401 발생 시 로그아웃 처리만 수행 (갱신은 서버가 담당)
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // 토큰 갱신 요청 자체가 실패한 경우나 이미 재시도 중인 경우 즉시 실패 처리하여 무한 루프 방지
    if (originalRequest.url === "/api/auth/refresh" || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            return api(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        await api.post("/api/auth/refresh");
        console.info("[API] Token refresh successful. Retrying original request.");
        isRefreshing = false;
        processQueue(null);
        return api(originalRequest);
      } catch (refreshError) {
        isRefreshing = false;
        processQueue(refreshError);
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
