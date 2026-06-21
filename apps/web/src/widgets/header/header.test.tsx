import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { Header } from "./header";

const { mockLogout, mockSetLoginModalOpen, mockInstall } = vi.hoisted(() => ({
  mockLogout: vi.fn(),
  mockSetLoginModalOpen: vi.fn(),
  mockInstall: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/search",
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useLogout: () => ({
    mutate: mockLogout,
    isPending: false,
  }),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: {
    user: null;
    isLoading: boolean;
    setLoginModalOpen: (open: boolean) => void;
  }) => unknown) =>
    selector({
      user: null,
      isLoading: false,
      setLoginModalOpen: mockSetLoginModalOpen,
    }),
}));

vi.mock("@/shared/hooks/usePWAInstall", () => ({
  usePWAInstall: () => ({
    install: mockInstall,
    platform: "android",
  }),
}));

vi.mock("@/shared/hooks/useHasMounted", () => ({
  useHasMounted: () => true,
}));

vi.mock("./ui/nav-links", () => ({
  NavLinks: () => <nav data-testid="nav-links" />,
}));

vi.mock("./ui/user-status", () => ({
  HeaderDesktopUser: () => <div data-testid="desktop-user" />,
  HeaderMobileUserStatus: () => <div data-testid="mobile-user" />,
}));

describe("Header", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("모바일 메뉴 버튼에 접근 가능한 이름을 제공한다", () => {
    render(<Header />);

    expect(screen.getByRole("button", { name: "메뉴 열기" })).toBeInTheDocument();
  });
});
