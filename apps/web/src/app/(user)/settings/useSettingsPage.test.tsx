import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
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
    vi.resetAllMocks();
    mocks.profile.mockResolvedValue({
      id: 7, name: "가상 사용자", email: "user@example.invalid", role: "USER",
      notificationEmail: "user@example.invalid", onboardingCompleted: true,
      emailEnabled: true, webPushEnabled: false, discordEnabled: false,
    });
    mocks.devices.mockResolvedValue([]);
    mocks.save.mockResolvedValue(undefined);
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
});
