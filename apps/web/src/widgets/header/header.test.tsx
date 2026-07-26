import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { MouseEvent } from "react";

import { Header } from "./header";
import { IS_LOGGED_IN_COOKIE_NAME } from "@/shared/lib/cookie";

type MockUser = {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
};

type MockAuthStore = {
  user: MockUser | null;
  isLoading: boolean;
  setLoginModalOpen: (open: boolean) => void;
};

type MockNavLinksProps = {
  isMobile?: boolean;
  isAdmin: boolean;
  isLoggedIn: boolean;
  isLoading?: boolean;
  onGuardedAction: (event: MouseEvent) => void;
  onLinkClick?: () => void;
};

type MockUserStatusProps = {
  user: MockUser | null | undefined;
  isLoading: boolean;
};

const {
  mockLogout,
  mockInstall,
  mockAuthStore,
  mockNavLinks,
  mockDesktopUser,
  mockHasMounted,
} = vi.hoisted(() => ({
  mockLogout: vi.fn(),
  mockInstall: vi.fn(),
  mockAuthStore: {
    user: null as MockUser | null,
    isLoading: false,
    setLoginModalOpen: vi.fn(),
  } as MockAuthStore,
  mockNavLinks: vi.fn(),
  mockDesktopUser: vi.fn(),
  mockHasMounted: { value: true },
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
  useAuthStore: (selector: (state: MockAuthStore) => unknown) => selector(mockAuthStore),
}));

vi.mock("@/shared/hooks/usePWAInstall", () => ({
  usePWAInstall: () => ({
    install: mockInstall,
    platform: "android",
  }),
}));

vi.mock("@/shared/hooks/useHasMounted", () => ({
  useHasMounted: () => mockHasMounted.value,
}));

vi.mock("./ui/nav-links", () => ({
  NavLinks: (props: MockNavLinksProps) => {
    mockNavLinks(props);
    return <nav data-testid="nav-links" />;
  },
}));

vi.mock("./ui/user-status", () => ({
  HeaderDesktopUser: (props: MockUserStatusProps) => {
    mockDesktopUser(props);
    return <div data-testid="desktop-user" />;
  },
  HeaderMobileUserStatus: () => <div data-testid="mobile-user" />,
}));

describe("Header", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasMounted.value = true;
    mockAuthStore.user = null;
    mockAuthStore.isLoading = false;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=; path=/; max-age=0; expires=Thu, 01 Jan 1970 00:00:00 UTC;`;
  });

  it("모바일 메뉴 버튼에 접근 가능한 이름을 제공한다", () => {
    render(<Header />);

    expect(screen.getByRole("button", { name: "메뉴 열기" })).toBeInTheDocument();
  });

  it("세션 로딩 여부는 로그인 힌트 쿠키가 아니라 인증 스토어만 따른다", () => {
    mockAuthStore.isLoading = true;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: true })
    );
  });

  it("인증 스토어 로딩이 끝나면 로그인 힌트 쿠키가 있어도 로딩 상태를 표시하지 않는다", () => {
    mockAuthStore.isLoading = false;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=true; path=/`;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: false })
    );
  });

  it("로그인 여부는 인증 스토어의 사용자만 따른다", () => {
    mockAuthStore.isLoading = false;
    document.cookie = `${IS_LOGGED_IN_COOKIE_NAME}=true; path=/`;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoggedIn: false })
    );
  });

  it("SSR과 첫 클라이언트 렌더에서는 인증 영역을 동일한 스켈레톤으로 유지한다", () => {
    mockHasMounted.value = false;

    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: true, isLoggedIn: false })
    );
    expect(mockDesktopUser).toHaveBeenCalledWith(
      expect.objectContaining({ isLoading: true, user: undefined })
    );
  });

});
