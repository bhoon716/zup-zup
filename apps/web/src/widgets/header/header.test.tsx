import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { Header } from "./header";
import { IS_LOGGED_IN_COOKIE_NAME } from "@/shared/lib/cookie";

const { mockLogout, mockSetLoginModalOpen, mockInstall, mockAuthStore, mockNavLinks } = vi.hoisted(() => ({
  mockLogout: vi.fn(),
  mockSetLoginModalOpen: vi.fn(),
  mockInstall: vi.fn(),
  mockAuthStore: {
    user: null as any,
    isLoading: false,
    setLoginModalOpen: vi.fn(),
  },
  mockNavLinks: vi.fn(),
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
  useAuthStore: (selector: (state: any) => unknown) => selector(mockAuthStore),
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
  NavLinks: (props: any) => {
    mockNavLinks(props);
    return <nav data-testid="nav-links" />;
  },
}));

vi.mock("./ui/user-status", () => ({
  HeaderDesktopUser: () => <div data-testid="desktop-user" />,
  HeaderMobileUserStatus: () => <div data-testid="mobile-user" />,
}));

describe("Header", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthStore.user = null;
    mockAuthStore.isLoading = false;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 UTC;`;
  });

  it("모바일 메뉴 버튼에 접근 가능한 이름을 제공한다", () => {
    render(<Header />);

    expect(screen.getByRole("button", { name: "메뉴 열기" })).toBeInTheDocument();
  });

  it("세션이 로딩 중이지만 로그인 힌트 쿠키가 없으면 NavLinks에 isLoading=false를 전달한다", () => {
    mockAuthStore.isLoading = true;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: false })
    );
  });

  it("세션이 로딩 중이고 로그인 힌트 쿠키가 존재하면 NavLinks에 isLoading=true를 전달한다", () => {
    mockAuthStore.isLoading = true;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=true; path=/`;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: true })
    );
  });

  it("세션 로딩이 완료되면 로그인 힌트 쿠키 여부와 관계없이 NavLinks에 isLoading=false를 전달한다", () => {
    mockAuthStore.isLoading = false;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=true; path=/`;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: false })
    );
  });

});
