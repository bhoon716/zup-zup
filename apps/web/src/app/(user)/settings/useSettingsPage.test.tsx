import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useSettingsPage } from "./useSettingsPage";

const mocks = vi.hoisted(() => ({ profile: vi.fn(), devices: vi.fn(), save: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn() }), useSearchParams: () => new URLSearchParams(),
}));
vi.mock("@/features/user/hooks/useWebPush", () => ({
  useWebPush: () => ({ subscribe: vi.fn(), loading: false }),
}));
vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: mocks.profile, updateSettings: mocks.save, getDevices: mocks.devices,
  deleteDevice: vi.fn(), unlinkDiscord: vi.fn(), sendVerificationCode: vi.fn(),
  verifyEmail: vi.fn(), sendTestNotification: vi.fn(),
}));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock("@/shared/telemetry/client-error", () => ({ reportClientError: vi.fn() }));

describe("지원 채널 설정 저장", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    vi.resetAllMocks();
    mocks.profile.mockResolvedValue({
      id: 7, name: "가상 사용자", email: "user@example.invalid", role: "USER",
      notificationEmail: "user@example.invalid", onboardingCompleted: true,
      emailEnabled: true, webPushEnabled: false, discordEnabled: false,
    });
    mocks.devices.mockResolvedValue([]);
    mocks.save.mockImplementation(async values => ({
      id: 7, name: "가상 사용자", email: "user@example.invalid", role: "USER",
      notificationEmail: "user@example.invalid", onboardingCompleted: true,
      emailEnabled: true, webPushEnabled: false, discordEnabled: false, ...values,
    }));
  });

  it("지원 채널의 설정만 요청에 포함한다", async () => {
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    await act(async () => {
      await result.current.onSubmit({
        notificationEmail: "user@example.invalid", emailEnabled: false,
        webPushEnabled: false, discordEnabled: false,
      });
    });
    expect(mocks.save).toHaveBeenCalledExactlyOnceWith({
      notificationEmail: "user@example.invalid", emailEnabled: false,
      webPushEnabled: false, discordEnabled: false,
    });
  });

  it.each(["profile", "devices"] as const)("%s 조회 실패 후에는 설정을 저장하지 않는다", async (failedRequest) => {
    mocks[failedRequest].mockRejectedValueOnce(new Error("local unavailable"));
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.user).toBeNull();
    await act(async () => {
      await result.current.onSubmit({
        notificationEmail: "user@example.invalid", emailEnabled: true,
        webPushEnabled: false, discordEnabled: false,
      });
    });
    expect(mocks.save).not.toHaveBeenCalled();
    expect(result.current.isSubmitting).toBe(false);
    expect(toast.error).toHaveBeenCalledWith("프로필 정보를 확인한 뒤 다시 시도해 주세요.");
  });
  it("저장 응답으로 공유 사용자를 갱신하고 프로필을 추가 조회하지 않는다", async () => {
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    await act(async () => result.current.onSubmit({
      notificationEmail: "user@example.invalid", emailEnabled: false,
      webPushEnabled: false, discordEnabled: false,
    }));
    expect(useAuthStore.getState().user?.emailEnabled).toBe(false);
    expect(result.current.user?.emailEnabled).toBe(false);
    expect(mocks.profile).toHaveBeenCalledTimes(1);
  });

  it("전역 사용자가 있어도 기기 조회가 실패하면 설정 저장을 차단한다", async () => {
    useAuthStore.getState().setUser(await mocks.profile());
    mocks.devices.mockRejectedValueOnce(new Error("local unavailable"));
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(useAuthStore.getState().user?.id).toBe(7);
    expect(result.current.user).toBeNull();
    await act(async () => result.current.onSubmit({
      notificationEmail: "user@example.invalid", emailEnabled: true,
      webPushEnabled: false, discordEnabled: false,
    }));
    expect(mocks.save).not.toHaveBeenCalled();
  });

  it("다른 곳의 프로필 갱신은 작성 중인 폼을 초기화하지 않는다", async () => {
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    act(() => result.current.setValue("notificationEmail", "draft@example.invalid"));
    act(() => useAuthStore.getState().setUser({ ...useAuthStore.getState().user!, name: "수정된 이름" }));
    expect(result.current.watch("notificationEmail")).toBe("draft@example.invalid");
    expect(result.current.user?.name).toBe("수정된 이름");
  });

  it("로그아웃 후 도착한 설정 저장 응답은 사용자를 복원하지 않는다", async () => {
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    const profile = useAuthStore.getState().user;
    let finish!: (value: unknown) => void;
    mocks.save.mockReturnValueOnce(new Promise(resolve => { finish = resolve; }));
    let pending!: Promise<void>;
    act(() => { pending = result.current.onSubmit({
      notificationEmail: "user@example.invalid", emailEnabled: false,
      webPushEnabled: false, discordEnabled: false,
    }); });
    await waitFor(() => expect(mocks.save).toHaveBeenCalledTimes(1));
    act(() => useAuthStore.getState().logout());
    await act(async () => { finish(profile); await pending; });
    expect(useAuthStore.getState().user).toBeNull();
    expect(result.current.user).toBeNull();
  });

});
