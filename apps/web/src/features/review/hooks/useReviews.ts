import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import * as reviewApi from "@/features/review/api/review.api";
import { getReviewScopeKey } from "@/shared/lib/course";
import type {
  Course,
  EmojiReviewResponse,
  ReviewCreateRequest,
  ReviewReactionRequest,
  ReviewResponse,
  ReviewUpdateRequest,
} from "@/shared/types/api";

const sortEmojiStats = (items: EmojiReviewResponse[]) =>
  [...items].sort((left, right) => {
    if (right.count !== left.count) {
      return right.count - left.count;
    }

    return left.emoji.localeCompare(right.emoji);
  });

type CourseInfiniteCache = {
  pages: Array<{
    content: Course[];
    last: boolean;
    number: number;
  }>;
  pageParams: unknown[];
};

const updateCourseReviewStats = (course: Course, previousRating: number | null, nextRating: number) => {
  const currentCount = course.reviewCount ?? 0;
  const currentAverage = course.averageRating ?? 0;

  if (previousRating === null) {
    const nextCount = currentCount + 1;
    const nextAverage = nextCount > 0 ? ((currentAverage * currentCount) + nextRating) / nextCount : nextRating;
    return {
      ...course,
      averageRating: nextAverage,
      reviewCount: nextCount,
      isReviewed: true,
    };
  }

  const nextAverage = currentCount > 0
    ? ((currentAverage * currentCount) - previousRating + nextRating) / currentCount
    : nextRating;

  return {
    ...course,
    averageRating: nextAverage,
    reviewCount: currentCount,
    isReviewed: true,
  };
};

const updateCourseLists = (current: unknown, updatedCourse: Course) => {
  if (!current || typeof current !== "object" || !("pages" in current) || !Array.isArray((current as CourseInfiniteCache).pages)) {
    return current;
  }

  const cache = current as CourseInfiniteCache;
  const updatedScopeKey = getReviewScopeKey(updatedCourse);

  return {
    ...cache,
    pages: cache.pages.map((page) => ({
      ...page,
      content: page.content.map((course) =>
        getReviewScopeKey(course) === updatedScopeKey ? { ...course, ...updatedCourse } : course
      ),
    })),
  };
};

const updateCourseDetailCaches = (queryClient: ReturnType<typeof useQueryClient>, updatedCourse: Course) => {
  const updatedScopeKey = getReviewScopeKey(updatedCourse);

  queryClient.getQueriesData<Course>({ queryKey: ["course-detail"] }).forEach(([queryKey, currentCourse]) => {
    if (!currentCourse || getReviewScopeKey(currentCourse) !== updatedScopeKey) {
      return;
    }

    queryClient.setQueryData(queryKey, { ...currentCourse, ...updatedCourse });
  });
};

export const useReviews = (reviewScopeKey: string, courseKey: string) => {
  return useQuery<ReviewResponse | null>({
    queryKey: ["review", reviewScopeKey],
    queryFn: async () => {
      const response = await reviewApi.getReviews(courseKey, 0, 1);
      return response.data.content[0] ?? null;
    },
    enabled: !!reviewScopeKey && !!courseKey,
  });
};

export const useCreateReview = (reviewScopeKey: string, courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ReviewCreateRequest) => reviewApi.createReview(courseKey, request),
    onSuccess: (response) => {
      const previousReview = queryClient.getQueryData<ReviewResponse | null>(["review", reviewScopeKey]);
      const previousCourse = queryClient.getQueryData<Course>(["course-detail", courseKey]);

      queryClient.setQueryData(["review", reviewScopeKey], response.data);

      if (previousCourse) {
        const updatedCourse = updateCourseReviewStats(previousCourse, previousReview?.rating ?? null, response.data.rating);
        queryClient.setQueryData(["course-detail", courseKey], updatedCourse);
        queryClient.setQueriesData({ queryKey: ["courses"] }, (current) => updateCourseLists(current, updatedCourse));
        updateCourseDetailCaches(queryClient, updatedCourse);
      }
    },
  });
};

export const useUpdateReview = (reviewScopeKey: string, courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reviewId, request }: { reviewId: number; request: ReviewUpdateRequest }) =>
      reviewApi.updateReview(reviewId, request),
    onSuccess: (response) => {
      const previousReview = queryClient.getQueryData<ReviewResponse | null>(["review", reviewScopeKey]);
      const previousCourse = queryClient.getQueryData<Course>(["course-detail", courseKey]);

      queryClient.setQueryData(["review", reviewScopeKey], response.data);

      if (previousCourse) {
        const updatedCourse = updateCourseReviewStats(previousCourse, previousReview?.rating ?? null, response.data.rating);
        queryClient.setQueryData(["course-detail", courseKey], updatedCourse);
        queryClient.setQueriesData({ queryKey: ["courses"] }, (current) => updateCourseLists(current, updatedCourse));
        updateCourseDetailCaches(queryClient, updatedCourse);
      }
    },
  });
};

export const useToggleReviewReaction = (reviewScopeKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ reviewId, request }: { reviewId: number; request: ReviewReactionRequest }) =>
      reviewApi.toggleReviewReaction(reviewId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["review", reviewScopeKey] });
    },
  });
};

/** 강의의 이모지 리뷰 통계를 실시간 조회합니다. */
export const useCourseEmojis = (reviewScopeKey: string, courseKey: string) => {
  return useQuery({
    queryKey: ["course-emojis", reviewScopeKey],
    queryFn: async () => {
      const response = await reviewApi.getCourseEmojis(courseKey);
      return response.data;
    },
    enabled: !!reviewScopeKey && !!courseKey,
  });
};

/** 이모지를 토글합니다. 성공 시 해당 강의 이모지 캐시를 자동 무효화합니다. */
export const useToggleCourseEmoji = (reviewScopeKey: string, courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (emoji: string) => reviewApi.toggleCourseEmoji(courseKey, emoji),
    onMutate: async (emoji: string) => {
      await queryClient.cancelQueries({ queryKey: ["course-emojis", reviewScopeKey] });

      const previousEmojis = queryClient.getQueryData<EmojiReviewResponse[]>(["course-emojis", reviewScopeKey]);

      queryClient.setQueryData<EmojiReviewResponse[]>(["course-emojis", reviewScopeKey], (current = []) => {
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

      queryClient.setQueryData(["course-emojis", reviewScopeKey], context.previousEmojis);
    },
    onSettled: () =>
      queryClient.invalidateQueries({ queryKey: ["course-emojis", reviewScopeKey] }),
  });
};
