import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import HomePage from "./page";

const { mockAuthState, mockUseDashboardSnapshot } = vi.hoisted(() => ({
  mockAuthState: {
    user: null as null | { id: number; email: string; name: string; role: string },
    isLoading: false,
  },
  mockUseDashboardSnapshot: vi.fn(),
}));

vi.mock("@/widgets/home/hooks/useDashboard", () => ({
  useDashboardSnapshot: (options: unknown) => mockUseDashboardSnapshot(options),
}));

vi.mock("@/widgets/home/dashboard", () => ({
  Dashboard: () => <div data-testid="dashboard" />,
}));

vi.mock("@/widgets/home/home-landing", () => ({
  HomeLanding: () => <div data-testid="home-landing" />,
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: typeof mockAuthState) => unknown) => selector(mockAuthState),
}));

describe("HomePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthState.user = null;
    mockAuthState.isLoading = false;
    mockUseDashboardSnapshot.mockReturnValue({
      data: null,
      isLoading: false,
      isError: false,
    });
  });

  it("게스트 상태에서는 인증 대시보드 요청을 비활성화한다", () => {
    render(<HomePage />);

    expect(mockUseDashboardSnapshot).toHaveBeenCalledWith({ enabled: false });
  });
});
