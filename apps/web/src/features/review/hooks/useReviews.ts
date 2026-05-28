import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import * as reviewApi from '@/features/review/api/review.api';

/** 강의의 6종 이모지 리뷰 통계를 실시간 조회합니다. */
export const useCourseEmojis = (courseKey: string) => {
  return useQuery({
    queryKey: ['course-emojis', courseKey],
    queryFn: async () => {
      const response = await reviewApi.getCourseEmojis(courseKey);
      return response.data;
    },
    enabled: !!courseKey,
  });
};

/** 이모지를 토글(추가/취소)합니다. 성공 시 해당 강의 이모지 캐시를 자동 무효화합니다. */
export const useToggleCourseEmoji = (courseKey: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (emoji: string) => reviewApi.toggleCourseEmoji(courseKey, emoji),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['course-emojis', courseKey] });
    },
  });
};
