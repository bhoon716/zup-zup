import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import * as adminApi from "@/features/admin/api/admin.api";
import type { AdminNotificationDeliveryResponse, PageResponse } from "@/shared/types/api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

import {
  adminNotificationDeliveryKeys,
  useAdminDlqNotificationDeliveries,
  useAdminNotificationDelivery,
  useReplayAdminNotificationDelivery,
} from "./useAdminNotificationDeliveries";

const delivery: AdminNotificationDeliveryResponse = {
  id: 41, outboxId: 11, courseKey: "course", courseName: "강좌", channel: "EMAIL", status: "DLQ",
  attempts: 5, lastError: "N001", lastErrorReason: "수신 실패", deadLetteredAt: "2026-11-11T00:00:00",
  idempotencyKeyRetained: true,
};
const dlqPage: PageResponse<AdminNotificationDeliveryResponse> = {
  content: [delivery],
  pageable: { pageNumber: 0, pageSize: 30, sort: { empty: true, sorted: false, unsorted: true } },
  totalElements: 1, totalPages: 1, last: true, size: 30, number: 0, first: true, numberOfElements: 1, empty: false,
};

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
    vi.mocked(adminApi.getAdminDlqNotificationDeliveries).mockResolvedValue({ content: [], totalElements: 0 } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useAdminDlqNotificationDeliveries(1), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(adminApi.getAdminDlqNotificationDeliveries).toHaveBeenCalledWith(1);
  });

  it("최신 DLQ 목록을 본 다른 관리자가 먼저 재처리했어도 상세에서 현재 상태를 조회한다", async () => {
    const replayed = { ...delivery, status: "PENDING" as const };
    vi.mocked(adminApi.getAdminNotificationDelivery).mockResolvedValue(replayed);
    const queryClient = createTestQueryClient();
    queryClient.setDefaultOptions({ queries: { staleTime: 60_000, retry: false } });
    queryClient.setQueryData(adminNotificationDeliveryKeys.dlq(0), dlqPage);

    const { result } = renderHook(() => useAdminNotificationDelivery(delivery.id), {
      wrapper: createQueryWrapper(queryClient),
    });

    await waitFor(() => expect(result.current.data).toEqual(replayed));
    expect(adminApi.getAdminNotificationDelivery).toHaveBeenCalledExactlyOnceWith(delivery.id);
  });

  it("재처리가 성공하면 해당 상세와 DLQ 목록을 다시 조회한다", async () => {
    const replayed = { ...delivery, status: "PENDING" as const };
    const emptyPage = { ...dlqPage, content: [], totalElements: 0, totalPages: 0, numberOfElements: 0, empty: true };
    vi.mocked(adminApi.replayAdminNotificationDelivery).mockResolvedValue(replayed);
    vi.mocked(adminApi.getAdminNotificationDelivery).mockResolvedValueOnce(delivery).mockResolvedValue(replayed);
    vi.mocked(adminApi.getAdminDlqNotificationDeliveries).mockResolvedValueOnce(dlqPage).mockResolvedValue(emptyPage);
    const queryClient = createTestQueryClient();
    const { result } = renderHook(() => ({
      detail: useAdminNotificationDelivery(delivery.id),
      list: useAdminDlqNotificationDeliveries(),
      replay: useReplayAdminNotificationDelivery(),
    }), { wrapper: createQueryWrapper(queryClient) });

    await waitFor(() => expect(result.current.detail.data).toEqual(delivery));
    await waitFor(() => expect(result.current.list.data).toEqual(dlqPage));
    act(() => {
      result.current.replay.mutate({ id: delivery.id, request: {} });
    });

    await waitFor(() => expect(result.current.detail.data).toEqual(replayed));
    await waitFor(() => expect(result.current.list.data).toEqual(emptyPage));
    expect(adminApi.replayAdminNotificationDelivery).toHaveBeenCalledExactlyOnceWith(delivery.id, {});
    expect(adminApi.getAdminNotificationDelivery).toHaveBeenCalledTimes(2);
  });
});
