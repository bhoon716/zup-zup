import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import OnboardingPage from "./page";

const createDeferred = <T,>() => {
  let resolve!: (value: T | PromiseLike<T>) => void;
  const promise = new Promise<T>((promiseResolve) => {
    resolve = promiseResolve;
  });

  return { promise, resolve };
};

const {
  mockCompleteOnboarding,
  mockReplace,
  mockSubscribe,
  mockUnsubscribe,
  mockUseUser,
} = vi.hoisted(() => ({
  mockCompleteOnboarding: vi.fn(),
  mockReplace: vi.fn(),
  mockSubscribe: vi.fn(),
  mockUnsubscribe: vi.fn(),
  mockUseUser: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: () => new URLSearchParams(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useCompleteOnboarding: () => ({
    isPending: false,
    mutate: mockCompleteOnboarding,
  }),
  useUser: () => mockUseUser(),
}));

vi.mock("@/features/user/hooks/useWebPush", () => ({
  useWebPush: () => ({
    loading: false,
    subscribe: mockSubscribe,
    unsubscribe: mockUnsubscribe,
  }),
}));

vi.mock("sonner", () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

describe("OnboardingPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("ResizeObserver", class {
      disconnect() {}
      observe() {}
      unobserve() {}
    });
    mockUseUser.mockReturnValue({
      data: {
        discordId: null,
        email: "user@example.com",
        onboardingCompleted: false,
      },
      isLoading: false,
    });
    mockSubscribe.mockResolvedValue(true);
    mockUnsubscribe.mockResolvedValue(true);
    mockCompleteOnboarding.mockImplementation((
      _request: unknown,
      options: { onSuccess?: () => void } | undefined,
    ) => {
      options?.onSuccess?.();
    });
  });

  it("기본 활성화된 웹 푸시를 등록한 뒤 온보딩을 완료한다", async () => {
    const user = userEvent.setup();
    render(<OnboardingPage />);

    await user.type(screen.getByPlaceholderText("기기 이름 (예: 내 노트북, 스마트폰)"), "내 노트북");
    await user.click(screen.getByRole("button", { name: "완료" }));

    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockSubscribe).toHaveBeenCalledWith("내 노트북");
    expect(mockCompleteOnboarding).toHaveBeenCalledWith(
      expect.objectContaining({ webPushEnabled: true }),
      expect.any(Object),
    );
    expect(mockSubscribe.mock.invocationCallOrder[0])
      .toBeLessThan(mockCompleteOnboarding.mock.invocationCallOrder[0]);
    expect(mockReplace).toHaveBeenCalledWith("/");
  });

  it("웹 푸시 기기 등록에 실패하면 온보딩 완료와 이동을 실행하지 않는다", async () => {
    const user = userEvent.setup();
    mockSubscribe.mockResolvedValue(false);
    render(<OnboardingPage />);

    await user.type(screen.getByPlaceholderText("기기 이름 (예: 내 노트북, 스마트폰)"), "내 노트북");
    await user.click(screen.getByRole("button", { name: "완료" }));

    await waitFor(() => expect(mockSubscribe).toHaveBeenCalledWith("내 노트북"));
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
    expect(mockReplace).not.toHaveBeenCalledWith("/");
  });

  it("웹 푸시 등록 중 중복 제출을 무시한다", async () => {
    const user = userEvent.setup();
    const deferredSubscribe = createDeferred<boolean>();
    mockSubscribe.mockReturnValue(deferredSubscribe.promise);
    render(<OnboardingPage />);

    await user.type(screen.getByPlaceholderText("기기 이름 (예: 내 노트북, 스마트폰)"), "내 노트북");
    const form = screen.getByRole("button", { name: "완료" }).closest("form");
    expect(form).not.toBeNull();

    await act(async () => {
      fireEvent.submit(form!);
      fireEvent.submit(form!);
    });

    expect(mockSubscribe).toHaveBeenCalledTimes(1);
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "완료" })).toBeDisabled();

    await act(async () => {
      deferredSubscribe.resolve(true);
      await deferredSubscribe.promise;
    });

    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
  });
});
