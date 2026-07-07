import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as wishlistApi from "@/features/wishlist/api/wishlist.api";
import { useUser } from "@/features/user/hooks/useUser";
import { toast } from "sonner";
import { useToggleWishlist, useWishlist } from "./useWishlist";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { WishlistResponse } from "@/shared/types/api";

vi.mock("@/features/wishlist/api/wishlist.api", () => ({
  getMyWishlist: vi.fn(),
  toggleWishlist: vi.fn(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: vi.fn(),
}));

vi.mock("sonner", () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe("useWishlist hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("로그인 사용자일 때 찜 목록을 조회한다", async () => {
    vi.mocked(useUser).mockReturnValue({ data: { id: 1 } } as never);
    vi.mocked(wishlistApi.getMyWishlist).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: [
        {
          id: 1,
          userId: 1,
          courseKey: "CSE-101",
          courseName: "자료구조",
          professor: "김교수",
          classification: "전공",
          credits: "3",
          classTime: "월 09:00-10:15",
          capacity: 40,
          current: 30,
          available: 10,
          subjectCode: "CSE101",
          classNumber: "01",
          createdAt: "2026-01-01T00:00:00",
        },
      ],
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useWishlist(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(wishlistApi.getMyWishlist).toHaveBeenCalledTimes(1);
  });

  it("찜 토글 실패 시 낙관적 업데이트를 롤백한다", async () => {
    const initialWishlist: WishlistResponse[] = [
      {
        id: 1,
        userId: 1,
        courseKey: "CSE-101",
        courseName: "자료구조",
        professor: "김교수",
        classification: "전공",
        credits: "3",
        classTime: "월 09:00-10:15",
        capacity: 40,
        current: 30,
        available: 10,
        subjectCode: "CSE101",
        classNumber: "01",
        createdAt: "2026-01-01T00:00:00",
      },
    ];

    const mockedToggle = vi.mocked(wishlistApi.toggleWishlist);
    mockedToggle.mockRejectedValue({
      response: { data: { message: "찜 변경 실패" } },
    });

    const queryClient = createTestQueryClient();
    queryClient.setQueryData(["wishlist"], initialWishlist);
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useToggleWishlist(), { wrapper });

    act(() => {
      result.current.mutate("CSE-101");
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(queryClient.getQueryData(["wishlist"])).toEqual(initialWishlist);
    expect(toast.error).toHaveBeenCalledWith("찜 변경 실패");
  });

  it("찜 토글 성공 시 낙관적 업데이트를 유지하고 목록을 재조회한다", async () => {
    const initialWishlist: WishlistResponse[] = [
      {
        id: 1,
        userId: 1,
        courseKey: "CSE-101",
        courseName: "자료구조",
        professor: "김교수",
        classification: "전공",
        credits: "3",
        classTime: "월 09:00-10:15",
        capacity: 40,
        current: 30,
        available: 10,
        subjectCode: "CSE101",
        classNumber: "01",
        createdAt: "2026-01-01T00:00:00",
      },
    ];

    const mockedToggle = vi.mocked(wishlistApi.toggleWishlist);
    mockedToggle.mockResolvedValue({
      code: "SUCCESS",
      message: "찜 갱신 완료",
      data: { isWished: true },
    } as never);

    const queryClient = createTestQueryClient();
    queryClient.setQueryData(["wishlist"], initialWishlist);
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useToggleWishlist(), { wrapper });

    act(() => {
      result.current.mutate("CSE-201");
    });

    await waitFor(() => {
      const optimisticList = queryClient.getQueryData<WishlistResponse[]>(["wishlist"]);
      expect(optimisticList?.some((item) => item.courseKey === "CSE-201")).toBe(true);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(toast.success).toHaveBeenCalledWith("찜 갱신 완료");
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["wishlist"] });
  });
});
