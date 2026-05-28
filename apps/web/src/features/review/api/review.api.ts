import api from "@/shared/api/client";
import type { CommonResponse, EmojiReviewResponse } from '@/shared/types/api';

/** 강의의 6종 이모지 리뷰 통계(카운트 + 본인 탭 여부)를 조회합니다. */
export const getCourseEmojis = async (
  courseKey: string
): Promise<CommonResponse<EmojiReviewResponse[]>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/emojis`);
  return data;
};

/** 특정 이모지를 토글합니다. (처음 탭 → 추가, 재탭 → 취소) */
export const toggleCourseEmoji = async (
  courseKey: string,
  emoji: string
): Promise<CommonResponse<void>> => {
  const { data } = await api.post(
    `/api/v1/courses/${encodeURIComponent(courseKey)}/emojis/toggle`,
    null,
    { params: { emoji } }
  );
  return data;
};
