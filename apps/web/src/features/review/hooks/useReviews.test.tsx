import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, renderHook, waitFor } from "@testing-library/react";
import * as reviewApi from "@/features/review/api/review.api";
import { useCourseEmojis, useToggleCourseEmoji } from "./useReviews";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import type { EmojiReviewResponse } from "@/shared/types/api";

vi.mock("@/features/review/api/review.api", () => ({
  getCourseEmojis: vi.fn(),
  toggleCourseEmoji: vi.fn(),
}));

describe("이모지 리뷰 훅", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const COURSE_KEY = "TEST-COURSE";

  const MOCK_EMOJIS: EmojiReviewResponse[] = [
    { emoji: "👍", count: 3, isMine: true },
    { emoji: "🔥", count: 1, isMine: false },
    { emoji: "🎓", count: 0, isMine: false },
    { emoji: "📝", count: 0, isMine: false },
    { emoji: "😴", count: 0, isMine: false },
    { emoji: "🚨", count: 0, isMine: false },
  ];

  it("강의 이모지 통계를 성공적으로 불러온다", async () => {
    vi.mocked(reviewApi.getCourseEmojis).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: MOCK_EMOJIS,
    } as Awaited<ReturnType<typeof reviewApi.getCourseEmojis>>);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCourseEmojis(COURSE_KEY), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(6);
    expect(result.current.data![0].emoji).toBe("👍");
    expect(result.current.data![0].count).toBe(3);
    expect(result.current.data![0].isMine).toBe(true);
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
      await result.current.mutateAsync("👍");
    });

    expect(reviewApi.toggleCourseEmoji).toHaveBeenCalledWith(COURSE_KEY, "👍");
    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ["course-emojis", COURSE_KEY] })
    );
  });

  it("courseKey가 없으면 쿼리를 실행하지 않는다", () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useCourseEmojis(""), { wrapper });

    expect(result.current.fetchStatus).toBe("idle");
    expect(reviewApi.getCourseEmojis).not.toHaveBeenCalled();
  });
});
