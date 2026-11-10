import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import * as adminApi from "@/features/admin/api/admin.api";
import type { AdminNotificationDeliveryResponse, PageResponse } from "@/shared/types/api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

import AdminNotificationDeliveriesPage from "./page";

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
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe("AdminNotificationDeliveriesPage query integration", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("재처리한 작업이 다시 DLQ에 들어오면 목록 새로고침과 재선택 후 다시 재처리할 수 있다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const queryClient = createTestQueryClient();
    queryClient.setDefaultOptions({ queries: { staleTime: 60_000, retry: false } });
    let currentDelivery = delivery;
    vi.mocked(adminApi.getAdminDlqNotificationDeliveries).mockImplementation(async () => ({
      ...dlqPage, content: [currentDelivery],
    }));
    vi.mocked(adminApi.getAdminNotificationDelivery).mockImplementation(async () => currentDelivery);
    vi.mocked(adminApi.replayAdminNotificationDelivery).mockImplementation(async () => {
      currentDelivery = { ...delivery, attempts: 6, lastErrorReason: "재처리 후 다시 실패" };
      return { ...delivery, status: "PENDING" };
    });

    try {
      render(<AdminNotificationDeliveriesPage />, { wrapper: createQueryWrapper(queryClient) });
      await user.click(await screen.findByRole("button", { name: /강좌/ }));
      const replayButton = await screen.findByRole("button", { name: "선택한 발송 작업 재처리" });
      await waitFor(() => expect(replayButton).toBeEnabled());
      await user.click(replayButton);

      await waitFor(() => expect(replayButton).not.toBeInTheDocument());
      const refresh = screen.getByRole("button", { name: "새로고침" });
      await waitFor(() => expect(refresh).toBeEnabled());
      await user.click(refresh);
      await user.click(await screen.findByRole("button", { name: /강좌.*시도 6회/ }));

      await waitFor(() => expect(screen.getByRole("button", { name: "선택한 발송 작업 재처리" })).toBeEnabled());
      expect(screen.getByText("재처리 후 다시 실패", { exact: true })).toBeInTheDocument();
    } finally {
      confirm.mockRestore();
    }
  });

  it("상세가 열린 상태에서 새로고침하면 서버 상태에 따라 재처리를 막는다", async () => {
    const user = userEvent.setup();
    vi.mocked(adminApi.getAdminDlqNotificationDeliveries).mockResolvedValue(dlqPage);
    vi.mocked(adminApi.getAdminNotificationDelivery)
      .mockResolvedValueOnce(delivery)
      .mockResolvedValue({ ...delivery, status: "PENDING" });

    render(<AdminNotificationDeliveriesPage />, { wrapper: createQueryWrapper(createTestQueryClient()) });
    await user.click(await screen.findByRole("button", { name: /강좌/ }));
    const replayButton = await screen.findByRole("button", { name: "선택한 발송 작업 재처리" });
    await waitFor(() => expect(replayButton).toBeEnabled());

    await user.click(screen.getByRole("button", { name: "새로고침" }));

    expect(await screen.findByText("PENDING")).toBeInTheDocument();
    expect(replayButton).toBeDisabled();
    expect(adminApi.getAdminNotificationDelivery).toHaveBeenCalledTimes(2);
  });
});
