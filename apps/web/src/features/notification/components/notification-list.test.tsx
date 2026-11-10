import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { NotificationList } from "./notification-list";

vi.mock("@/features/notification/hooks/useNotifications", () => ({
  useNotifications: vi.fn(),
}));

const mockUseNotifications = vi.mocked(useNotifications);

describe("NotificationList", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("retries the notification query without reloading the page", async () => {
    const user = userEvent.setup();
    const refetch = vi.fn();

    mockUseNotifications.mockReturnValue({
      data: undefined,
      isLoading: false,
      error: new Error("notification request failed"),
      fetchNextPage: vi.fn(),
      hasNextPage: false,
      isFetchingNextPage: false,
      refetch,
    } as unknown as ReturnType<typeof useNotifications>);

    render(<NotificationList />);

    await user.click(screen.getByRole("button", { name: "다시 시도" }));

    expect(refetch).toHaveBeenCalledTimes(1);
  });

  it("알림 목록을 다시 조회하는 동안 더 보기를 잠그고 완료하면 다음 페이지를 요청한다", async () => {
    const user = userEvent.setup();
    const fetchNextPage = vi.fn();
    const query = {
      data: [{ id: 1, courseKey: "course", title: "빈자리 알림", message: "여석이 생겼습니다." }],
      isLoading: false,
      error: null,
      fetchNextPage,
      hasNextPage: true,
      isFetchingNextPage: false,
    };
    mockUseNotifications.mockReturnValue({ ...query, isFetching: true } as unknown as ReturnType<typeof useNotifications>);

    const { rerender } = render(<NotificationList />);
    const busyButton = screen.getByRole("button", { name: "불러오는 중..." });
    expect(busyButton).toBeDisabled();
    await user.click(busyButton);
    expect(fetchNextPage).not.toHaveBeenCalled();

    mockUseNotifications.mockReturnValue({ ...query, isFetching: false } as unknown as ReturnType<typeof useNotifications>);
    rerender(<NotificationList />);
    await user.click(screen.getByRole("button", { name: "더 많은 내역 보기" }));

    expect(fetchNextPage).toHaveBeenCalledExactlyOnceWith({ cancelRefetch: false });
  });
});
