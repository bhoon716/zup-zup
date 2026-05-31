import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";

import * as reviewApi from "@/features/review/api/review.api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { Course, EmojiReviewResponse, ReviewResponse } from "@/shared/types/api";
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
    likeCount: 2,
    dislikeCount: 0,
    isMine: true,
    createdAt: "2024-03-07T00:00:00",
    updatedAt: "2024-03-07T00:00:00",
  };

  const MOCK_COURSE: Course = {
    courseKey: COURSE_KEY,
    subjectCode: "CSE101",
    name: "자료구조",
    classNumber: "01",
    averageRating: 4,
    reviewCount: 10,
    isReviewed: false,
  };

  const MOCK_EMOJIS: EmojiReviewResponse[] = [
    { emoji: "👍", count: 3, isMine: true },
    { emoji: "😂", count: 1, isMine: false },
  ];

  it("강의 리뷰를 단건으로 불러온다", async () => {
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
    expect(result.current.data?.rating).toBe(5);
  });

  it("리뷰를 생성하고 관련 캐시와 쿼리를 갱신한다", async () => {
    vi.mocked(reviewApi.createReview).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_REVIEW,
    } as Awaited<ReturnType<typeof reviewApi.createReview>>);

    const queryClient = createTestQueryClient();
    queryClient.setQueryData(["course-detail", COURSE_KEY], MOCK_COURSE);
    queryClient.setQueryData(["courses", { name: "자료" }], {
      pages: [
        {
          content: [MOCK_COURSE],
          last: true,
          number: 0,
        },
      ],
      pageParams: [0],
    });
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCreateReview(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ rating: 5 });
    });

    expect(reviewApi.createReview).toHaveBeenCalledWith(COURSE_KEY, { rating: 5 });
    expect(queryClient.getQueryData(["review", COURSE_KEY])).toEqual(MOCK_REVIEW);
    const updatedCourse = queryClient.getQueryData<Course>(["course-detail", COURSE_KEY]);
    expect(updatedCourse?.averageRating).toBeCloseTo(4.1, 1);
    expect(updatedCourse?.reviewCount).toBe(11);
    expect(updatedCourse?.isReviewed).toBe(true);

    const updatedCourses = queryClient.getQueryData<{
      pages: Array<{ content: Course[]; last: boolean; number: number }>;
      pageParams: number[];
    }>(["courses", { name: "자료" }]);
    expect(updatedCourses?.pages[0].content[0].averageRating).toBeCloseTo(4.1, 1);
    expect(updatedCourses?.pages[0].content[0].reviewCount).toBe(11);
    expect(updatedCourses?.pages[0].content[0].isReviewed).toBe(true);
  });

  it("리뷰를 수정하고 관련 캐시와 쿼리를 갱신한다", async () => {
    vi.mocked(reviewApi.updateReview).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_REVIEW,
    } as Awaited<ReturnType<typeof reviewApi.updateReview>>);

    const queryClient = createTestQueryClient();
    queryClient.setQueryData(["review", COURSE_KEY], {
      ...MOCK_REVIEW,
      rating: 3,
    });
    queryClient.setQueryData(["course-detail", COURSE_KEY], MOCK_COURSE);
    queryClient.setQueryData(["courses", { name: "자료" }], {
      pages: [
        {
          content: [{ ...MOCK_COURSE }],
          last: true,
          number: 0,
        },
      ],
      pageParams: [0],
    });
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useUpdateReview(COURSE_KEY), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ reviewId: 1, request: { rating: 4 } });
    });

    expect(reviewApi.updateReview).toHaveBeenCalledWith(1, { rating: 4 });
    expect(queryClient.getQueryData(["review", COURSE_KEY])).toEqual(MOCK_REVIEW);
    const updatedCourse = queryClient.getQueryData<Course>(["course-detail", COURSE_KEY]);
    expect(updatedCourse?.averageRating).toBeCloseTo(4.2, 1);
    expect(updatedCourse?.reviewCount).toBe(10);
    expect(updatedCourse?.isReviewed).toBe(true);

    const updatedCourses = queryClient.getQueryData<{
      pages: Array<{ content: Course[]; last: boolean; number: number }>;
      pageParams: number[];
    }>(["courses", { name: "자료" }]);
    expect(updatedCourses?.pages[0].content[0].averageRating).toBeCloseTo(4.2, 1);
    expect(updatedCourses?.pages[0].content[0].reviewCount).toBe(10);
    expect(updatedCourses?.pages[0].content[0].isReviewed).toBe(true);
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
    expect(invalidateSpy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ["review", COURSE_KEY] }));
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
