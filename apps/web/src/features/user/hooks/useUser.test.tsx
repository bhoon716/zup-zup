import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { AxiosError } from "axios";
import * as userApi from "@/features/user/api/user.api";
import { useCompleteOnboarding, useUpdateProfile, useUser } from "./useUser";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

const { mockSetAuthUser } = vi.hoisted(() => ({
  mockSetAuthUser: vi.fn(),
}));

vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: vi.fn(),
  logout: vi.fn(),
  withdraw: vi.fn(),
  updateProfile: vi.fn(),
  completeOnboarding: vi.fn(),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: Object.assign(vi.fn(), {
    getState: () => ({ setUser: mockSetAuthUser }),
  }),
}));

describe("useUser hook", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("내 정보 조회 성공 시 사용자 데이터를 반환한다", async () => {
    vi.mocked(userApi.getMyProfile).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: { id: 1, name: "홍길동" },
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual({ id: 1, name: "홍길동" });
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

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });

  it("내 정보 조회는 공통 세션 갱신 경로를 사용한다", async () => {
    vi.mocked(userApi.getMyProfile).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: null,
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    renderHook(() => useUser(), { wrapper });

    await waitFor(() => expect(userApi.getMyProfile).toHaveBeenCalledWith());
  });

  it("disabled 상태에서는 사용자 조회를 실행하지 않는다", async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    renderHook(() => useUser({ enabled: false }), { wrapper });

    expect(userApi.getMyProfile).not.toHaveBeenCalled();
  });

  it("401이 아닌 에러는 그대로 실패 상태로 전달한다", async () => {
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

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it("프로필 수정 성공 시 Query 캐시와 인증 스토어를 함께 갱신한다", async () => {
    const updatedUser = { id: 1, name: "수정된 사용자" };
    vi.mocked(userApi.updateProfile).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: updatedUser,
    } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUpdateProfile(), { wrapper });

    result.current.mutate({ name: "수정된 사용자" } as never);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(["user", "me"])).toEqual(updatedUser);
    expect(mockSetAuthUser).toHaveBeenCalledWith(updatedUser);
  });

  it("온보딩 완료 성공 시 Query 캐시와 인증 스토어를 함께 갱신한다", async () => {
    const onboardedUser = { id: 1, name: "사용자", onboardingCompleted: true };
    vi.mocked(userApi.completeOnboarding).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: onboardedUser,
    } as never);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCompleteOnboarding(), { wrapper });

    result.current.mutate({} as never);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(queryClient.getQueryData(["user", "me"])).toEqual(onboardedUser);
    expect(mockSetAuthUser).toHaveBeenCalledWith(onboardedUser);
  });
});
