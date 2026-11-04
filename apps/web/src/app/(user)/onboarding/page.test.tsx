import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import OnboardingPage from "./page";
import { trackOnboardingStarted } from "@/shared/analytics/onboarding-events";

const createDeferred = <T,>() => {
  let resolve!: (value: T | PromiseLike<T>) => void;
  const promise = new Promise<T>((resolvePromise) => { resolve = resolvePromise; });
  return { promise, resolve };
};
const { mockCompleteOnboarding, mockReplace, mockSubscribe, mockUnsubscribe, mockUseUser } = vi.hoisted(() => ({
  mockCompleteOnboarding: vi.fn(), mockReplace: vi.fn(), mockSubscribe: vi.fn(),
  mockUnsubscribe: vi.fn(), mockUseUser: vi.fn(),
}));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace }), useSearchParams: () => new URLSearchParams(),
}));
vi.mock("@/features/user/hooks/useUser", () => ({
  useCompleteOnboarding: () => ({ isPending: false, mutate: mockCompleteOnboarding }), useUser: () => mockUseUser(),
}));
vi.mock("@/features/user/hooks/useWebPush", () => ({
  useWebPush: () => ({ loading: false, subscribe: mockSubscribe, unsubscribe: mockUnsubscribe }),
}));
vi.mock("sonner", () => ({ toast: { error: vi.fn(), success: vi.fn() } }));
vi.mock("@/shared/analytics/onboarding-events", () => ({ trackOnboardingStarted: vi.fn() }));

const pushSwitch = () => screen.getByRole("switch", { name: "웹 푸시 (현재 기기)" });
const deviceInput = () => screen.getByPlaceholderText("기기 이름 (예: 내 노트북, 스마트폰)");
const completeButton = () => screen.getByRole("button", { name: "완료" });
const registerButton = () => screen.getByRole("button", { name: "현재 기기 등록" });

