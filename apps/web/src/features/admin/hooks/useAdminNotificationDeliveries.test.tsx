import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import * as adminApi from "@/features/admin/api/admin.api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

import {
  useAdminDlqNotificationDeliveries,
  useReplayAdminNotificationDelivery,
} from "./useAdminNotificationDeliveries";

vi.mock("@/features/admin/api/admin.api", () => ({
  getAdminDlqNotificationDeliveries: vi.fn(),
  getAdminNotificationDelivery: vi.fn(),
  replayAdminNotificationDelivery: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("admin notification delivery hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("DLQ 목록을 관리자 페이지 키로 조회한다", async () => {
    vi.mocked(adminApi.getAdminDlqNotificationDeliveries).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: { content: [], totalElements: 0 },
    } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useAdminDlqNotificationDeliveries(1, 20), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminApi.getAdminDlqNotificationDeliveries).toHaveBeenCalledWith(1, 20);
  });

  it("성공한 재처리 뒤 DLQ 목록과 상세 캐시를 무효화한다", async () => {
    vi.mocked(adminApi.replayAdminNotificationDelivery).mockResolvedValue({
      code: "SUCCESS",
      message: "재처리 대기열에 넣었습니다.",
      data: {},
    } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const { result } = renderHook(() => useReplayAdminNotificationDelivery(), { wrapper });

    act(() => {
      result.current.mutate({ id: 41, request: {} });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminApi.replayAdminNotificationDelivery).toHaveBeenCalledWith(41, {});
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["admin", "notification-deliveries"] });
  });
});
