import { describe, it, expect, vi, beforeEach } from "vitest";
import { useAuthStore } from "./useAuthStore";
import * as userApi from "@/features/user/api/user.api";
import type { CommonResponse, User } from "@/shared/types/api";

vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: vi.fn(),
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
  });
});