describe("OnboardingPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("ResizeObserver", class { disconnect() {} observe() {} unobserve() {} });
    mockUseUser.mockReturnValue({
      data: { id: 1, discordId: null, email: "user@example.com", onboardingCompleted: false }, isLoading: false,
    });
    mockSubscribe.mockResolvedValue(true);
    mockUnsubscribe.mockResolvedValue(true);
    mockCompleteOnboarding.mockImplementation((
      _request: unknown, options: { onSuccess?: () => void; onSettled?: () => void },
    ) => { options.onSuccess?.(); options.onSettled?.(); });
  });

  it("로딩과 미인증 상태에서는 시작을 기록하지 않고 미완료 프로필이 준비되면 기록한다", () => {
    mockUseUser.mockReturnValue({ data: undefined, isLoading: true });
    const { rerender } = render(<OnboardingPage />);
    expect(trackOnboardingStarted).not.toHaveBeenCalled();
    mockUseUser.mockReturnValue({ data: null, isLoading: false });
    rerender(<OnboardingPage />);
    expect(trackOnboardingStarted).not.toHaveBeenCalled();
    mockUseUser.mockReturnValue({ data: { id: 1, email: "user@example.com", onboardingCompleted: false }, isLoading: false });
    rerender(<OnboardingPage />);
    expect(trackOnboardingStarted).toHaveBeenCalledWith(1);
  });

  it("이미 완료한 사용자에게는 시작 이벤트 없이 홈으로 이동한다", () => {
    mockUseUser.mockReturnValue({ data: { id: 1, onboardingCompleted: true }, isLoading: false });
    render(<OnboardingPage />);
    expect(trackOnboardingStarted).not.toHaveBeenCalled();
    expect(mockReplace).toHaveBeenCalledWith("/");
  });

  it("웹 푸시는 기본 off이며 구독이나 기기 이름 없이 이메일로 완료한다", async () => {
    const user = userEvent.setup();
    render(<OnboardingPage />);
    expect(pushSwitch()).not.toBeChecked();
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockSubscribe).not.toHaveBeenCalled();
    expect(mockUnsubscribe).not.toHaveBeenCalled();
    expect(mockCompleteOnboarding).toHaveBeenCalledWith(
      expect.objectContaining({ emailEnabled: true, webPushEnabled: false }), expect.any(Object),
    );
    expect(mockReplace).toHaveBeenCalledWith("/");
  });

  it("명시적 선택 후 구독이 성공해야 켜지고 완료 시 true로 저장한다", async () => {
    const user = userEvent.setup();
    const subscription = createDeferred<boolean>();
    mockSubscribe.mockReturnValueOnce(subscription.promise);
    render(<OnboardingPage />);
    await user.type(deviceInput(), "내 노트북");
    await user.click(pushSwitch());
    expect(pushSwitch()).not.toBeChecked();
    expect(completeButton()).toBeDisabled();
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
    await act(async () => { subscription.resolve(true); await subscription.promise; });
    expect(pushSwitch()).toBeChecked();
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockSubscribe).toHaveBeenCalledWith("내 노트북");
    expect(mockCompleteOnboarding.mock.calls[0][0].webPushEnabled).toBe(true);
  });

  it("푸시 실패 후 off로 이메일 완료가 가능하고 자동 재구독하지 않는다", async () => {
    const user = userEvent.setup();
    mockSubscribe.mockResolvedValue(false);
    render(<OnboardingPage />);
    await user.click(pushSwitch());
    await waitFor(() => expect(pushSwitch()).toBeEnabled());
    expect(pushSwitch()).not.toBeChecked();
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockSubscribe).toHaveBeenCalledTimes(1);
    expect(mockCompleteOnboarding.mock.calls[0][0].webPushEnabled).toBe(false);
  });

  it("기기 등록 버튼 실패 후 명시적 재시도가 성공해야 켠다", async () => {
    const user = userEvent.setup();
    mockSubscribe.mockResolvedValueOnce(false).mockResolvedValueOnce(true);
    render(<OnboardingPage />);
    await user.type(deviceInput(), "내 휴대폰");
    await user.click(registerButton());
    expect(pushSwitch()).not.toBeChecked();
    await user.click(registerButton());
    await waitFor(() => expect(pushSwitch()).toBeChecked());
    expect(mockSubscribe).toHaveBeenCalledTimes(2);
    expect(mockSubscribe).toHaveBeenLastCalledWith("내 휴대폰");
  });

  it("완료 직전 재등록 실패 시 true를 저장하지 않고 다음 완료에서 off를 저장한다", async () => {
    const user = userEvent.setup();
    mockSubscribe.mockResolvedValueOnce(true).mockResolvedValueOnce(false);
    render(<OnboardingPage />);
    await user.type(deviceInput(), "내 노트북");
    await user.click(pushSwitch());
    await user.click(completeButton());
    await waitFor(() => expect(pushSwitch()).not.toBeChecked());
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
    expect(mockReplace).not.toHaveBeenCalled();
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockCompleteOnboarding.mock.calls[0][0].webPushEnabled).toBe(false);
  });

  it("푸시를 다시 끄면 해제하고 완료 시 다시 켜지 않는다", async () => {
    const user = userEvent.setup();
    render(<OnboardingPage />);
    await user.type(deviceInput(), "내 노트북");
    await user.click(pushSwitch());
    await user.click(pushSwitch());
    expect(pushSwitch()).not.toBeChecked();
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockSubscribe).toHaveBeenCalledTimes(1);
    expect(mockUnsubscribe).toHaveBeenCalledTimes(1);
    expect(mockCompleteOnboarding.mock.calls[0][0].webPushEnabled).toBe(false);
  });

  it("등록 중 중복 클릭과 폼 제출을 무시한다", async () => {
    const user = userEvent.setup();
    const subscription = createDeferred<boolean>();
    mockSubscribe.mockReturnValue(subscription.promise);
    render(<OnboardingPage />);
    await user.type(deviceInput(), "내 노트북");
    await act(async () => {
      fireEvent.click(registerButton());
      fireEvent.click(registerButton());
      fireEvent.submit(completeButton().closest("form")!);
    });
    expect(mockSubscribe).toHaveBeenCalledTimes(1);
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
    await act(async () => { subscription.resolve(true); await subscription.promise; });
    expect(pushSwitch()).toBeChecked();
  });

  it("저장 응답까지 중복 제출을 막고 실패 후 재시도할 수 있다", async () => {
    const user = userEvent.setup();
    let settle!: () => void;
    mockCompleteOnboarding.mockImplementation((_request, options) => { settle = options.onSettled; });
    render(<OnboardingPage />);
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(completeButton()).toBeDisabled();
    await act(async () => { fireEvent.submit(completeButton().closest("form")!); });
    expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1);
    await act(async () => { settle(); });
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(2));
  });

  it("연결된 Discord는 유지하고 이메일과 푸시를 켜지 않는다", async () => {
    const user = userEvent.setup();
    mockUseUser.mockReturnValue({
      data: { discordId: "linked", email: "user@example.com", onboardingCompleted: false }, isLoading: false,
    });
    render(<OnboardingPage />);
    await user.click(document.getElementById("emailEnabled")!);
    await user.click(completeButton());
    await waitFor(() => expect(mockCompleteOnboarding).toHaveBeenCalledTimes(1));
    expect(mockCompleteOnboarding).toHaveBeenCalledWith(
      expect.objectContaining({ discordEnabled: true, emailEnabled: false, webPushEnabled: false }), expect.any(Object),
    );
    expect(mockSubscribe).not.toHaveBeenCalled();
  });

  it("이메일 변경 시 기존 인증 요구를 유지한다", async () => {
    const user = userEvent.setup();
    render(<OnboardingPage />);
    const email = screen.getByPlaceholderText("example@email.com");
    fireEvent.change(email, { target: { value: "other@example.com" } });
    await user.click(completeButton());
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("이메일 인증을 완료해주세요."));
    expect(mockCompleteOnboarding).not.toHaveBeenCalled();
  });
});
