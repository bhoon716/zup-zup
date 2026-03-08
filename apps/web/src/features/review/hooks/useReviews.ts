import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as reviewApi from '@/features/review/api/review.api';
import type { ReviewCreateRequest, ReviewReactionRequest } from '@/shared/types/api';

export const useReviews = (courseKey: string, sort: string = 'createdAt,desc') => {
  return useInfiniteQuery({
    queryKey: ['reviews', courseKey, sort],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await reviewApi.getReviews(courseKey, pageParam as number, 20, sort);
      const sliceData = response.data;
      return {
        content: sliceData.content,
        last: sliceData.last,
        number: sliceData.number
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
      queryClient.invalidateQueries({ queryKey: ['reviews', courseKey] });
      queryClient.invalidateQueries({ queryKey: ['course-detail', courseKey] });
      queryClient.invalidateQueries({ queryKey: ['courses'] });
    },
  });
};

export const useToggleReviewReaction = (courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reviewId, request }: { reviewId: number; request: ReviewReactionRequest }) =>
      reviewApi.toggleReviewReaction(reviewId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', courseKey] });
    },
  });
};
