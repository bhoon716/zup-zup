import type { ChangeEventHandler, RefObject } from "react";

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import FeedbackWritePage from "./page";

type FormValues = {
  type: "BUG" | "SUGGESTION" | "OTHER";
  title: string;
  content: string;
};

type FeedbackCreateFormProps = {
  onSubmit: (values: FormValues) => Promise<void>;
  previews: string[];
  onFileChange: ChangeEventHandler<HTMLInputElement>;
  onRemoveFile: (index: number) => void;
  fileInputRef: RefObject<HTMLInputElement | null>;
};

const {
  mockCompressImage,
  mockCreateFeedback,
  mockCreateObjectUrl,
  mockPush,
  mockRevokeObjectUrl,
} = vi.hoisted(() => ({
  mockCompressImage: vi.fn(),
  mockCreateFeedback: vi.fn(),
  mockCreateObjectUrl: vi.fn(),
  mockPush: vi.fn(),
  mockRevokeObjectUrl: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
}));

vi.mock("@/shared/lib/image", () => ({
  compressImage: mockCompressImage,
  isSupportedImageType: () => true,
}));

vi.mock("@/features/feedback/feedback-metadata", () => ({
  buildFeedbackMetadata: vi.fn(() => "{}"),
}));

vi.mock("@/features/feedback/hooks/useFeedback", () => ({
  useCreateFeedback: () => ({
    isPending: false,
    mutateAsync: mockCreateFeedback,
  }),
}));

vi.mock("@/features/feedback/components/feedback-create-form", () => ({
  FeedbackCreateForm: ({
    onSubmit,
    previews,
    onFileChange,
    onRemoveFile,
    fileInputRef,
  }: FeedbackCreateFormProps) => (
    <div>
      <input aria-label="이미지 선택" type="file" ref={fileInputRef} onChange={onFileChange} />
      <button
        type="button"
        onClick={() => void onSubmit({ type: "SUGGESTION", title: "제목", content: "내용입니다." })}
      >
        제출
      </button>
      {previews.map((preview, index) => (
        <button key={preview} type="button" onClick={() => onRemoveFile(index)}>
          {preview}
        </button>
      ))}
    </div>
  ),
}));

describe("FeedbackWritePage attachment previews", () => {
  const originalCreateObjectUrl = URL.createObjectURL;
  const originalRevokeObjectUrl = URL.revokeObjectURL;

  beforeEach(() => {
    vi.clearAllMocks();
    mockCompressImage.mockImplementation(async (file: File) => file);
    mockCreateFeedback.mockResolvedValue({ code: "SUCCESS", message: "ok", data: 1 });
    mockCreateObjectUrl.mockReturnValue("blob:feedback-image");
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: mockCreateObjectUrl,
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: mockRevokeObjectUrl,
    });
  });

  afterEach(() => {
    Object.defineProperty(URL, "createObjectURL", {
      configurable: true,
      value: originalCreateObjectUrl,
    });
    Object.defineProperty(URL, "revokeObjectURL", {
      configurable: true,
      value: originalRevokeObjectUrl,
    });
  });

  it("페이지를 벗어나면 모든 미리보기 Blob URL을 해제한다", async () => {
    const view = render(<FeedbackWritePage />);
    const file = new File(["image"], "feedback.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("이미지 선택"), { target: { files: [file] } });
    await waitFor(() => expect(mockCreateObjectUrl).toHaveBeenCalledTimes(1));

    view.unmount();

    expect(mockRevokeObjectUrl).toHaveBeenCalledWith("blob:feedback-image");
  });

  it("피드백 제출 성공 시 미리보기 Blob URL을 해제한다", async () => {
    render(<FeedbackWritePage />);
    const file = new File(["image"], "feedback.png", { type: "image/png" });

    fireEvent.change(screen.getByLabelText("이미지 선택"), { target: { files: [file] } });
    await waitFor(() => expect(mockCreateObjectUrl).toHaveBeenCalledTimes(1));

    fireEvent.click(screen.getByRole("button", { name: "제출" }));

    await waitFor(() => expect(mockPush).toHaveBeenCalledWith("/feedback"));
    expect(mockRevokeObjectUrl).toHaveBeenCalledWith("blob:feedback-image");
  });
});
