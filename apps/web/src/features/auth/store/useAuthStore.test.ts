import { describe, it, expect, vi, beforeEach } from "vitest";
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
    // Zustand store reset
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isLoginModalOpen: false,
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
    vi.mocked(userApi.getMyProfile).mockRejectedValue(new Error("Unauthorized"));

    await useAuthStore.getState().checkSession();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
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
