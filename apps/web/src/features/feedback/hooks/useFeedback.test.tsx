import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import * as feedbackApi from "@/features/feedback/api/feedback.api";
import {
  useAdminFeedbackAttachmentDownload,
  useCreateFeedback,
  useFeedbacksForAdmin,
  useMyFeedbacks,
} from "./useFeedback";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import { CommonResponse, PageResponse, FeedbackResponse } from "@/shared/types/api";

vi.mock("@/features/feedback/api/feedback.api", () => ({
  createFeedback: vi.fn(),
  getMyFeedbacks: vi.fn(),
  getMyFeedbackDetail: vi.fn(),
  deleteFeedback: vi.fn(),
  getFeedbacksForAdmin: vi.fn(),
  getFeedbackDetailForAdmin: vi.fn(),
  updateFeedbackStatus: vi.fn(),
  createFeedbackReply: vi.fn(),
  updateFeedbackReply: vi.fn(),
  deleteFeedbackReply: vi.fn(),
  downloadFeedbackAttachmentForAdmin: vi.fn(),
}));

describe("useFeedback hooks", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("useMyFeedbacks", () => {
    it("내 피드백 목록을 성공적으로 불러온다", async () => {
      const mockData: PageResponse<FeedbackResponse> = {
        content: [
          { 
            id: 1, 
            title: "테스트 피드백", 
            status: "PENDING", 
            type: "BUG", 
            createdAt: "2024-01-01T00:00:00",
            hasReplies: false
          }
        ],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 10,
        last: true,
        first: true,
        numberOfElements: 1,
        empty: false,
        pageable: {
          pageNumber: 0,
          pageSize: 10,
          sort: { empty: true, sorted: false, unsorted: true }
        }
      };

      vi.mocked(feedbackApi.getMyFeedbacks).mockResolvedValue({
        code: "SUCCESS",
        message: "ok",
        data: mockData,
      } as CommonResponse<PageResponse<FeedbackResponse>>);

      const queryClient = createTestQueryClient();
      const wrapper = createQueryWrapper(queryClient);
      const { result } = renderHook(() => useMyFeedbacks(0), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(result.current.data).toEqual(mockData);
      expect(feedbackApi.getMyFeedbacks).toHaveBeenCalledWith(0);
    });
  });

  describe("useCreateFeedback", () => {
    it("피드백 생성 요청 성공 시 캐시를 무효화한다", async () => {
      vi.mocked(feedbackApi.createFeedback).mockResolvedValue({
        code: "SUCCESS",
        message: "ok",
        data: 1,
      });

      const queryClient = createTestQueryClient();
      const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
      const wrapper = createQueryWrapper(queryClient);
      
      const { result } = renderHook(() => useCreateFeedback(), { wrapper });

      await result.current.mutateAsync({ 
        request: { title: "새 피드백", content: "내용", type: "SUGGESTION", metaInfo: "{}" }, 
        files: [] 
      });

      expect(feedbackApi.createFeedback).toHaveBeenCalled();
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["feedback"] });
    });
  });

  describe("관리자 피드백", () => {
    it("삭제 상태 필터를 별도 캐시 키와 API 요청으로 전달한다", async () => {
      vi.mocked(feedbackApi.getFeedbacksForAdmin).mockResolvedValue({
        code: "SUCCESS",
        message: "ok",
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          number: 0,
          size: 20,
          last: true,
          first: true,
          numberOfElements: 0,
          empty: true,
          pageable: { pageNumber: 0, pageSize: 20, sort: { empty: true, sorted: false, unsorted: true } },
        },
      } as CommonResponse<PageResponse<never>>);

      const queryClient = createTestQueryClient();
      const wrapper = createQueryWrapper(queryClient);
      const { result } = renderHook(() => useFeedbacksForAdmin(0, "DELETED"), { wrapper });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      expect(feedbackApi.getFeedbacksForAdmin).toHaveBeenCalledWith(0, "DELETED");
    });

    it("명시적으로 확인한 관리자 첨부파일 다운로드 요청만 전송한다", async () => {
      vi.mocked(feedbackApi.downloadFeedbackAttachmentForAdmin).mockResolvedValue(new Blob(["image"]));

      const queryClient = createTestQueryClient();
      const wrapper = createQueryWrapper(queryClient);
      const { result } = renderHook(() => useAdminFeedbackAttachmentDownload(), { wrapper });

      await result.current.mutateAsync({ feedbackId: 10, attachmentId: 20 });

      expect(feedbackApi.downloadFeedbackAttachmentForAdmin).toHaveBeenCalledWith(10, 20);
    });
  });
});
