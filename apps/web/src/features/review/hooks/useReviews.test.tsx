import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as reviewApi from "@/features/review/api/review.api";
import { useReviews, useCreateReview, useToggleReviewReaction } from "./useReviews";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/review/api/review.api", () => ({
  getReviews: vi.fn(),
  createReview: vi.fn(),
  toggleReviewReaction: vi.fn(),
}));

describe("useReviews hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const COURSE_KEY = "TEST-COURSE";

  it("리뷰 목록을 성공적으로 불러온다", async () => {
    const mockedGetReviews = vi.mocked(reviewApi.getReviews);
    mockedGetReviews.mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
        content: [
          {
            id: 1,
            content: "좋은 강의입니다.",
            rating: 5,
            userId: 1,
            nickname: "학생1",
            createdAt: "2024-03-07T00:00:00",
          },
        ],
        last: true,
        number: 0,
      },
    } as any);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useReviews(COURSE_KEY), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.pages[0].content).toHaveLength(1);
    expect(result.current.data?.pages[0].content[0].content).toBe("좋은 강의입니다.");
  });

  it("리뷰를 생성하고 관련 쿼리를 무효화한다", async () => {
    const mockedCreateReview = vi.mocked(reviewApi.createReview);
    mockedCreateReview.mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: { id: 2, content: "새 리뷰" },
    } as any);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    
    const { result } = renderHook(() => useCreateReview(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ content: "새 리뷰", rating: 4 });
    });

    expect(mockedCreateReview).toHaveBeenCalledWith(COURSE_KEY, { content: "새 리뷰", rating: 4 });
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["reviews", COURSE_KEY] }));
  });

  it("리뷰 반응(좋아요/싫어요)을 토글한다", async () => {
    const mockedToggleReaction = vi.mocked(reviewApi.toggleReviewReaction);
    mockedToggleReaction.mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: null,
    } as any);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    
    const { result } = renderHook(() => useToggleReviewReaction(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ reviewId: 1, request: { reactionType: "LIKE" } });
    });

    expect(mockedToggleReaction).toHaveBeenCalledWith(1, { reactionType: "LIKE" });
  });
});
