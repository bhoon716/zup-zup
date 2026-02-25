import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as adminApi from "@/features/admin/api/admin.api";
import { toast } from "sonner";
import { useCrawlCourses, useSendTestNotification } from "./useAdminActions";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/admin/api/admin.api", () => ({
  crawlCourses: vi.fn(),
  sendTestNotification: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("useAdminActions hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("크롤링 성공 시 통계 쿼리를 무효화하고 성공 토스트를 노출한다", async () => {
    const mockedCrawlCourses = vi.mocked(adminApi.crawlCourses);
    mockedCrawlCourses.mockResolvedValue({
      code: "SUCCESS",
      message: "크롤링 시작",
      data: "ok",
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const { result } = renderHook(() => useCrawlCourses(), { wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["admin", "stats"] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["admin", "overview"] });
    expect(toast.success).toHaveBeenCalledWith("크롤링 시작");
  });

  it("크롤링 실패 시 에러 토스트를 노출한다", async () => {
    const mockedCrawlCourses = vi.mocked(adminApi.crawlCourses);
    mockedCrawlCourses.mockRejectedValue({
      response: { data: { message: "크롤링 실패" } },
    });

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCrawlCourses(), { wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(toast.error).toHaveBeenCalledWith("크롤링 실패");
  });

  it("테스트 알림 전송 성공 시 성공 토스트를 노출한다", async () => {
    const mockedSend = vi.mocked(adminApi.sendTestNotification);
    mockedSend.mockResolvedValue({
      code: "SUCCESS",
      message: "알림 전송 완료",
      data: undefined,
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useSendTestNotification(), { wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(toast.success).toHaveBeenCalledWith("알림 전송 완료");
  });
});
