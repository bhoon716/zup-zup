import api from "@/shared/api/client";
import type {
  CommonResponse,
  EmojiReviewResponse,
  ReviewCreateRequest,
  ReviewReactionRequest,
  ReviewResponse,
  SliceResponse,
} from "@/shared/types/api";

export const getReviews = async (
  courseKey: string,
  page: number = 0,
  size: number = 20,
  sort: string = "createdAt,desc"
): Promise<CommonResponse<SliceResponse<ReviewResponse>>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/reviews`, {
    params: { page, size, sort },
  });
  return data;
};

export const createReview = async (
  courseKey: string,
  request: ReviewCreateRequest
): Promise<CommonResponse<ReviewResponse>> => {
  const { data } = await api.post(`/api/v1/courses/${encodeURIComponent(courseKey)}/reviews`, request);
  return data;
};

export const toggleReviewReaction = async (
  reviewId: number,
  request: ReviewReactionRequest
): Promise<CommonResponse<void>> => {
  const { data } = await api.post(`/api/v1/courses/reviews/${reviewId}/reaction`, request);
  return data;
};

/** 강의의 이모지 리뷰 통계(카운트 + 본인 탭 여부)를 조회합니다. */
export const getCourseEmojis = async (
  courseKey: string
): Promise<CommonResponse<EmojiReviewResponse[]>> => {
  const { data } = await api.get(`/api/v1/courses/${encodeURIComponent(courseKey)}/emojis`);
  return data;
};

/** 특정 이모지를 토글합니다. */
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
