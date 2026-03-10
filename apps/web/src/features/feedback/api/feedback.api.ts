import api from "@/shared/api/client";
import type { 
  PageResponse, 
  FeedbackResponse, 
  FeedbackDetailResponse, 
  FeedbackCreateRequest,
  FeedbackStatusUpdateRequest,
  FeedbackReplyCreateRequest,
  FeedbackReplyUpdateRequest
} from '@/shared/types/api';

/**
 * 피드백 등록 (이미지 포함)
 */
export const createFeedback = async (
  request: FeedbackCreateRequest,
  files: File[]
): Promise<number> => {
  const formData = new FormData();
  
  // JSON 데이터를 Blob으로 변환하여 추가 (Spring @RequestPart 매핑용)
  formData.append('feedback', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  
  // 파일들 추가
  files.forEach(file => {
    formData.append('files', file);
  });

  const { data } = await api.post('/api/v1/feedbacks', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return data;
};

/**
 * 내 피드백 목록 조회
 */
export const getMyFeedbacks = async (
  page: number = 0,
  size: number = 10
): Promise<PageResponse<FeedbackResponse>> => {
  const { data } = await api.get('/api/v1/feedbacks/me', {
    params: { page, size },
  });
  return data;
};

/**
 * 내 피드백 상세 조회
 */
export const getMyFeedbackDetail = async (
  feedbackId: number
): Promise<FeedbackDetailResponse> => {
  const { data } = await api.get(`/api/v1/feedbacks/${feedbackId}`);
  return data;
};

/**
 * 피드백 삭제 (소프트 삭제)
 */
export const deleteFeedback = async (feedbackId: number): Promise<void> => {
  await api.delete(`/api/v1/feedbacks/${feedbackId}`);
};

// ================= Admin APIs =================

/**
 * 전체 피드백 목록 조회 (관리자)
 */
export const getFeedbacksForAdmin = async (
  page: number = 0,
  size: number = 20
): Promise<PageResponse<FeedbackResponse>> => {
  const { data } = await api.get('/api/v1/admin/feedbacks', {
    params: { page, size },
  });
  return data;
};

/**
 * 피드백 상세 조회 (관리자)
 */
export const getFeedbackDetailForAdmin = async (
  feedbackId: number
): Promise<FeedbackDetailResponse> => {
  const { data } = await api.get(`/api/v1/admin/feedbacks/${feedbackId}`);
  return data;
};

/**
 * 피드백 상태 변경 (관리자)
 */
export const updateFeedbackStatus = async (
  feedbackId: number,
  request: FeedbackStatusUpdateRequest
): Promise<void> => {
  await api.patch(`/api/v1/admin/feedbacks/${feedbackId}/status`, request);
};

/**
 * 관리자 답변 등록
 */
export const createFeedbackReply = async (
  feedbackId: number,
  request: FeedbackReplyCreateRequest
): Promise<number> => {
  const { data } = await api.post(`/api/v1/admin/feedbacks/${feedbackId}/reply`, request);
  return data;
};

/**
 * 관리자 답변 수정
 */
export const updateFeedbackReply = async (
  replyId: number,
  request: FeedbackReplyUpdateRequest
): Promise<void> => {
  await api.patch(`/api/v1/admin/feedbacks/reply/${replyId}`, request);
};
