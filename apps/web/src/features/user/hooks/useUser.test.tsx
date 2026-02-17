import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import { AxiosError } from "axios";
import * as userApi from "@/features/user/api/user.api";
import { useUser } from "./useUser";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/user/api/user.api", () => ({
  getMyProfile: vi.fn(),
  logout: vi.fn(),
  withdraw: vi.fn(),
  updateProfile: vi.fn(),
  completeOnboarding: vi.fn(),
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
});
