import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
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
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      console.error("[API] Unauthorized session. Redirecting to login.");
      // 세션 세팅 과정에서 발생할 수 있는 401 루프를 방지하기 위해 로그인 페이지가 아닐 때만 이동
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);

export default api;
