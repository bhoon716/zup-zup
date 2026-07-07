import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as subscriptionApi from "@/features/subscription/api/subscription.api";
import { useUser } from "@/features/user/hooks/useUser";
import { toast } from "sonner";
import { useSubscribe, useSubscriptions, useUnsubscribe } from "./useSubscriptions";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/subscription/api/subscription.api", () => ({
  getMySubscriptions: vi.fn(),
  subscribe: vi.fn(),
  unsubscribe: vi.fn(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("useSubscriptions hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("비로그인 상태에서는 구독 목록 조회를 실행하지 않는다", async () => {
    vi.mocked(useUser).mockReturnValue({ data: null } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useSubscriptions(), { wrapper });

    await waitFor(() => expect(result.current.fetchStatus).toBe("idle"));
    expect(subscriptionApi.getMySubscriptions).not.toHaveBeenCalled();
  });

  it("로그인 상태에서는 구독 목록을 조회한다", async () => {
    vi.mocked(useUser).mockReturnValue({ data: { id: 1 } } as never);
    vi.mocked(subscriptionApi.getMySubscriptions).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: [{ id: 1, courseKey: "CSE-101", courseName: "자료구조", professorName: "김교수", isActive: true, createdAt: "2026-01-01T00:00:00" }],
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useSubscriptions(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(subscriptionApi.getMySubscriptions).toHaveBeenCalledTimes(1);
  });

  it("구독 성공 시 목록 캐시를 무효화하고 성공 토스트를 노출한다", async () => {
    vi.mocked(subscriptionApi.subscribe).mockResolvedValue({
      code: "SUCCESS",
      message: "구독 성공",
      data: null,
    } as never);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useSubscribe(), { wrapper });

    act(() => {
      result.current.mutate({ courseKey: "CSE-101" });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["subscriptions"] });
    expect(toast.success).toHaveBeenCalledWith("구독 성공");
  });

  it("구독 취소 실패 시 에러 토스트를 노출한다", async () => {
    vi.mocked(subscriptionApi.unsubscribe).mockRejectedValue({
      response: { data: { message: "취소 실패" } },
    });

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUnsubscribe(), { wrapper });

    act(() => {
      result.current.mutate(1);
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(toast.error).toHaveBeenCalledWith("취소 실패");
  });
});
