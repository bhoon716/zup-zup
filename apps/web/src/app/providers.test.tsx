import { render, waitFor } from "@testing-library/react";
import { useQuery } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Providers, { getAppQueryClient } from "./providers";

const {
  mockCheckSession,
  mockReplace,
  mockGetCookie,
  mockSetUser,
  mockLogout,
  mockRegisterAuthFailureHandler,
  mockReportClientError,
  mockState,
} = vi.hoisted(() => {
  const mockCheckSession = vi.fn();
  const mockReplace = vi.fn();
  const mockGetCookie = vi.fn();
  const mockSetUser = vi.fn();
  const mockLogout = vi.fn();
  const mockRegisterAuthFailureHandler = vi.fn();
  const mockReportClientError = vi.fn();

  const mockState = {
    user: null as null | {
      id: number;
      email: string;
      name: string;
      role: string;
      onboardingCompleted?: boolean;
    },
    isAuthenticated: false,
    isLoading: true,
    checkSession: mockCheckSession,
    logout: mockLogout,
    isLoginModalOpen: false,
    setLoginModalOpen: vi.fn(),
    setUser: mockSetUser,
  };

  return {
    mockCheckSession,
    mockReplace,
    mockGetCookie,
    mockSetUser,
    mockLogout,
    mockRegisterAuthFailureHandler,
    mockReportClientError,
    mockState,
  };
});

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

vi.mock("@/shared/api/client", () => ({
  registerAuthFailureHandler: (handler: () => void) => mockRegisterAuthFailureHandler(handler),
}));

vi.mock("@/shared/telemetry/client-error", () => ({
  reportClientError: mockReportClientError,
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
    mockState.user = null;
    mockState.isAuthenticated = false;
    mockedUseRouter.mockReturnValue({ replace: mockReplace } as never);
    mockGetCookie.mockReturnValue("true");
    getAppQueryClient().clear();
  });

  it("앱 시작 시 auth 실패 핸들러를 등록한다", async () => {
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockRegisterAuthFailureHandler).toHaveBeenCalledTimes(1));

    const handler = mockRegisterAuthFailureHandler.mock.calls[0]?.[0] as (() => void) | undefined;
    handler?.();
    expect(mockLogout).toHaveBeenCalledTimes(1);
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

  it("로그인 힌트 쿠키가 없어도 저장된 사용자가 있으면 세션을 재검증한다", async () => {
    mockGetCookie.mockReturnValue(undefined);
    mockState.user = {
      id: 1,
      email: "user@test.com",
      name: "사용자",
      role: "USER",
    };
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(1));
    expect(mockSetUser).not.toHaveBeenCalled();
  });

  it("복원된 미검증 사용자는 window focus 시 세션을 다시 검증한다", async () => {
    mockGetCookie.mockReturnValue(undefined);
    mockState.user = {
      id: 1,
      email: "user@test.com",
      name: "사용자",
      role: "USER",
    };
    mockState.isAuthenticated = false;
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(1));
    window.dispatchEvent(new Event("focus"));
    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(2));
  });

  it("복원된 미검증 사용자의 온보딩 상태만으로 강제 이동하지 않는다", async () => {
    mockState.user = {
      id: 1,
      email: "restored@test.com",
      name: "복원 사용자",
      role: "USER",
      onboardingCompleted: false,
    };
    mockState.isAuthenticated = false;
    mockState.isLoading = false;
    mockedUsePathname.mockReturnValue("/");

    render(
      <Providers>
        <div>public child</div>
      </Providers>
    );

    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(1));
    expect(mockReplace).not.toHaveBeenCalled();
  });

  it("서버 검증된 사용자가 온보딩 미완료이면 온보딩으로 이동한다", async () => {
    mockState.user = {
      id: 1,
      email: "verified@test.com",
      name: "검증 사용자",
      role: "USER",
      onboardingCompleted: false,
    };
    mockState.isAuthenticated = true;
    mockState.isLoading = false;
    mockedUsePathname.mockReturnValue("/");

    render(
      <Providers>
        <div>protected child</div>
      </Providers>
    );

    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith("/onboarding"));
  });

  it("브라우저에서는 QueryClient를 재사용한다", () => {
    const first = getAppQueryClient();
    const second = getAppQueryClient();

    expect(first).toBe(second);
  });

  it("QueryCache 오류를 중앙 클라이언트 오류 추적기로 전달한다", async () => {
    mockedUsePathname.mockReturnValue("/search");

    function FailingQuery() {
      useQuery({
        queryKey: ["providers-telemetry-test"],
        queryFn: async () => {
          throw new Error("query failure");
        },
        retry: false,
      });
      return null;
    }

    render(
      <Providers>
        <FailingQuery />
      </Providers>,
    );

    await waitFor(() => {
      expect(mockReportClientError).toHaveBeenCalledWith(
        expect.any(Error),
        { source: "react-query", operation: "query" },
      );
    });
  });
});
