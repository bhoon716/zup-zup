import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { CourseReviewSection } from "./course-review-section";
import * as reviewHooks from "@/features/review/hooks/useReviews";
import * as userHooks from "@/features/user/hooks/useUser";

const { mockCreateReview, mockToggleEmoji, mockSetLoginModalOpen } = vi.hoisted(() => ({
  mockCreateReview: vi.fn(),
  mockToggleEmoji: vi.fn(),
  mockSetLoginModalOpen: vi.fn(),
}));

vi.mock("@/features/review/hooks/useReviews", () => ({
  useCreateReview: vi.fn(),
  useCourseEmojis: vi.fn(),
  useToggleCourseEmoji: vi.fn(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: vi.fn(),
}));

vi.mock("@emoji-mart/react", () => ({
  default: ({ onEmojiSelect }: { onEmojiSelect: (emoji: { native: string }) => void }) => (
    <div data-testid="emoji-picker">
      <button type="button" onClick={() => onEmojiSelect({ native: "🥹" })}>
        🥹
      </button>
      <button type="button" onClick={() => onEmojiSelect({ native: "😂" })}>
        😂
      </button>
    </div>
  ),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setLoginModalOpen: (open: boolean) => void }) => unknown) =>
    selector({ setLoginModalOpen: mockSetLoginModalOpen }),
}));

describe("CourseReviewSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(reviewHooks.useCreateReview).mockReturnValue({
      mutate: mockCreateReview,
      isPending: false,
    } as never);

    vi.mocked(reviewHooks.useCourseEmojis).mockReturnValue({
      data: [
        { emoji: "👍", count: 3, isMine: true },
        { emoji: "😂", count: 0, isMine: false },
      ],
      status: "success",
    } as never);

    vi.mocked(reviewHooks.useToggleCourseEmoji).mockReturnValue({
      mutate: mockToggleEmoji,
      isPending: false,
    } as never);

    vi.mocked(userHooks.useUser).mockReturnValue({
      data: { id: 1, email: "user@test.com", name: "테스트", role: "USER" },
      isPending: false,
    } as never);
  });

  it("평균 별점, 별점 등록 폼, 이모지 리뷰만 보여주고 목록 패널은 숨긴다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    expect(screen.getByRole("heading", { name: "강의 리뷰" })).toBeInTheDocument();
    expect(screen.getByText("전체 평균 별점")).toBeInTheDocument();
    expect(screen.getByText("4.2점")).toBeInTheDocument();
    expect(screen.getByText("(13개)")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "별점 리뷰 등록" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "등록" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "이모지 리뷰" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "별점 리뷰 목록" })).not.toBeInTheDocument();
    expect(screen.queryByText("모든 별점 리뷰의 평균입니다.")).not.toBeInTheDocument();
    expect(screen.getByText("이모지만 남길 수 있습니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "👍 3개" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "👍 3개" })).toHaveClass("bg-primary/20");
  });

  it("평균 별점이 없으면 0점과 0개로 표시한다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" isReviewed={false} />);

    expect(screen.getByText("0점")).toBeInTheDocument();
    expect(screen.getByText("(0개)")).toBeInTheDocument();
  });

  it("별점을 선택하고 등록하면 리뷰 생성 훅을 호출한다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    fireEvent.click(screen.getByRole("button", { name: /5점/ }));
    fireEvent.click(screen.getByRole("button", { name: "등록" }));

    expect(mockCreateReview).toHaveBeenCalledWith({ rating: 5 }, expect.any(Object));
  });

  it("이모지 추가 버튼을 누르면 선택 모달이 열리고 이모지를 고르면 토글 훅을 호출한다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    fireEvent.click(screen.getByRole("button", { name: "이모지 추가" }));
    expect(screen.getByTestId("emoji-picker")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "🥹" }));

    expect(mockToggleEmoji).toHaveBeenCalledWith("🥹", expect.objectContaining({ onSettled: expect.any(Function) }));
  });

  it("내가 단 이모지 버튼을 다시 누르면 같은 이모지를 토글한다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    fireEvent.click(screen.getByRole("button", { name: "👍 3개" }));

    expect(mockToggleEmoji).toHaveBeenCalledWith("👍", expect.objectContaining({ onSettled: expect.any(Function) }));
  });

  it("같은 이모지를 연속으로 눌러도 중복 토글 요청은 보내지 않는다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    const thumbButton = screen.getByRole("button", { name: "👍 3개" });
    fireEvent.click(thumbButton);
    fireEvent.click(thumbButton);

    expect(mockToggleEmoji).toHaveBeenCalledTimes(1);
    expect(mockToggleEmoji).toHaveBeenCalledWith("👍", expect.objectContaining({ onSettled: expect.any(Function) }));
  });

  it("이모지 토글 대기 중에는 다른 이모지도 추가로 보내지 않는다", () => {
    vi.mocked(reviewHooks.useCourseEmojis).mockReturnValueOnce({
      data: [
        { emoji: "👍", count: 3, isMine: true },
        { emoji: "😂", count: 2, isMine: false },
      ],
      status: "success",
    } as never);

    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    fireEvent.click(screen.getByRole("button", { name: "👍 3개" }));
    fireEvent.click(screen.getByRole("button", { name: "😂 2개" }));

    expect(mockToggleEmoji).toHaveBeenCalledTimes(1);
    expect(mockToggleEmoji).toHaveBeenCalledWith("👍", expect.objectContaining({ onSettled: expect.any(Function) }));
  });

  it("비로그인 상태에서는 로그인 모달을 연다", () => {
    vi.mocked(userHooks.useUser).mockReturnValueOnce({
      data: null,
      isPending: false,
    } as never);

    render(<CourseReviewSection courseKey="TEST-COURSE" averageRating={4.2} reviewCount={13} isReviewed={false} />);

    fireEvent.click(screen.getByRole("button", { name: "이모지 추가" }));

    expect(mockSetLoginModalOpen).toHaveBeenCalledWith(true);
    expect(mockToggleEmoji).not.toHaveBeenCalled();
  });
});
