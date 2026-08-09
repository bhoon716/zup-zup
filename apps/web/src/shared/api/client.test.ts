import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

describe("shared api client", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("로컬 API URL에서는 ngrok 경고 헤더를 붙이지 않는다", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://localhost:8080");

    const { default: api } = await import("./client");

    expect((api.defaults.headers.common as Record<string, unknown>)["ngrok-skip-browser-warning"]).toBeUndefined();
  });

  it("refresh 실패 시 로그인 페이지로 이동한다", async () => {
    const refreshError = Object.assign(new Error("refresh failed"), {
      response: { status: 401 },
    });
    const redirectSpy = vi.fn();
    const logoutSpy = vi.fn();
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};

    const apiMock = {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async (url: string) => {
        if (url === "/api/auth/refresh") {
          throw refreshError;
        }

        return { data: null };
      }),
    };

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    vi.doMock("@/shared/lib/navigation", () => ({
      redirectToLogin: redirectSpy,
    }));

    const { default: api, registerAuthFailureHandler } = await import("./client");
    registerAuthFailureHandler(logoutSpy);
    void api;

    const responseError = {
      config: { url: "/api/v1/users/me" },
      response: { status: 401 },
    };

    await expect(responseHandlers.onRejected?.(responseError)).rejects.toThrow("refresh failed");
    expect(redirectSpy).toHaveBeenCalledTimes(1);
    expect(logoutSpy).toHaveBeenCalledTimes(1);
  });

  it("silent bootstrap의 definitive refresh 실패는 로그인 이동 없이 guest로 수렴한다", async () => {
    const refreshError = Object.assign(new Error("refresh failed"), {
      response: { status: 401 },
    });
    const redirectSpy = vi.fn();
    const logoutSpy = vi.fn();
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};

    const apiMock = {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async (url: string) => {
        if (url === "/api/auth/refresh") {
          throw refreshError;
        }

        return { data: null };
      }),
    };

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    vi.doMock("@/shared/lib/navigation", () => ({
      redirectToLogin: redirectSpy,
    }));

    const { default: api, registerAuthFailureHandler } = await import("./client");
    registerAuthFailureHandler(logoutSpy);
    void api;

    const responseError = {
      config: { url: "/api/v1/users/me", silentAuthFailure: true },
      response: { status: 401 },
    };

    await expect(responseHandlers.onRejected?.(responseError)).rejects.toThrow("refresh failed");
    expect(redirectSpy).not.toHaveBeenCalled();
    expect(logoutSpy).not.toHaveBeenCalled();
  });

  it("silent bootstrap도 유효한 refresh가 있으면 원래 프로필 요청을 재시도한다", async () => {
    const redirectSpy = vi.fn();
    const logoutSpy = vi.fn();
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};
    const retryResponse = { data: { code: "SUCCESS" } };
    const requestMock = vi.fn(async () => retryResponse);
    const apiMock = Object.assign(requestMock, {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async () => ({ data: null })),
    });

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    vi.doMock("@/shared/lib/navigation", () => ({
      redirectToLogin: redirectSpy,
    }));

    const { default: api, registerAuthFailureHandler } = await import("./client");
    registerAuthFailureHandler(logoutSpy);
    void api;

    const originalRequest = { url: "/api/v1/users/me", silentAuthFailure: true };
    const responseError = {
      config: originalRequest,
      response: { status: 401 },
    };

    await expect(responseHandlers.onRejected?.(responseError)).resolves.toEqual(retryResponse);
    expect(apiMock.post).toHaveBeenCalledWith("/api/auth/refresh");
    expect(requestMock).toHaveBeenCalledWith(expect.objectContaining({
      url: "/api/v1/users/me",
      silentAuthFailure: true,
      _retry: true,
    }));
    expect(redirectSpy).not.toHaveBeenCalled();
    expect(logoutSpy).not.toHaveBeenCalled();
  });

  it("refresh의 일시적인 서버 오류에서는 로그인 상태를 제거하거나 이동하지 않는다", async () => {
    const refreshError = Object.assign(new Error("temporarily unavailable"), {
      response: { status: 503 },
    });
    const redirectSpy = vi.fn();
    const logoutSpy = vi.fn();
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};

    const apiMock = {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async (url: string) => {
        if (url === "/api/auth/refresh") {
          throw refreshError;
        }

        return { data: null };
      }),
    };

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    vi.doMock("@/shared/lib/navigation", () => ({
      redirectToLogin: redirectSpy,
    }));

    const { default: api, registerAuthFailureHandler } = await import("./client");
    registerAuthFailureHandler(logoutSpy);
    void api;

    const responseError = {
      config: { url: "/api/v1/users/me" },
      response: { status: 401 },
    };

    await expect(responseHandlers.onRejected?.(responseError)).rejects.toThrow("temporarily unavailable");
    expect(redirectSpy).not.toHaveBeenCalled();
    expect(logoutSpy).not.toHaveBeenCalled();
  });

  it("엇갈린 보호 요청은 공유 transient cooldown과 refresh 상한을 함께 사용한다", async () => {
    vi.useFakeTimers();
    const refreshError = Object.assign(new Error("temporarily unavailable"), {
      response: { status: 503 },
    });
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};
    let refreshCalls = 0;
    const requestMock = vi.fn(async () => ({ data: null }));
    const apiMock = Object.assign(requestMock, {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async () => {
        refreshCalls += 1;
        throw refreshError;
      }),
    });

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    const { default: api } = await import("./client");
    void api;
    const rejectProtectedRequest = (id: number) => responseHandlers.onRejected?.({
      config: { url: `/api/v1/protected/${id}` },
      response: { status: 401 },
    });

    await expect(rejectProtectedRequest(1)).rejects.toBe(refreshError);
    await expect(rejectProtectedRequest(2)).rejects.toBe(refreshError);
    expect(refreshCalls).toBe(1);

    vi.advanceTimersByTime(250);
    await expect(rejectProtectedRequest(3)).rejects.toBe(refreshError);
    expect(refreshCalls).toBe(2);

    vi.advanceTimersByTime(1000);
    await expect(rejectProtectedRequest(4)).rejects.toBe(refreshError);
    expect(refreshCalls).toBe(3);

    vi.advanceTimersByTime(2000);
    await expect(rejectProtectedRequest(5)).rejects.toBe(refreshError);
    expect(refreshCalls).toBe(3);
  });

  it("refresh 성공과 명시적 인증 상태 초기화는 transient cooldown을 초기화한다", async () => {
    vi.useFakeTimers();
    const transientError = Object.assign(new Error("temporarily unavailable"), {
      response: { status: 503 },
    });
    const definitiveError = Object.assign(new Error("expired"), {
      response: { status: 401 },
    });
    const redirectSpy = vi.fn();
    const responseHandlers: {
      onRejected?: (error: unknown) => Promise<unknown>;
    } = {};
    const refreshResponses: Array<Error | null> = [transientError, null, definitiveError, transientError];
    let refreshCalls = 0;
    const requestMock = vi.fn(async () => ({ data: { code: "SUCCESS" } }));
    const apiMock = Object.assign(requestMock, {
      defaults: { headers: { common: {} as Record<string, unknown> } },
      interceptors: {
        response: {
          use: (_onFulfilled: unknown, onRejected: (error: unknown) => Promise<unknown>) => {
            responseHandlers.onRejected = onRejected;
          },
        },
      },
      post: vi.fn(async () => {
        const refreshError = refreshResponses[refreshCalls];
        refreshCalls += 1;
        if (refreshError) {
          throw refreshError;
        }
        return { data: null };
      }),
    });

    vi.doMock("axios", () => ({
      AxiosError: class AxiosError {},
      InternalAxiosRequestConfig: class InternalAxiosRequestConfig {},
      default: {
        create: () => apiMock,
      },
    }));

    vi.doMock("@/shared/lib/navigation", () => ({
      redirectToLogin: redirectSpy,
    }));

    const { default: api, resetAuthRefreshState } = await import("./client");
    void api;
    const rejectProtectedRequest = (id: number) => responseHandlers.onRejected?.({
      config: { url: `/api/v1/protected/${id}` },
      response: { status: 401 },
    });

    await expect(rejectProtectedRequest(1)).rejects.toBe(transientError);
    resetAuthRefreshState();
    await expect(rejectProtectedRequest(2)).resolves.toEqual({ data: { code: "SUCCESS" } });
    expect(refreshCalls).toBe(2);

    await expect(rejectProtectedRequest(3)).rejects.toBe(definitiveError);
    expect(redirectSpy).toHaveBeenCalledTimes(1);

    await expect(rejectProtectedRequest(4)).rejects.toBe(transientError);
    expect(refreshCalls).toBe(4);
  });
});
