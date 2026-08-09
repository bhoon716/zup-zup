import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import HomePage from "./page";

const { mockAuthState, mockUseDashboardSnapshot } = vi.hoisted(() => ({
  mockAuthState: {
    user: null as null | { id: number; email: string; name: string; role: string },
    isAuthenticated: false,
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
    mockAuthState.isAuthenticated = false;
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

  it("복원된 미검증 사용자가 있어도 인증 대시보드 요청을 비활성화한다", () => {
    mockAuthState.user = {
      id: 1,
      email: "restored@test.com",
      name: "복원 사용자",
      role: "USER",
    };

    render(<HomePage />);

    expect(mockUseDashboardSnapshot).toHaveBeenCalledWith({ enabled: false });
  });

  it("서버 검증된 사용자에게는 인증 대시보드 요청을 활성화한다", () => {
    mockAuthState.user = {
      id: 1,
      email: "verified@test.com",
      name: "검증 사용자",
      role: "USER",
    };
    mockAuthState.isAuthenticated = true;

    render(<HomePage />);

    expect(mockUseDashboardSnapshot).toHaveBeenCalledWith({ enabled: true });
  });
});
