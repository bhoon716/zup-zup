import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as userApi from "@/features/user/api/user.api";
import { toast } from "sonner";
import { useRegisterDevice, useUnregisterDevice } from "./useUserDevices";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/user/api/user.api", () => ({
  registerDevice: vi.fn(),
  unregisterDevice: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("useUserDevices hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("기기 등록 성공 시 사용자 프로필 캐시를 무효화한다", async () => {
    vi.mocked(userApi.registerDevice).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: undefined,
    } as never);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useRegisterDevice(), { wrapper });

    act(() => {
      result.current.mutate({ type: "WEB", token: "token-value" });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["user-profile"] });
    expect(toast.success).toHaveBeenCalledWith("기기가 등록되었습니다.");
  });

  it("기기 해제 실패 시 에러 토스트를 노출한다", async () => {
    vi.mocked(userApi.unregisterDevice).mockRejectedValue(new Error("failed"));

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUnregisterDevice(), { wrapper });

    act(() => {
      result.current.mutate("token-value");
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(toast.error).toHaveBeenCalledWith("기기 해제에 실패했습니다.");
  });
});
