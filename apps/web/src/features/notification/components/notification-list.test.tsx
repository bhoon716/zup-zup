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
});
