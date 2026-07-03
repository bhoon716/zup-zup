import { render, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Providers, { getAppQueryClient } from "./providers";

const mockCheckSession = vi.fn();
const mockReplace = vi.fn();

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

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector?: (state: {
    user: null;
    isLoading: boolean;
    checkSession: typeof mockCheckSession;
    logout: () => void;
    isLoginModalOpen: boolean;
    setLoginModalOpen: () => void;
  }) => unknown) => {
    const state = {
      user: null,
      isLoading: true,
      checkSession: mockCheckSession,
      logout: vi.fn(),
      isLoginModalOpen: false,
      setLoginModalOpen: vi.fn(),
    };

    return selector ? selector(state) : state;
  },
}));

import { usePathname, useRouter } from "next/navigation";

describe("Providers", () => {
  const mockedUsePathname = vi.mocked(usePathname);
  const mockedUseRouter = vi.mocked(useRouter);

  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseRouter.mockReturnValue({ replace: mockReplace } as never);
  });

  it("search 페이지에서도 세션 부트스트랩을 수행한다", async () => {
    mockedUsePathname.mockReturnValue("/search");

    render(
      <Providers>
        <div>child</div>
      </Providers>
    );

    await waitFor(() => expect(mockCheckSession).toHaveBeenCalledTimes(1));
  });

  it("브라우저에서는 QueryClient를 재사용한다", () => {
    const first = getAppQueryClient();
    const second = getAppQueryClient();

    expect(first).toBe(second);
  });
});
