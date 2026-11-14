import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

import { Header } from "./header";

type MockUser = {
  id: number;
  email: string;
  name: string;
  role: "USER" | "ADMIN";
};

type MockAuthStore = {
  user: MockUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
};

type MockNavLinksProps = {
  isMobile?: boolean;
  isAdmin: boolean;
  isLoggedIn: boolean;
  onLinkClick?: () => void;
};

const {
  mockGetInbox,
  mockLogout,
  mockInstall,
  mockAuthStore,
  mockNavLinks,
} = vi.hoisted(() => ({
  mockGetInbox: vi.fn(),
  mockLogout: vi.fn(),
  mockInstall: vi.fn(),
  mockAuthStore: {
    user: null as MockUser | null,
    isAuthenticated: false,
    isLoading: false,
  } as MockAuthStore,
  mockNavLinks: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  usePathname: () => "/search",
}));

vi.mock("@/features/notification/api/inbox.api", () => ({
  getInbox: mockGetInbox,
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

vi.mock("./ui/nav-links", () => ({
  NavLinks: (props: MockNavLinksProps) => {
    mockNavLinks(props);
    return <nav data-testid="nav-links" />;
  },
}));

describe("Header", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthStore.user = null;
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.isLoading = false;
  });

  it("모바일 메뉴 버튼에 접근 가능한 이름을 제공한다", () => {
    render(<Header />);

    expect(screen.getByRole("button", { name: "메뉴 열기" })).toBeInTheDocument();
  });

  it("주요 메뉴와 헤더 액션을 별도 그룹으로 구분한다", () => {
    render(<Header />);

    expect(screen.getByRole("navigation", { name: "주요 메뉴" })).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "헤더 액션" })).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "사용자 메뉴" })).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "모바일 메뉴" })).toBeInTheDocument();
  });

  it("프로필을 확인하는 동안 공개 메뉴와 로그인 상태를 유지한다", () => {
    mockAuthStore.isLoading = true;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(expect.objectContaining({ isLoggedIn: false, isAdmin: false }));
    expect(screen.getByRole("link", { name: "로그인" })).toBeInTheDocument();
  });

  it("캐시된 사용자 정보만 있을 때는 로그인 전용 메뉴를 표시하지 않는다", () => {
    mockAuthStore.user = {
      id: 1,
      email: "user@test.com",
      name: "사용자",
      role: "ADMIN",
    };
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.isLoading = false;

    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoggedIn: false, isAdmin: false })
    );
    expect(screen.queryByRole("link", { name: "프로필" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "알림함" })).not.toBeInTheDocument();
  });

  it("서버 검증이 완료된 사용자는 프로필 링크와 관리자 메뉴를 볼 수 있다", () => {
    mockAuthStore.user = {
      id: 1,
      email: "admin@test.com",
      name: "관리자",
      role: "ADMIN",
    };
    mockAuthStore.isAuthenticated = true;
    mockGetInbox.mockResolvedValue({ content: [], number: 0, totalPages: 0, totalElements: 0 });

    render(<Header />, { wrapper: createQueryWrapper(createTestQueryClient()) });

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoggedIn: true, isAdmin: true })
    );
    expect(screen.getByRole("link", { name: "프로필" })).toHaveAttribute("href", "/profile");
    expect(screen.getByRole("button", { name: "알림함" })).toBeInTheDocument();
  });

  it("세션 검증이 끝난 게스트는 로그인 상태를 표시하지 않는다", () => {
    mockAuthStore.isLoading = false;
    render(<Header />);

    expect(mockNavLinks).toHaveBeenCalledWith(
      expect.objectContaining({ isLoggedIn: false })
    );
  });

});
