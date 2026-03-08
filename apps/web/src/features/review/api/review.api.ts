import api from "@/shared/api/client";
import type { CommonResponse, SliceResponse, ReviewResponse, ReviewCreateRequest, ReviewReactionRequest } from '@/shared/types/api';

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
