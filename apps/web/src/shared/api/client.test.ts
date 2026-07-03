import { beforeEach, describe, expect, it, vi } from "vitest";

describe("shared api client", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("로컬 API URL에서는 ngrok 경고 헤더를 붙이지 않는다", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://localhost:8080");

    const { default: api } = await import("./client");

    expect((api.defaults.headers.common as Record<string, unknown>)["ngrok-skip-browser-warning"]).toBeUndefined();
  });

  it("refresh 실패 시 로그인 페이지로 이동한다", async () => {
    const refreshError = new Error("refresh failed");
    const redirectSpy = vi.fn();
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

    await import("./client");

    const responseError = {
      config: { url: "/api/v1/users/me" },
      response: { status: 401 },
    };

    await expect(responseHandlers.onRejected?.(responseError)).rejects.toThrow("refresh failed");
    expect(redirectSpy).toHaveBeenCalledTimes(1);
  });
});
