import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as reviewApi from '@/features/review/api/review.api';
import type { EmojiReviewResponse, ReviewCreateRequest, ReviewReactionRequest } from "@/shared/types/api";

const sortEmojiStats = (items: EmojiReviewResponse[]) =>
  [...items].sort((left, right) => {
    if (right.count !== left.count) {
      return right.count - left.count;
    }

    return left.emoji.localeCompare(right.emoji);
  });

export const useReviews = (courseKey: string, sort: string = "createdAt,desc") => {
  return useInfiniteQuery({
    queryKey: ["reviews", courseKey, sort],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await reviewApi.getReviews(courseKey, pageParam as number, 20, sort);
      const sliceData = response.data;
      return {
        content: sliceData.content,
        last: sliceData.last,
        number: sliceData.number,
      };
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return lastPage.number + 1;
    },
    initialPageParam: 0,
    enabled: !!courseKey,
  });
};

export const useCreateReview = (courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ReviewCreateRequest) => reviewApi.createReview(courseKey, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reviews", courseKey] });
      queryClient.invalidateQueries({ queryKey: ["course-detail", courseKey] });
      queryClient.invalidateQueries({ queryKey: ["courses"] });
    },
  });
};

export const useToggleReviewReaction = (courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reviewId, request }: { reviewId: number; request: ReviewReactionRequest }) =>
      reviewApi.toggleReviewReaction(reviewId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reviews", courseKey] });
    },
  });
};

/** 강의의 이모지 리뷰 통계를 실시간 조회합니다. */
export const useCourseEmojis = (courseKey: string) => {
  return useQuery({
    queryKey: ["course-emojis", courseKey],
    queryFn: async () => {
      const response = await reviewApi.getCourseEmojis(courseKey);
      return response.data;
    },
    enabled: !!courseKey,
  });
};

/** 이모지를 토글합니다. 성공 시 해당 강의 이모지 캐시를 자동 무효화합니다. */
export const useToggleCourseEmoji = (courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (emoji: string) => reviewApi.toggleCourseEmoji(courseKey, emoji),
    onMutate: async (emoji: string) => {
      await queryClient.cancelQueries({ queryKey: ["course-emojis", courseKey] });

      const previousEmojis = queryClient.getQueryData<EmojiReviewResponse[]>(["course-emojis", courseKey]);

      queryClient.setQueryData<EmojiReviewResponse[]>(["course-emojis", courseKey], (current = []) => {
        const next = new Map(current.map((item) => [item.emoji, { ...item }]));
        const existing = next.get(emoji);

        if (existing?.isMine) {
          const count = existing.count - 1;
          if (count <= 0) {
            next.delete(emoji);
          } else {
            next.set(emoji, { ...existing, count, isMine: false });
          }
        } else if (existing) {
          next.set(emoji, { ...existing, count: existing.count + 1, isMine: true });
        } else {
          next.set(emoji, { emoji, count: 1, isMine: true });
        }

        return sortEmojiStats([...next.values()].filter((item) => item.count > 0));
      });

      return { previousEmojis };
    },
    onError: (_error, _emoji, context) => {
      if (!context) return;

      queryClient.setQueryData(["course-emojis", courseKey], context.previousEmojis);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-emojis", courseKey] });
    },
  });
};
