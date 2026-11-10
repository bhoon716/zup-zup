import { useQuery, useMutation, useQueryClient, type QueryClient } from "@tanstack/react-query";
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

export const feedbackKeys = {
  all: ["feedback"] as const,
  myList: (page: number) => [...feedbackKeys.all, "my", { page }] as const,

  adminList: (page: number, deletion: AdminFeedbackDeletionFilter) =>
    [...feedbackKeys.all, "admin", { page, deletion }] as const,

  detail: (id: number) => [...feedbackKeys.all, "detail", id] as const,
};

function invalidateFeedbackListsAndDetail(queryClient: QueryClient, id?: number) {
  void queryClient.invalidateQueries({
    queryKey: feedbackKeys.all,
    predicate: ({ queryKey }) => queryKey[1] !== "detail" || queryKey[2] === id,
  });
}

export function useMyFeedbacks(page: number = 0) {
  return useQuery({
    queryKey: feedbackKeys.myList(page),
    queryFn: async () => {
      const response = await getMyFeedbacks(page);
      return response;
    },
  });
}

export function useFeedbackDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: feedbackKeys.detail(id),
    queryFn: async () => {
      const response = await getMyFeedbackDetail(id);
      return response;
    },
    enabled: enabled && !!id,
  });
}

export function useCreateFeedback() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ request, files }: { request: FeedbackCreateRequest; files: File[] }) => 
      createFeedback(request, files),

    onSuccess: () => {
      invalidateFeedbackListsAndDetail(queryClient);
    },
  });
}

export function useDeleteFeedback() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteFeedback(id),

    onSuccess: (_, id) => {
      invalidateFeedbackListsAndDetail(queryClient, id);
    },
  });
}

export function useFeedbacksForAdmin(
  page: number = 0,
  deletion: AdminFeedbackDeletionFilter = "ALL"
) {
  return useQuery({
    queryKey: feedbackKeys.adminList(page, deletion),
    queryFn: async () => {
      const response = await getFeedbacksForAdmin(page, deletion);
      return response;
    },
  });
}

export function useAdminFeedbackAttachmentDownload() {
  return useMutation({
    mutationFn: ({ feedbackId, attachmentId }: { feedbackId: number; attachmentId: number }) =>
      downloadFeedbackAttachmentForAdmin(feedbackId, attachmentId),
  });
}

export function useAdminFeedbackDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: [...feedbackKeys.detail(id), "admin"],
    queryFn: async () => {
      const response = await getFeedbackDetailForAdmin(id);
      return response;
    },
    enabled: enabled && !!id,
  });
}

export function useUpdateFeedbackStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: FeedbackStatusUpdateRequest }) => 
      updateFeedbackStatus(id, request),

    onSuccess: (_, { id }) => {
      invalidateFeedbackListsAndDetail(queryClient, id);
    },
  });
}

export function useCreateFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: FeedbackReplyCreateRequest }) => 
      createFeedbackReply(id, request),

    onSuccess: (_, { id }) => {
      invalidateFeedbackListsAndDetail(queryClient, id);
    },
  });
}

export function useUpdateFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ replyId, request }: { replyId: number; feedbackId: number; request: FeedbackReplyUpdateRequest }) => 
      updateFeedbackReply(replyId, request),

    onSuccess: (_, { feedbackId }) => {
      invalidateFeedbackListsAndDetail(queryClient, feedbackId);
    },
  });
}

export function useDeleteFeedbackReply() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ replyId }: { replyId: number; feedbackId: number }) => 
      deleteFeedbackReply(replyId),

    onSuccess: (_, { feedbackId }) => {
      invalidateFeedbackListsAndDetail(queryClient, feedbackId);
    },
  });
}
