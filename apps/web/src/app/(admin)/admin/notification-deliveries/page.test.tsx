import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import AdminNotificationDeliveriesPage from "./page";

const {
  mockReplay,
  mockUseAdminDlqNotificationDeliveries,
  mockUseAdminNotificationDelivery,
  mockUseReplayAdminNotificationDelivery,
} = vi.hoisted(() => ({
  mockReplay: vi.fn(),
  mockUseAdminDlqNotificationDeliveries: vi.fn(),
  mockUseAdminNotificationDelivery: vi.fn(),
  mockUseReplayAdminNotificationDelivery: vi.fn(),
}));

vi.mock("@/features/admin/hooks/useAdminNotificationDeliveries", () => ({
  useAdminDlqNotificationDeliveries: (...args: unknown[]) => mockUseAdminDlqNotificationDeliveries(...args),
  useAdminNotificationDelivery: (...args: unknown[]) => mockUseAdminNotificationDelivery(...args),
  useReplayAdminNotificationDelivery: () => mockUseReplayAdminNotificationDelivery(),
}));

describe("AdminNotificationDeliveriesPage", () => {
  const delivery = {
    id: 41,
    outboxId: 12,
    courseKey: "CS-101",
    courseName: "알고리즘",
    channel: "EMAIL",
    status: "DLQ",
    attempts: 5,
    lastError: "N001",
    deadLetteredAt: "2026-07-13T01:23:45",
    idempotencyKeyRetained: true,
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    mockUseAdminDlqNotificationDeliveries.mockReturnValue({
      data: { content: [delivery], totalElements: 1 },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    });
    mockUseAdminNotificationDelivery.mockImplementation((id: number | null) => ({
      data: id === delivery.id ? delivery : undefined,
      isLoading: false,
      isError: false,
    }));
    mockUseReplayAdminNotificationDelivery.mockReturnValue({
      mutateAsync: mockReplay,
      isPending: false,
    });
    mockReplay.mockResolvedValue({ code: "SUCCESS", message: "ok", data: delivery });
  });

  it("DLQ 목록에서 선택한 delivery의 안전한 상세와 재처리 동작을 제공한다", async () => {
    render(<AdminNotificationDeliveriesPage />);

    fireEvent.click(screen.getByRole("button", { name: /알고리즘/ }));

    expect(screen.getByText("CS-101")).toBeInTheDocument();
    expect(screen.getByText("idempotency key 유지")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "선택 delivery 재처리" }));

    await waitFor(() => {
      expect(mockReplay).toHaveBeenCalledWith({ id: 41, request: {} });
    });
  });

  it("전체 DLQ를 페이지 이동으로 조회할 수 있다", async () => {
    const secondPageDelivery = {
      ...delivery,
      id: 42,
      courseKey: "CS-102",
      courseName: "운영체제",
    };
    mockUseAdminDlqNotificationDeliveries.mockImplementation((page: number = 0) => ({
      data: {
        content: page === 0 ? [delivery] : [secondPageDelivery],
        totalElements: 21,
        totalPages: 2,
        number: page,
        size: 20,
      },
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    }));

    render(<AdminNotificationDeliveriesPage />);

    expect(screen.getByText("1 / 2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "다음 페이지" }));

    await waitFor(() => {
      expect(mockUseAdminDlqNotificationDeliveries).toHaveBeenLastCalledWith(1, 20);
    });
    expect(screen.getByText("운영체제")).toBeInTheDocument();
  });
});
