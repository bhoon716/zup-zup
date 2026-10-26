import axios, { AxiosError, CanceledError, type InternalAxiosRequestConfig } from "axios";
import { isUnauthenticated } from "@/shared/api/auth-error";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "",
  timeout: 10000,
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
      if (generation !== sessionGeneration) throw new CanceledError("Session changed before request dispatch");
      const token = response.data?.token;
      if (typeof token !== "string" || token.length === 0) {
        throw new AxiosError("CSRF token is missing", AxiosError.ERR_BAD_RESPONSE, response.config, response.request, response);
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
  if (generation !== sessionGeneration) throw new CanceledError("Session changed before request dispatch", request);
  const method = (request.method || "GET").toUpperCase();
  const pathname = new URL(request.url || "", "http://request.local").pathname;
  if (["GET", "HEAD", "OPTIONS"].includes(method)
    || (method === "POST" && pathname === "/api/v1/courses/search")) return request;

  const token = await loadCsrfToken();
  if (generation !== sessionGeneration) throw new CanceledError("Session changed before request dispatch", request);
  request.headers.set("X-CSRF-TOKEN", token);
  return request;
});

api.interceptors.response.use(
  response => response,
  (error: AxiosError<{ code?: string }>) => {
    const request = error.config as SessionRequestConfig | undefined;
    if (request?.sessionGeneration === sessionGeneration) {
      const unauthorized = isUnauthenticated(error);
      if (unauthorized || (error.response?.status === 403 && error.response.data?.code === "A003")) {
        csrfToken = null;
        csrfPromise = null;
      }
      if (unauthorized && !request.silentAuthFailure && handledFailureGeneration !== sessionGeneration) {
        handledFailureGeneration = sessionGeneration;
        authFailureHandler?.();
      }
    }
    return Promise.reject(error);
  },
);

export default api;
