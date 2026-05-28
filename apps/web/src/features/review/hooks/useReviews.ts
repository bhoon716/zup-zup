import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import * as reviewApi from '@/features/review/api/review.api';
import type { ReviewCreateRequest, ReviewReactionRequest } from "@/shared/types/api";

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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["course-emojis", courseKey] });
    },
  });
};
