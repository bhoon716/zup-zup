import { AxiosError, AxiosHeaders, isCancel, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import { beforeEach, describe, expect, it, vi } from "vitest";

type SilentRequest = AxiosRequestConfig & { silentAuthFailure: boolean };

const response = (config: InternalAxiosRequestConfig, data: unknown, status = 200): AxiosResponse => {
  const result = { data, status, statusText: "test", headers: new AxiosHeaders(), config };
  if (status >= 400) throw new AxiosError("request failed", AxiosError.ERR_BAD_RESPONSE, config, undefined, result);
  return result;
};

const deferred = <T,>() => {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(complete => { resolve = complete; });
  return { promise, resolve };
};

describe("session api client", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("안전한 요청과 정확한 강의 검색 POST는 CSRF 조회 없이 세션 쿠키로 전송한다", async () => {
    const { default: api } = await import("./client");
    const requests: InternalAxiosRequestConfig[] = [];
    api.defaults.adapter = async config => { requests.push(config); return response(config, []); };
    await Promise.all([
      api.get("/api/v1/users/me"), api.head("/health"), api.options("/api/v1/courses"),
      api.post("/api/v1/courses/search?size=30", {}),
    ]);
    expect(requests).toHaveLength(4);
    for (const request of requests) {
      expect(request.withCredentials).toBe(true);
      expect(request.headers.get("X-CSRF-TOKEN")).toBeUndefined();
      expect(request.headers.get("Authorization")).toBeUndefined();
    }
  });

  it("동시 변경 요청은 CSRF 조회를 공유하고 이후에도 메모리에 저장된 토큰을 사용한다", async () => {
    const { default: api } = await import("./client");
    const csrfRequested = deferred<void>();
    const csrf = deferred<void>();
    const requests: InternalAxiosRequestConfig[] = [];
    api.defaults.adapter = async config => {
      requests.push(config);
      if (config.url === "/api/auth/csrf") { csrfRequested.resolve(); await csrf.promise; return response(config, { token: "session-csrf" }); }
      return response(config, null);
    };
    const writes = [api.post("/resource"), api.put("/resource"), api.patch("/resource"), api.delete("/resource")];
    await csrfRequested.promise;
    expect(requests).toHaveLength(1);
    csrf.resolve();
    await Promise.all(writes);
    await api.post("/api/auth/logout");
    expect(requests.filter(request => request.url === "/api/auth/csrf")).toHaveLength(1);
    expect(requests.filter(request => request.url !== "/api/auth/csrf").map(request => request.headers.get("X-CSRF-TOKEN")))
      .toEqual(Array(5).fill("session-csrf"));
  });

  it("강의 검색의 비슷한 경로나 다른 변경 메서드는 CSRF 예외가 아니다", async () => {
    const { default: api } = await import("./client");
    const writes: InternalAxiosRequestConfig[] = [];
    api.defaults.adapter = async config => {
      if (config.url === "/api/auth/csrf") return response(config, { token: "csrf" });
      writes.push(config); return response(config, null);
    };
    await Promise.all([api.post("/api/v1/courses/search/"), api.post("/api/v1/courses/search-extra"), api.put("/api/v1/courses/search")]);
    expect(writes).toHaveLength(3);
    expect(writes.every(request => request.headers.get("X-CSRF-TOKEN") === "csrf")).toBe(true);
  });

  it.each(["network", "missing-token"])("CSRF 조회 실패 시 쓰기를 보내지 않고 다음 명시적 요청에서 다시 조회한다 (%s)", async failure => {
    const { default: api } = await import("./client");
    let csrfRequests = 0;
    let writes = 0;
    api.defaults.adapter = async config => {
      if (config.url === "/api/auth/csrf") {
        csrfRequests += 1;
        if (csrfRequests === 1) {
          if (failure === "network") throw new AxiosError("offline", AxiosError.ERR_NETWORK, config);
          return response(config, {});
        }
        return response(config, { token: "recovered-csrf" });
      }
      writes += 1; return response(config, null);
    };
    await expect(api.post("/resource")).rejects.toBeInstanceOf(Error);
    expect(csrfRequests).toBe(1);
    expect(writes).toBe(0);
    await api.post("/resource");
    expect(csrfRequests).toBe(2);
    expect(writes).toBe(1);
  });

  it.each([false, true])("401은 자동 재시도 없이 인증 실패를 한 번만 알린다 (핸들러 상태 초기화: %s)", async resetInHandler => {
    const { default: api, registerAuthFailureHandler, resetSessionState } = await import("./client");
    const handler = vi.fn(() => { if (resetInHandler) resetSessionState(); });
    registerAuthFailureHandler(handler);
    const requests: string[] = [];
    api.defaults.adapter = async config => { requests.push(config.url!); return response(config, { code: "A001" }, 401); };
    const results = await Promise.allSettled([api.get("/private/one"), api.get("/private/two")]);
    expect(results.map(result => result.status)).toEqual(["rejected", "rejected"]);
    expect(handler).toHaveBeenCalledTimes(1);
    expect(requests).toEqual(["/private/one", "/private/two"]);
  });

  it("silent 401은 로그인 안내 없이 CSRF 캐시만 무효화한다", async () => {
    const { default: api, registerAuthFailureHandler } = await import("./client");
    const handler = vi.fn(); registerAuthFailureHandler(handler);
    let csrfRequests = 0;
    api.defaults.adapter = async config => {
      if (config.url === "/api/auth/csrf") return response(config, { token: `csrf-${++csrfRequests}` });
      return response(config, null, config.url === "/api/v1/users/me" ? 401 : 200);
    };
    await api.post("/resource");
    await expect(api.get("/api/v1/users/me", { silentAuthFailure: true } as SilentRequest)).rejects.toMatchObject({ response: { status: 401 } });
    expect(handler).not.toHaveBeenCalled();
    await api.post("/resource");
    expect(csrfRequests).toBe(2);
  });

  it.each(["A002", "A003"])("403은 로그아웃이나 쓰기 재전송 없이 전달하고 A003만 CSRF 캐시를 비운다 (%s)", async code => {
    const { default: api, registerAuthFailureHandler } = await import("./client");
    const handler = vi.fn(); registerAuthFailureHandler(handler);
    let csrfRequests = 0;
    const writeTokens: unknown[] = [];
    api.defaults.adapter = async config => {
      if (config.url === "/api/auth/csrf") return response(config, { token: `csrf-${++csrfRequests}` });
      writeTokens.push(config.headers.get("X-CSRF-TOKEN"));
      return response(config, { code }, writeTokens.length === 1 ? 403 : 200);
    };
    await expect(api.post("/resource")).rejects.toMatchObject({ response: { status: 403, data: { code } } });
    expect(writeTokens).toEqual(["csrf-1"]);
    expect(handler).not.toHaveBeenCalled();
    await api.post("/resource");
    expect(writeTokens).toEqual(["csrf-1", code === "A003" ? "csrf-2" : "csrf-1"]);
    expect(csrfRequests).toBe(code === "A003" ? 2 : 1);
  });

  it("이전 세대의 401은 현재 인증 상태와 새 CSRF 캐시를 변경하지 않는다", async () => {
    const { default: api, registerAuthFailureHandler, resetSessionState } = await import("./client");
    const handler = vi.fn(); registerAuthFailureHandler(handler);
    const started = deferred<void>();
    const oldResponse = deferred<void>();
    let csrfRequests = 0;
    api.defaults.adapter = async config => {
      if (config.url === "/old-private") { started.resolve(); await oldResponse.promise; return response(config, { code: "A001" }, 401); }
      if (config.url === "/api/auth/csrf") return response(config, { token: `csrf-${++csrfRequests}` });
      return response(config, null);
    };
    const old = api.get("/old-private");
    const result = Promise.allSettled([old]);
    await started.promise;
    resetSessionState();
    await api.post("/new-resource");
    oldResponse.resolve();
    expect((await result)[0].status).toBe("rejected");
    await api.post("/new-resource");
    expect(handler).not.toHaveBeenCalled();
    expect(csrfRequests).toBe(1);
  });

  it("세션 초기화 중 대기하던 쓰기는 취소하고 새 세대의 CSRF 조회를 보존한다", async () => {
    const { default: api, resetSessionState } = await import("./client");
    const oldStarted = deferred<void>();
    const newStarted = deferred<void>();
    const oldCsrf = deferred<void>();
    const newCsrf = deferred<void>();
    let csrfRequests = 0;
    const writes: string[] = [];
    api.defaults.adapter = async config => {
      if (config.url === "/api/auth/csrf") {
        const index = ++csrfRequests;
        (index === 1 ? oldStarted : newStarted).resolve();
        await (index === 1 ? oldCsrf : newCsrf).promise;
        return response(config, { token: `csrf-${index}` });
      }
      writes.push(`${config.url}:${config.headers.get("X-CSRF-TOKEN")}`);
      return response(config, null);
    };
    const oldWrites = Promise.allSettled([api.post("/old-resource"), api.delete("/old-resource")]);
    await oldStarted.promise;
    resetSessionState();
    const currentWrite = api.post("/new-resource");
    await newStarted.promise;
    oldCsrf.resolve();
    const cancelled = await oldWrites;
    expect(cancelled.every(result => result.status === "rejected" && isCancel(result.reason))).toBe(true);
    expect(writes).toEqual([]);
    const sharedCurrentWrite = api.patch("/new-resource");
    newCsrf.resolve();
    await Promise.all([currentWrite, sharedCurrentWrite]);
    await api.post("/new-resource");
    expect(csrfRequests).toBe(2);
    expect(writes).toEqual(Array(3).fill("/new-resource:csrf-2"));
  });
});
