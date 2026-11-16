import axios, { AxiosError, CanceledError, type InternalAxiosRequestConfig } from "axios";
import { isCsrfFailure, isUnauthenticated } from "@/shared/api/auth-error";

const API_REQUEST_TIMEOUT_MS = 10_000;
const MAX_CSRF_ERROR_BODY_BYTES = 65_536;
const SESSION_CHANGED_MESSAGE = "요청을 보내기 전에 로그인 세션이 변경됐습니다.";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "",
  timeout: API_REQUEST_TIMEOUT_MS,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

interface SessionRequestConfig extends InternalAxiosRequestConfig {
  silentAuthFailure?: boolean;
  sessionGeneration?: number;
}

let sessionGeneration = 0;
let csrfToken: string | null = null;
let csrfPromise: Promise<string> | null = null;
let handledFailureGeneration: number | null = null;
let authFailureHandler: (() => void) | null = null;

export const registerAuthFailureHandler = (handler: (() => void) | null) => {
  authFailureHandler = handler;
};

export const resetSessionState = () => {
  sessionGeneration += 1;
  csrfToken = null;
  csrfPromise = null;
  handledFailureGeneration = null;
};

const loadCsrfToken = () => {
  if (csrfToken) return Promise.resolve(csrfToken);
  if (csrfPromise) return csrfPromise;

  const generation = sessionGeneration;
  const pending = api.get<{ token: string }>("/api/auth/csrf", { sessionGeneration: generation } as SessionRequestConfig)
    .then(response => {
      if (generation !== sessionGeneration) throw new CanceledError(SESSION_CHANGED_MESSAGE);
      const token = response.data?.token;
      if (typeof token !== "string" || token.length === 0) {
        throw new AxiosError("요청 보안 토큰이 누락됐습니다.", AxiosError.ERR_BAD_RESPONSE, response.config, response.request, response);
      }
      if (csrfPromise === pending) csrfToken = token;
      return token;
    })
    .finally(() => {
      if (csrfPromise === pending) csrfPromise = null;
    });
  csrfPromise = pending;
  return pending;
};

api.interceptors.request.use(async config => {
  const request = config as SessionRequestConfig;
  const generation = request.sessionGeneration ?? sessionGeneration;
  request.sessionGeneration = generation;
  if (generation !== sessionGeneration) throw new CanceledError(SESSION_CHANGED_MESSAGE, request);
  const method = (request.method || "GET").toUpperCase();
  const pathname = new URL(request.url || "", "http://request.local").pathname;
  if (["GET", "HEAD", "OPTIONS"].includes(method)
    || (method === "POST" && pathname === "/api/v1/courses/search")) return request;

  const token = await loadCsrfToken();
  if (generation !== sessionGeneration) throw new CanceledError(SESSION_CHANGED_MESSAGE, request);
  request.headers.set("X-CSRF-TOKEN", token);
  return request;
});

api.interceptors.response.use(
  response => response,
  async (error: AxiosError<unknown>) => {
    const request = error.config as SessionRequestConfig | undefined;
    if (!request || request.sessionGeneration !== sessionGeneration) return Promise.reject(error);
    const errorResponse = error.response;
    if (errorResponse?.status === 403 && errorResponse.data instanceof Blob
      && errorResponse.data.type.split(";", 1)[0].trim().toLowerCase() === "application/json"
      && errorResponse.data.size <= MAX_CSRF_ERROR_BODY_BYTES) {
      try {
        errorResponse.data = JSON.parse(await errorResponse.data.text());
      } catch {
        return Promise.reject(error);
      }
    }
    if (request.sessionGeneration !== sessionGeneration) return Promise.reject(error);
    const unauthorized = isUnauthenticated(error);
    if (unauthorized || isCsrfFailure(error)) {
      csrfToken = null;
      csrfPromise = null;
    }
    if (unauthorized && !request.silentAuthFailure && handledFailureGeneration !== sessionGeneration) {
      handledFailureGeneration = sessionGeneration;
      authFailureHandler?.();
    }
    return Promise.reject(error);
  },
);

export default api;
