import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useSettingsPage } from "./useSettingsPage";

const mocks = vi.hoisted(() => ({ profile: vi.fn(), save: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn() }), useSearchParams: () => new URLSearchParams(),
}));
vi.mock("@/features/user/hooks/useWebPush", () => ({
  useWebPush: () => ({ subscribe: vi.fn(), loading: false }),
}));
vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: mocks.profile, updateSettings: mocks.save,
  getDevices: vi.fn().mockResolvedValue([]), deleteDevice: vi.fn(), unlinkDiscord: vi.fn(),
  sendVerificationCode: vi.fn(), verifyEmail: vi.fn(), sendTestNotification: vi.fn(),
}));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock("@/shared/telemetry/client-error", () => ({ reportClientError: vi.fn() }));

describe("지원 종료된 FCM 설정 호환", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.profile.mockResolvedValue({
      id: 7, name: "가상 사용자", email: "legacy@example.invalid", role: "USER",
      notificationEmail: "legacy@example.invalid", onboardingCompleted: true,
      emailEnabled: true, webPushEnabled: false, fcmEnabled: true, discordEnabled: false,
    });
    mocks.save.mockResolvedValue(undefined);
  });

  it("과거 true 프로필을 읽어도 설정 저장으로 FCM을 다시 활성화하지 않는다", async () => {
    const { result } = renderHook(() => useSettingsPage());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.user?.fcmEnabled).toBe(true);
    await act(async () => {
      await result.current.onSubmit({
        notificationEmail: "legacy@example.invalid", emailEnabled: true,
        webPushEnabled: false, discordEnabled: false,
      });
    });
    expect(mocks.save).toHaveBeenCalledWith({
      notificationEmail: "legacy@example.invalid", emailEnabled: true,
      webPushEnabled: false, discordEnabled: false, fcmEnabled: false,
    });
    expect(result.current.user?.fcmEnabled).toBe(true);
  });
});
