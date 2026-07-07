import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import HomePage from "./page";

const { mockSetUser } = vi.hoisted(() => ({
  mockSetUser: vi.fn(),
}));

vi.mock("@/widgets/home/hooks/useDashboard", () => ({
  useDashboardSnapshot: () => ({
    data: null,
    isLoading: false,
    isError: false,
  }),
}));

vi.mock("@/widgets/home/dashboard", () => ({
  Dashboard: () => <div data-testid="dashboard" />,
}));

vi.mock("@/widgets/home/home-landing", () => ({
  HomeLanding: () => <div data-testid="home-landing" />,
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setUser: (user: null) => void }) => unknown) =>
    selector({
      setUser: mockSetUser,
    }),
}));

describe("HomePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("게스트 상태에서는 전역 auth 상태를 지우지 않는다", () => {
    render(<HomePage />);

    expect(mockSetUser).not.toHaveBeenCalled();
  });
});
