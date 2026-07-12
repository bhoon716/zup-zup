import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import AdminFeedbackPage from "./page";

const {
  mockDownloadAttachment,
  mockToastError,
  mockUseAdminFeedbackAttachmentDownload,
  mockUseAdminFeedbackDetail,
  mockUseFeedbacksForAdmin,
} = vi.hoisted(() => ({
  mockDownloadAttachment: vi.fn(),
  mockToastError: vi.fn(),
  mockUseAdminFeedbackAttachmentDownload: vi.fn(),
  mockUseAdminFeedbackDetail: vi.fn(),
  mockUseFeedbacksForAdmin: vi.fn(),
}));

vi.mock("@/features/feedback/hooks/useFeedback", () => ({
  useFeedbacksForAdmin: (...args: unknown[]) => mockUseFeedbacksForAdmin(...args),
  useAdminFeedbackDetail: (...args: unknown[]) => mockUseAdminFeedbackDetail(...args),
  useUpdateFeedbackStatus: () => ({ mutateAsync: vi.fn() }),
  useCreateFeedbackReply: () => ({ mutateAsync: vi.fn() }),
  useUpdateFeedbackReply: () => ({ mutateAsync: vi.fn() }),
  useDeleteFeedbackReply: () => ({ mutateAsync: vi.fn() }),
  useAdminFeedbackAttachmentDownload: () => mockUseAdminFeedbackAttachmentDownload(),
}));

vi.mock("sonner", () => ({
  toast: {
    error: mockToastError,
    success: vi.fn(),
  },
}));

describe("AdminFeedbackPage attachment preview", () => {
  const createObjectUrl = vi.fn(() => "blob:authenticated-image");
  const revokeObjectUrl = vi.fn();
  const originalCreateObjectUrl = URL.createObjectURL;
  const originalRevokeObjectUrl = URL.revokeObjectURL;

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: createObjectUrl,
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: revokeObjectUrl,
    });

    mockUseFeedbacksForAdmin.mockReturnValue({
      data: {
        content: [{
          id: 10,
          type: "BUG",
          title: "이미지 첨부 문의",
          status: "PENDING",
          createdAt: "2026-07-13T00:00:00.000Z",
          hasReplies: false,
          deleted: false,
          deletedAt: null,
          authorLabel: "사용자",
        }, {
          id: 11,
          type: "BUG",
          title: "다른 첨부 문의",
          status: "PENDING",
          createdAt: "2026-07-13T00:00:00.000Z",
          hasReplies: false,
          deleted: false,
          deletedAt: null,
          authorLabel: "사용자",
        }],
        totalElements: 1,
      },
      isLoading: false,
    });
    mockUseAdminFeedbackDetail.mockReturnValue({
      data: {
        id: 10,
        type: "BUG",
        title: "이미지 첨부 문의",
        content: "미리보기할 이미지가 있습니다.",
        status: "PENDING",
        createdAt: "2026-07-13T00:00:00.000Z",
        deleted: false,
        deletedAt: null,
        authorLabel: "사용자",
        attachments: [{ id: 20 }],
        replies: [],
      },
      isLoading: false,
    });
    mockUseAdminFeedbackAttachmentDownload.mockReturnValue({
      mutateAsync: mockDownloadAttachment,
      isPending: false,
    });
    mockDownloadAttachment.mockResolvedValue(new Blob(["image"], { type: "image/png" }));
  });

  afterEach(() => {
    vi.restoreAllMocks();
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: originalCreateObjectUrl,
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: originalRevokeObjectUrl,
    });
  });

  it("renders a confirmed image blob in a preview dialog, provides a safe download name, and revokes the object URL when closed", async () => {
    render(<AdminFeedbackPage />);

    fireEvent.click(screen.getByText("이미지 첨부 문의"));
    fireEvent.click(screen.getByRole("button", { name: "첨부파일 1" }));

    await waitFor(() => {
      expect(mockDownloadAttachment).toHaveBeenCalledWith({ feedbackId: 10, attachmentId: 20 });
    });

    const dialog = await screen.findByRole("dialog", { name: "첨부파일 미리보기" });
    expect(within(dialog).getByRole("img", { name: "첨부파일 1 미리보기" }))
      .toHaveAttribute("src", "blob:authenticated-image");

    const download = within(dialog).getByRole("link", { name: "다운로드" });
    expect(download).toHaveAttribute("href", "blob:authenticated-image");
    expect(download).toHaveAttribute("download", "feedback-attachment-20.png");

    fireEvent.keyDown(document, { key: "Escape" });

    await waitFor(() => {
      expect(screen.queryByRole("dialog", { name: "첨부파일 미리보기" })).not.toBeInTheDocument();
      expect(revokeObjectUrl).toHaveBeenCalledWith("blob:authenticated-image");
    });
  });

  it("does not render a preview when the authenticated attachment request is rejected", async () => {
    mockDownloadAttachment.mockRejectedValueOnce(new Error("forbidden or missing"));
    render(<AdminFeedbackPage />);

    fireEvent.click(screen.getByText("이미지 첨부 문의"));
    fireEvent.click(screen.getByRole("button", { name: "첨부파일 1" }));

    await waitFor(() => {
      expect(mockToastError).toHaveBeenCalledWith("첨부파일을 열람할 수 없습니다.");
    });
    expect(createObjectUrl).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog", { name: "첨부파일 미리보기" })).not.toBeInTheDocument();
  });

  it("ignores a late attachment response after the selected feedback changes", async () => {
    let resolveBlob: (blob: Blob) => void = () => {};
    const pendingBlob = new Promise<Blob>((resolve) => {
      resolveBlob = resolve;
    });
    mockDownloadAttachment.mockReturnValueOnce(pendingBlob);
    render(<AdminFeedbackPage />);

    fireEvent.click(screen.getByText("이미지 첨부 문의"));
    fireEvent.click(screen.getByRole("button", { name: "첨부파일 1" }));
    await waitFor(() => expect(mockDownloadAttachment).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByText("다른 첨부 문의"));

    await act(async () => {
      resolveBlob(new Blob(["image"], { type: "image/png" }));
      await pendingBlob;
    });

    expect(createObjectUrl).not.toHaveBeenCalled();
    expect(screen.queryByRole("dialog", { name: "첨부파일 미리보기" })).not.toBeInTheDocument();
  });

  it("does not create a blob URL after the page unmounts during an attachment request", async () => {
    let resolveBlob: (blob: Blob) => void = () => {};
    const pendingBlob = new Promise<Blob>((resolve) => {
      resolveBlob = resolve;
    });
    mockDownloadAttachment.mockReturnValueOnce(pendingBlob);
    const view = render(<AdminFeedbackPage />);

    fireEvent.click(screen.getByText("이미지 첨부 문의"));
    fireEvent.click(screen.getByRole("button", { name: "첨부파일 1" }));
    await waitFor(() => expect(mockDownloadAttachment).toHaveBeenCalledTimes(1));
    view.unmount();

    await act(async () => {
      resolveBlob(new Blob(["image"], { type: "image/png" }));
      await pendingBlob;
    });

    expect(createObjectUrl).not.toHaveBeenCalled();
  });
});
