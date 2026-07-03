import { render, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Providers, { getAppQueryClient } from "./providers";

const mockCheckSession = vi.fn();
const mockReplace = vi.fn();
const mockGetCookie = vi.fn();
const mockSetUser = vi.fn();

const mockState = {
  user: null,
  isLoading: true,
  checkSession: mockCheckSession,
  logout: vi.fn(),
  isLoginModalOpen: false,
  setLoginModalOpen: vi.fn(),
  setUser: mockSetUser,
};

vi.mock("@/shared/lib/firebase", () => ({
  getFirebaseApp: vi.fn(),
}));

vi.mock("@/widgets/auth/login-modal", () => ({
  LoginModal: () => null,
}));

vi.mock("next/navigation", () => ({
  usePathname: vi.fn(),
  useRouter: vi.fn(),
}));

vi.mock("@/shared/lib/cookie", () => ({
  getCookie: (name: string) => mockGetCookie(name),
  IS_LOGGED_IN_COOKIE_NAME: "is_logged_in",
}));

vi.mock("@/features/auth/store/useAuthStore", () => {
  const storeMock = (selector?: (state: typeof mockState) => unknown) => {
    return selector ? selector(storeMock.getState()) : storeMock.getState();
  };
  storeMock.getState = () => mockState;
  return {
    useAuthStore: storeMock,
  };
});

import { usePathname, useRouter } from "next/navigation";

describe("Providers", () => {
  const mockedUsePathname = vi.mocked(usePathname);
  const mockedUseRouter = vi.mocked(useRouter);

  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseRouter.mockReturnValue({ replace: mockReplace } as never);
    mockGetCookie.mockReturnValue("true");
  });

  it("is_logged_in 쿠키가 true일 때 세션 부트스트랩을 수행한다", async () => {
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(1));
    expect(mockSetUser).not.toHaveBeenCalled();
  });

  it("is_logged_in 쿠키가 없거나 false일 때 세션 부트스트랩을 건너뛰고 setUser(null)을 수행한다", async () => {
    mockGetCookie.mockReturnValue(undefined);
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockSetUser).toHaveBeenCalledWith(null));
    expect(mockCheckSession).not.toHaveBeenCalled();
  });

  it("브라우저에서는 QueryClient를 재사용한다", () => {
    const first = getAppQueryClient();
    const second = getAppQueryClient();

    expect(first).toBe(second);
  });
});
