import { describe, it, expect, vi, beforeEach } from "vitest";
import { AxiosError } from "axios";
import { useAuthStore } from "./useAuthStore";
import * as userApi from "@/features/user/api/user.api";
import type { CommonResponse, User } from "@/shared/types/api";

vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: vi.fn(),
  clearMyProfileRequestCache: vi.fn(),
}));

describe("useAuthStore", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.persist.clearStorage();
    // Zustand store reset
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isLoginModalOpen: false,
    });
  });

  it("persisted 사용자를 복원해도 서버 검증 전에는 인증 완료로 취급하지 않는다", async () => {
    const restoredUser: User = {
      id: 1,
      email: "restored@test.com",
      name: "복원 사용자",
      role: "ADMIN",
      emailEnabled: false,
      webPushEnabled: false,
      fcmEnabled: false,
      discordEnabled: false,
      onboardingCompleted: false,
    };
    await useAuthStore.persist.getOptions().storage?.setItem("auth-storage", {
      state: { user: restoredUser },
      version: 0,
    });

    await useAuthStore.persist.rehydrate();

    expect(useAuthStore.getState()).toMatchObject({
      user: restoredUser,
      isAuthenticated: false,
      isLoading: false,
    });
  });

  it("setUser를 호출하면 사용자 정보와 인증 상태가 업데이트된다", () => {
    const user: User = { 
      id: 1, 
      email: "test@test.com", 
      name: "홍길동", 
      role: "USER", 
      emailEnabled: false,
      webPushEnabled: false,
      fcmEnabled: false,
      discordEnabled: false,
      onboardingCompleted: true 
    };
    useAuthStore.getState().setUser(user);

    const state = useAuthStore.getState();
    expect(state.user).toEqual(user);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it("checkSession 성공 시 사용자 정보를 저장하고 인증 상태를 true로 설정한다", async () => {
    const user: User = { 
      id: 1, 
      email: "test@test.com", 
      name: "홍길동", 
      role: "USER", 
      emailEnabled: false,
      webPushEnabled: false,
      fcmEnabled: false,
      discordEnabled: false,
      onboardingCompleted: true 
    };
    vi.mocked(userApi.getMyProfile).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: user,
    } as CommonResponse<User>);

    await useAuthStore.getState().checkSession();

    const state = useAuthStore.getState();
    expect(state.user).toEqual(user);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it("로컬 인증 상태가 남아 있어도 서버 세션을 다시 검증한다", async () => {
    const staleUser: User = {
      id: 1,
      email: "stale@test.com",
      name: "이전 사용자",
      role: "USER",
      emailEnabled: false,
      webPushEnabled: false,
      fcmEnabled: false,
      discordEnabled: false,
      onboardingCompleted: true,
    };
    useAuthStore.setState({
      user: staleUser,
      isAuthenticated: true,
      isLoading: false,
    });
    vi.mocked(userApi.getMyProfile).mockRejectedValue(
      new AxiosError("Unauthorized", undefined, undefined, undefined, {
        status: 401,
        statusText: "Unauthorized",
        headers: {},
        config: { headers: {} } as never,
        data: { message: "인증이 필요합니다." },
      })
    );

    await useAuthStore.getState().checkSession();

    expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);
    expect(useAuthStore.getState()).toMatchObject({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  });

  it("checkSession이 동시에 여러 번 호출되어도 프로필 요청은 한 번만 보낸다", async () => {
    let resolveProfile!: (value: CommonResponse<User>) => void;
    const profilePromise = new Promise<CommonResponse<User>>((resolve) => {
      resolveProfile = resolve;
    });

    vi.mocked(userApi.getMyProfile).mockReturnValue(profilePromise);

    const firstCall = useAuthStore.getState().checkSession();
    const secondCall = useAuthStore.getState().checkSession();

    resolveProfile({
      code: "SUCCESS",
      message: "ok",
      data: {
        id: 1,
        email: "test@test.com",
        name: "홍길동",
        role: "USER",
        emailEnabled: false,
        webPushEnabled: false,
        fcmEnabled: false,
        discordEnabled: false,
        onboardingCompleted: true,
      },
    });

    await Promise.all([firstCall, secondCall]);

    const state = useAuthStore.getState();
    expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it("로그아웃 후 늦게 도착한 세션 응답은 상태를 덮어쓰지 않는다", async () => {
    let resolveProfile!: (value: CommonResponse<User>) => void;
    const profilePromise = new Promise<CommonResponse<User>>((resolve) => {
      resolveProfile = resolve;
    });

    vi.mocked(userApi.getMyProfile).mockReturnValue(profilePromise);

    const pendingSessionCheck = useAuthStore.getState().checkSession();
    useAuthStore.getState().logout();

    resolveProfile({
      code: "SUCCESS",
      message: "ok",
      data: {
        id: 1,
        email: "test@test.com",
        name: "홍길동",
        role: "USER",
        emailEnabled: false,
        webPushEnabled: false,
        fcmEnabled: false,
        discordEnabled: false,
        onboardingCompleted: true,
      },
    });

    await pendingSessionCheck;

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });

  it("checkSession 실패 시 인증 상태를 false로 설정한다", async () => {
    vi.mocked(userApi.getMyProfile).mockRejectedValue(
      new AxiosError("Unauthorized", undefined, undefined, undefined, {
        status: 401,
        statusText: "Unauthorized",
        headers: {},
        config: { headers: {} } as never,
        data: { message: "인증이 필요합니다." },
      })
    );

    await useAuthStore.getState().checkSession();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });

  it("일시적인 서버 오류에서는 복원된 사용자를 보존하되 인증 완료로 승격하지 않는다", async () => {
    vi.useFakeTimers();
    try {
      const user: User = {
        id: 1,
        email: "test@test.com",
        name: "홍길동",
        role: "USER",
        emailEnabled: false,
        webPushEnabled: false,
        fcmEnabled: false,
        discordEnabled: false,
        onboardingCompleted: true,
      };
      useAuthStore.setState({ user, isAuthenticated: false, isLoading: false });
      vi.mocked(userApi.getMyProfile).mockRejectedValue(
        new AxiosError("Server Error", undefined, undefined, undefined, {
          status: 503,
          statusText: "Service Unavailable",
          headers: {},
          config: { headers: {} } as never,
          data: { message: "잠시 후 다시 시도해 주세요." },
        })
      );

      const sessionCheck = useAuthStore.getState().checkSession();
      await vi.advanceTimersByTimeAsync(1_000);
      await sessionCheck;

      expect(userApi.getMyProfile).toHaveBeenCalledTimes(3);
      expect(useAuthStore.getState()).toMatchObject({
        user,
        isAuthenticated: false,
        isLoading: false,
      });
    } finally {
      vi.useRealTimers();
    }
  });

  it("일시적인 서버 오류 후 자동 재시도에서 성공하면 인증 상태를 복구한다", async () => {
    vi.useFakeTimers();
    try {
      const user: User = {
        id: 1,
        email: "test@test.com",
        name: "홍길동",
        role: "USER",
        emailEnabled: false,
        webPushEnabled: false,
        fcmEnabled: false,
        discordEnabled: false,
        onboardingCompleted: true,
      };
      useAuthStore.setState({ user, isAuthenticated: false, isLoading: false });
      vi.mocked(userApi.getMyProfile)
        .mockRejectedValueOnce(
          new AxiosError("Server Error", undefined, undefined, undefined, {
            status: 503,
            statusText: "Service Unavailable",
            headers: {},
            config: { headers: {} } as never,
            data: { message: "잠시 후 다시 시도해 주세요." },
          })
        )
        .mockResolvedValueOnce({
          code: "SUCCESS",
          message: "ok",
          data: user,
        } as CommonResponse<User>);

      const sessionCheck = useAuthStore.getState().checkSession();
      await vi.advanceTimersByTimeAsync(1_000);
      await sessionCheck;

      expect(userApi.getMyProfile).toHaveBeenCalledTimes(2);
      expect(useAuthStore.getState()).toMatchObject({
        user,
        isAuthenticated: true,
        isLoading: false,
      });
    } finally {
      vi.useRealTimers();
    }
  });

  it("로그아웃하면 예약된 세션 재시도를 취소한다", async () => {
    vi.useFakeTimers();
    try {
      const user: User = {
        id: 1,
        email: "test@test.com",
        name: "홍길동",
        role: "USER",
        emailEnabled: false,
        webPushEnabled: false,
        fcmEnabled: false,
        discordEnabled: false,
        onboardingCompleted: true,
      };
      useAuthStore.setState({ user, isAuthenticated: false, isLoading: false });
      vi.mocked(userApi.getMyProfile)
        .mockRejectedValueOnce(
          new AxiosError("Server Error", undefined, undefined, undefined, {
            status: 503,
            statusText: "Service Unavailable",
            headers: {},
            config: { headers: {} } as never,
            data: { message: "잠시 후 다시 시도해 주세요." },
          })
        )
        .mockResolvedValueOnce({
          code: "SUCCESS",
          message: "ok",
          data: user,
        } as CommonResponse<User>);

      const sessionCheck = useAuthStore.getState().checkSession();
      await vi.advanceTimersByTimeAsync(0);
      expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);

      useAuthStore.getState().logout();
      await vi.advanceTimersByTimeAsync(1_000);
      await sessionCheck;

      expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);
      expect(useAuthStore.getState()).toMatchObject({
        user: null,
        isAuthenticated: false,
        isLoading: false,
      });
    } finally {
      vi.useRealTimers();
    }
  });

  it("logout 호출 시 상태가 초기화된다", () => {
    const user: User = { 
      id: 1, 
      email: "test@test.com", 
      name: "홍길동", 
      role: "USER", 
      emailEnabled: false,
      webPushEnabled: false,
      fcmEnabled: false,
      discordEnabled: false,
      onboardingCompleted: true 
    };
    useAuthStore.setState({ user, isAuthenticated: true });
    
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(userApi.clearMyProfileRequestCache).toHaveBeenCalledTimes(1);
  });
});
