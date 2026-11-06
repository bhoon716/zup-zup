import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import { AxiosError } from "axios";
import * as userApi from "@/features/user/api/user.api";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { reportClientError } from "@/shared/telemetry/client-error";
import { useCompleteOnboarding, useUpdateProfile, useUser } from "./useUser";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import { trackOnboardingCompleted, trackOnboardingFailed } from "@/shared/analytics/onboarding-events";

vi.mock("@/shared/analytics/onboarding-events", () => ({
  trackOnboardingCompleted: vi.fn(), trackOnboardingFailed: vi.fn(),
}));

vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: vi.fn(),
  logout: vi.fn(),
  withdraw: vi.fn(),
  updateProfile: vi.fn(),
  completeOnboarding: vi.fn(),
}));

vi.mock("@/shared/telemetry/client-error", () => ({ reportClientError: vi.fn() }));

const user = {
  id: 1, name: "홍길동", email: "user@example.invalid", role: "USER" as const,
  emailEnabled: true, webPushEnabled: false, discordEnabled: false, onboardingCompleted: true,
};

describe("useUser hook", () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    useAuthStore.setState({ hasCheckedSession: false, lastProfileCheckAt: 0, isLoading: true });
    vi.resetAllMocks();
  });

  it("내 정보 조회 성공 시 사용자 데이터를 반환한다", async () => {
    vi.mocked(userApi.getMyProfile).mockResolvedValue(user as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(user);
    expect(useAuthStore.getState().user).toEqual(user);
    expect(queryClient.getQueryData(["user", "me"])).toBeUndefined();
  });

  it("401 응답은 예외 대신 null 사용자로 처리한다", async () => {
    vi.mocked(userApi.getMyProfile).mockRejectedValue(
      new AxiosError("unauthorized", undefined, undefined, undefined, {
        status: 401,
        statusText: "Unauthorized",
        headers: {},
        config: { headers: {} } as never,
        data: { message: "인증 필요" },
      })
    );

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBeNull();
  });

  it("내 정보 조회는 공통 세션 갱신 경로를 사용한다", async () => {
    vi.mocked(userApi.getMyProfile).mockResolvedValue(null as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(userApi.getMyProfile).toHaveBeenCalledWith({}));
  });

  it("disabled 상태에서는 사용자 조회를 실행하지 않는다", async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    renderHook(() => useUser({ enabled: false }), { wrapper });

    expect(userApi.getMyProfile).not.toHaveBeenCalled();
  });

  it("401이 아닌 오류로 미검증 사용자를 인증 완료로 승격하지 않는다", async () => {
    vi.mocked(userApi.getMyProfile).mockRejectedValue(
      new AxiosError("server error", undefined, undefined, undefined, {
        status: 500,
        statusText: "Server Error",
        headers: {},
        config: { headers: {} } as never,
        data: { message: "서버 오류" },
      })
    );

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toBeNull();
    expect(reportClientError).toHaveBeenCalledTimes(1);
  });

  it("프로필 수정 성공 시 인증 스토어의 사용자 한 사본을 갱신한다", async () => {
    const updatedUser = { id: 1, name: "수정된 사용자" };
    vi.mocked(userApi.updateProfile).mockResolvedValue(updatedUser as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUpdateProfile(), { wrapper });

    result.current.mutate({ name: "수정된 사용자" } as never);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(["user", "me"])).toBeUndefined();
    expect(useAuthStore.getState().user).toEqual(updatedUser);
  });

  it("온보딩 완료 성공 시 인증 스토어의 사용자 한 사본을 갱신한다", async () => {
    const onboardedUser = { id: 1, name: "사용자", onboardingCompleted: true };
    vi.mocked(userApi.completeOnboarding).mockResolvedValue(onboardedUser as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCompleteOnboarding(), { wrapper });

    result.current.mutate({} as never);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(["user", "me"])).toBeUndefined();
    expect(useAuthStore.getState().user).toEqual(onboardedUser);
  });

  it("버튼 제출과 재렌더만으로 완료를 기록하지 않고 언마운트 후에도 저장 성공을 한 번 기록한다", async () => {
    let resolve!: (value: never) => void;
    vi.mocked(userApi.completeOnboarding).mockReturnValueOnce(new Promise((done) => { resolve = done; }));
    const wrapper = createQueryWrapper(createTestQueryClient());
    const { result, rerender, unmount } = renderHook(() => useCompleteOnboarding(1), { wrapper });
    act(() => result.current.mutate({} as never));
    rerender();
    expect(trackOnboardingCompleted).not.toHaveBeenCalled();
    expect(trackOnboardingFailed).not.toHaveBeenCalled();
    unmount();
    await act(async () => resolve({ ...user, onboardingCompleted: true } as never));
    await waitFor(() => expect(trackOnboardingCompleted).toHaveBeenCalledExactlyOnceWith(1));
    expect(trackOnboardingFailed).not.toHaveBeenCalled();
  });

  it("최종 저장 실패를 한 번 기록하고 수동 재시도 성공 때 완료를 기록한다", async () => {
    const error = new AxiosError("private token");
    vi.mocked(userApi.completeOnboarding).mockRejectedValueOnce(error).mockResolvedValueOnce({
      ...user, onboardingCompleted: true,
    } as never);
    const wrapper = createQueryWrapper(createTestQueryClient());
    const { result } = renderHook(() => useCompleteOnboarding(1), { wrapper });
    act(() => result.current.mutate({} as never));
    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(trackOnboardingFailed).toHaveBeenCalledExactlyOnceWith(1, error);
    expect(trackOnboardingCompleted).not.toHaveBeenCalled();
    act(() => result.current.mutate({} as never));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(trackOnboardingFailed).toHaveBeenCalledTimes(1);
    expect(trackOnboardingCompleted).toHaveBeenCalledExactlyOnceWith(1);
  });
  it("복원된 관리자 정보는 서버 검증 전 조회 소비자에게 노출하지 않는다", async () => {
    const admin = { ...user, role: "ADMIN" };
    useAuthStore.setState({ user: admin as never, isAuthenticated: false, isLoading: false });
    let finish!: (value: never) => void;
    vi.mocked(userApi.getMyProfile).mockReturnValueOnce(new Promise(resolve => { finish = resolve; }));
    const { result } = renderHook(() => useUser());
    expect(result.current.data).toBeNull();
    expect(result.current.isLoading).toBe(true);
    await act(async () => finish(admin as never));
    expect(result.current.data).toEqual(admin);
    expect(result.current.isLoading).toBe(false);
  });

  it("여러 조회 소비자는 진행 중 요청과 5분 내 확인 결과를 공유한다", async () => {
    vi.mocked(userApi.getMyProfile).mockResolvedValue(user as never);
    const first = renderHook(() => { useUser(); return useUser(); });
    await waitFor(() => expect(first.result.current.data).toEqual(user));
    expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);
    first.unmount();
    const second = renderHook(() => useUser());
    expect(second.result.current.data).toEqual(user);
    expect(userApi.getMyProfile).toHaveBeenCalledTimes(1);
    second.unmount();
    useAuthStore.setState({ lastProfileCheckAt: Date.now() - 5 * 60 * 1000 - 1 });
    renderHook(() => useUser());
    await waitFor(() => expect(userApi.getMyProfile).toHaveBeenCalledTimes(2));
  });

  it.each(["profile", "onboarding"] as const)("로그아웃 뒤 늦은 저장 응답은 사용자를 복구하지 않는다 (%s)", async mutation => {
    useAuthStore.getState().setUser(user);
    let finish!: (value: never) => void;
    const apiMutation = mutation === "profile" ? userApi.updateProfile : userApi.completeOnboarding;
    vi.mocked(apiMutation).mockReturnValueOnce(new Promise(resolve => { finish = resolve; }));
    const wrapper = createQueryWrapper(createTestQueryClient());
    const { result } = renderHook(() => ({ profile: useUpdateProfile(), onboarding: useCompleteOnboarding() }), { wrapper });
    act(() => result.current[mutation].mutate({} as never));
    await waitFor(() => expect(apiMutation).toHaveBeenCalledTimes(1));
    act(() => useAuthStore.getState().logout());
    await act(async () => finish(user as never));
    await waitFor(() => expect(result.current[mutation].isSuccess).toBe(true));
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });

  it.each(["profile", "onboarding"] as const)("새 계정 확인 뒤 이전 계정의 저장 응답은 무시한다 (%s)", async mutation => {
    useAuthStore.getState().setUser(user);
    let finish!: (value: never) => void;
    const apiMutation = mutation === "profile" ? userApi.updateProfile : userApi.completeOnboarding;
    vi.mocked(apiMutation).mockReturnValueOnce(new Promise(resolve => { finish = resolve; }));
    const wrapper = createQueryWrapper(createTestQueryClient());
    const { result } = renderHook(() => ({ profile: useUpdateProfile(), onboarding: useCompleteOnboarding() }), { wrapper });
    act(() => result.current[mutation].mutate({} as never));
    await waitFor(() => expect(apiMutation).toHaveBeenCalledTimes(1));
    const nextUser = { ...user, id: 2, name: "새 계정" };
    vi.mocked(userApi.getMyProfile).mockResolvedValueOnce(nextUser as never);
    await act(async () => { await useAuthStore.getState().refreshUser(); });
    await act(async () => finish(user as never));
    await waitFor(() => expect(result.current[mutation].isSuccess).toBe(true));
    expect(useAuthStore.getState().user).toEqual(nextUser);
  });

});
