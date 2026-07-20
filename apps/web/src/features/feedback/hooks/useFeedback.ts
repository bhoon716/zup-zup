import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { 
  createFeedback, 
  getMyFeedbacks, 
  getMyFeedbackDetail, 
  deleteFeedback,
  getFeedbacksForAdmin,
  getFeedbackDetailForAdmin,
  updateFeedbackStatus,
  createFeedbackReply,
  updateFeedbackReply,
  deleteFeedbackReply,
  downloadFeedbackAttachmentForAdmin,
} from "../api/feedback.api";
import { 
  FeedbackCreateRequest, 
  FeedbackReplyCreateRequest, 
  FeedbackReplyUpdateRequest, 
  AdminFeedbackDeletionFilter,
  FeedbackStatusUpdateRequest 
} from "@/shared/types/api";

// 문의 및 건의 관련 React Query 키 정의
export const feedbackKeys = {
  all: ["feedback"] as const,
  myList: (page: number) => [...feedbackKeys.all, "my", { page }] as const,
  adminList: (page: number, deletion: AdminFeedbackDeletionFilter) =>
    [...feedbackKeys.all, "admin", { page, deletion }] as const,
  detail: (id: number) => [...feedbackKeys.all, "detail", id] as const,
};

// ================= User Hooks =================

/**
 * 내 문의 및 건의 목록을 조회하는 훅
 */
export function useMyFeedbacks(page: number = 0) {
  return useQuery({
    queryKey: feedbackKeys.myList(page),
    queryFn: async () => {
      const response = await getMyFeedbacks(page);
      return response.data;
    },
  });
}

/**
 * 특정 문의 및 건의사항의 상세 내용을 조회하는 훅
 */
export function useFeedbackDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: feedbackKeys.detail(id),
    queryFn: async () => {
      const response = await getMyFeedbackDetail(id);
      return response.data;
    },
    enabled: enabled && !!id,
  });
}

/**
 * 새로운 문의 및 건의를 생성하는 훅
 */
export function useCreateFeedback() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ request, files }: { request: FeedbackCreateRequest; files: File[] }) => 
      createFeedback(request, files),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
    },
  });
}

/**
 * 문의 및 건의 게시글을 삭제하는 훅 (Soft Delete)
 */
export function useDeleteFeedback() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteFeedback(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
    },
  });
}

// ================= Admin Hooks =================

/**
 * 관리자용 문의 및 건의 전체 목록 조회 훅
 */
export function useFeedbacksForAdmin(
  page: number = 0,
  deletion: AdminFeedbackDeletionFilter = "ALL"
) {
  return useQuery({
    queryKey: feedbackKeys.adminList(page, deletion),
    queryFn: async () => {
      const response = await getFeedbacksForAdmin(page, deletion);
      return response.data;
    },
  });
}

/**
 * 확인된 관리자 첨부파일 요청을 수행한다.
 */
export function useAdminFeedbackAttachmentDownload() {
  return useMutation({
    mutationFn: ({ feedbackId, attachmentId }: { feedbackId: number; attachmentId: number }) =>
      downloadFeedbackAttachmentForAdmin(feedbackId, attachmentId),
  });
}

/**
 * 관리자용 문의 및 건의 상세 조회 훅
 */
export function useAdminFeedbackDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: [...feedbackKeys.detail(id), "admin"],
    queryFn: async () => {
      const response = await getFeedbackDetailForAdmin(id);
      return response.data;
    },
    enabled: enabled && !!id,
  });
}

/**
 * 문의 및 건의 처리 상태를 변경하는 훅
 */
export function useUpdateFeedbackStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: FeedbackStatusUpdateRequest }) => 
      updateFeedbackStatus(id, request),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
      queryClient.invalidateQueries({ queryKey: feedbackKeys.detail(id) });
    },
  });
}

/**
 * 문의 및 건의에 대한 관리자 답변을 등록하는 훅
 */
export function useCreateFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: FeedbackReplyCreateRequest }) => 
      createFeedbackReply(id, request),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
      queryClient.invalidateQueries({ queryKey: feedbackKeys.detail(id) });
    },
  });
}

/**
 * 등록된 관리자 답변을 수정하는 훅
 */
export function useUpdateFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ replyId, request }: { replyId: number; feedbackId: number; request: FeedbackReplyUpdateRequest }) => 
      updateFeedbackReply(replyId, request),
    onSuccess: (_, { feedbackId }) => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
      queryClient.invalidateQueries({ queryKey: feedbackKeys.detail(feedbackId) });
    },
  });
}

/**
 * 등록된 관리자 답변을 삭제하는 훅
 */
export function useDeleteFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ replyId }: { replyId: number; feedbackId: number }) => 
      deleteFeedbackReply(replyId),
    onSuccess: (_, { feedbackId }) => {
      queryClient.invalidateQueries({ queryKey: feedbackKeys.all });
      queryClient.invalidateQueries({ queryKey: feedbackKeys.detail(feedbackId) });
    },
  });
}
