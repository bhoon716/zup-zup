import api from "@/shared/api/client";
import type { PageResponse, FeedbackResponse, FeedbackDetailResponse, AdminFeedbackDeletionFilter, AdminFeedbackDetailResponse, AdminFeedbackResponse, FeedbackCreateRequest, FeedbackStatusUpdateRequest, FeedbackReplyCreateRequest, FeedbackReplyUpdateRequest } from "@/shared/types/api";




export const createFeedback = async (
  request: FeedbackCreateRequest,
  files: File[]
): Promise<number> => {
  const formData = new FormData();
  

  formData.append('feedback', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  

  files.forEach(file => {
    formData.append('files', file);
  });

  const { data } = await api.post<number>('/api/v1/feedbacks', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return data;
};




export const getMyFeedbacks = async (
  page: number = 0
): Promise<PageResponse<FeedbackResponse>> => {
  const { data } = await api.get<PageResponse<FeedbackResponse>>('/api/v1/feedbacks/me', {
    params: { page },
  });
  return data;
};




export const getMyFeedbackDetail = async (
  feedbackId: number
): Promise<FeedbackDetailResponse> => {
  const { data } = await api.get<FeedbackDetailResponse>(`/api/v1/feedbacks/${feedbackId}`);
  return data;
};




export const deleteFeedback = async (feedbackId: number): Promise<void> => {
  await api.delete(`/api/v1/feedbacks/${feedbackId}`);
};






export const getFeedbacksForAdmin = async (
  page: number = 0,
  deletion: AdminFeedbackDeletionFilter = 'ALL'
): Promise<PageResponse<AdminFeedbackResponse>> => {
  const { data } = await api.get<PageResponse<AdminFeedbackResponse>>('/api/v1/admin/feedbacks', {
    params: { page, deletion },
  });
  return data;
};




export const getFeedbackDetailForAdmin = async (
  feedbackId: number
): Promise<AdminFeedbackDetailResponse> => {
  const { data } = await api.get<AdminFeedbackDetailResponse>(`/api/v1/admin/feedbacks/${feedbackId}`);
  return data;
};




export const downloadFeedbackAttachmentForAdmin = async (
  feedbackId: number,
  attachmentId: number
): Promise<Blob> => {
  const { data } = await api.post<Blob>(
    `/api/v1/admin/feedbacks/${feedbackId}/attachments/${attachmentId}/download`,
    { confirmed: true },
    { responseType: 'blob' }
  );
  return data;
};




export const updateFeedbackStatus = async (
  feedbackId: number,
  request: FeedbackStatusUpdateRequest
): Promise<void> => {
  await api.patch(`/api/v1/admin/feedbacks/${feedbackId}/status`, request);
};




export const createFeedbackReply = async (
  feedbackId: number,
  request: FeedbackReplyCreateRequest
): Promise<number> => {
  const { data } = await api.post<number>(`/api/v1/admin/feedbacks/${feedbackId}/reply`, request);
  return data;
};




export const updateFeedbackReply = async (
  replyId: number,
  request: FeedbackReplyUpdateRequest
): Promise<void> => {
  await api.patch(`/api/v1/admin/feedbacks/reply/${replyId}`, request);
};




export const deleteFeedbackReply = async (
  replyId: number
): Promise<void> => {
  await api.delete(`/api/v1/admin/feedbacks/reply/${replyId}`);
};
