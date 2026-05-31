import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";

import * as reviewApi from "@/features/review/api/review.api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { EmojiReviewResponse, ReviewResponse } from "@/shared/types/api";
import { useCourseEmojis, useCreateReview, useReviews, useToggleCourseEmoji, useToggleReviewReaction, useUpdateReview } from "./useReviews";

vi.mock("@/features/review/api/review.api", () => ({
  getReviews: vi.fn(),
  createReview: vi.fn(),
  updateReview: vi.fn(),
  toggleReviewReaction: vi.fn(),
  getCourseEmojis: vi.fn(),
  toggleCourseEmoji: vi.fn(),
}));

describe("강의 리뷰 훅", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const COURSE_KEY = "TEST-COURSE";

  const MOCK_REVIEW: ReviewResponse = {
    id: 1,
    courseKey: COURSE_KEY,
    rating: 5,
    content: null,
    likeCount: 2,
    dislikeCount: 0,
    isMine: true,
    createdAt: "2024-03-07T00:00:00",
    updatedAt: "2024-03-07T00:00:00",
  };

  const MOCK_EMOJIS: EmojiReviewResponse[] = [
    { emoji: "👍", count: 3, isMine: true },
    { emoji: "😂", count: 1, isMine: false },
  ];

  it("강의 리뷰 목록을 성공적으로 불러온다", async () => {
    vi.mocked(reviewApi.getReviews).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
        content: [MOCK_REVIEW],
        last: true,
        number: 0,
      },
    } as Awaited<ReturnType<typeof reviewApi.getReviews>>);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useReviews(COURSE_KEY), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.pages[0].content).toHaveLength(1);
    expect(result.current.data?.pages[0].content[0].rating).toBe(5);
  });

  it("리뷰를 생성하고 관련 쿼리를 무효화한다", async () => {
    vi.mocked(reviewApi.createReview).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_REVIEW,
    } as Awaited<ReturnType<typeof reviewApi.createReview>>);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCreateReview(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ rating: 5 });
    });

    expect(reviewApi.createReview).toHaveBeenCalledWith(COURSE_KEY, { rating: 5 });
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["reviews", COURSE_KEY] }));
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["course-detail", COURSE_KEY] }));
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["courses"] }));
  });

  it("리뷰를 수정하고 관련 쿼리를 무효화한다", async () => {
    vi.mocked(reviewApi.updateReview).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_REVIEW,
    } as Awaited<ReturnType<typeof reviewApi.updateReview>>);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUpdateReview(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ reviewId: 1, request: { rating: 4 } });
    });

    expect(reviewApi.updateReview).toHaveBeenCalledWith(1, { rating: 4 });
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["reviews", COURSE_KEY] }));
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["course-detail", COURSE_KEY] }));
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["courses"] }));
  });

  it("리뷰 반응(좋아요/싫어요)을 토글한다", async () => {
    vi.mocked(reviewApi.toggleReviewReaction).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: undefined,
    } as Awaited<ReturnType<typeof reviewApi.toggleReviewReaction>>);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useToggleReviewReaction(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ reviewId: 1, request: { reactionType: "LIKE" } });
    });

    expect(reviewApi.toggleReviewReaction).toHaveBeenCalledWith(1, { reactionType: "LIKE" });
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["reviews", COURSE_KEY] }));
  });

  it("이모지 통계를 성공적으로 불러온다", async () => {
    vi.mocked(reviewApi.getCourseEmojis).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_EMOJIS,
    } as Awaited<ReturnType<typeof reviewApi.getCourseEmojis>>);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCourseEmojis(COURSE_KEY), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(2);
    expect(result.current.data?.[1].emoji).toBe("😂");
  });

  it("이모지 토글 후 캐시를 무효화한다", async () => {
    vi.mocked(reviewApi.toggleCourseEmoji).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: undefined,
    } as Awaited<ReturnType<typeof reviewApi.toggleCourseEmoji>>);

    const queryClient = createTestQueryClient();
    const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useToggleCourseEmoji(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync("😂");
    });

    expect(reviewApi.toggleCourseEmoji).toHaveBeenCalledWith(COURSE_KEY, "😂");
    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ["course-emojis", COURSE_KEY], refetchType: "none" })
    );
  });

  it("이모지 토글 시 캐시를 즉시 반영하고 0개가 되면 제거한다", async () => {
    vi.mocked(reviewApi.toggleCourseEmoji).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: undefined,
    } as Awaited<ReturnType<typeof reviewApi.toggleCourseEmoji>>);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    queryClient.setQueryData(["course-emojis", COURSE_KEY], [
      { emoji: "👍", count: 3, isMine: true },
      { emoji: "😂", count: 1, isMine: false },
      { emoji: "🥹", count: 1, isMine: true },
    ]);

    const { result } = renderHook(() => useToggleCourseEmoji(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync("😂");
    });

    expect(queryClient.getQueryData(["course-emojis", COURSE_KEY])).toEqual([
      { emoji: "👍", count: 3, isMine: true },
      { emoji: "😂", count: 2, isMine: true },
      { emoji: "🥹", count: 1, isMine: true },
    ]);

    await act(async () => {
      await result.current.mutateAsync("🥹");
    });

    expect(queryClient.getQueryData(["course-emojis", COURSE_KEY])).toEqual([
      { emoji: "👍", count: 3, isMine: true },
      { emoji: "😂", count: 2, isMine: true },
    ]);
  });
});
