import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";

import { CourseReviewSection } from "./course-review-section";
import * as reviewHooks from "@/features/review/hooks/useReviews";
import * as userHooks from "@/features/user/hooks/useUser";

const { mockMutate, mockSetLoginModalOpen } = vi.hoisted(() => ({
  mockMutate: vi.fn(),
  mockSetLoginModalOpen: vi.fn(),
}));

const EMOJI_STATS = [
  { emoji: "👍", count: 3, isMine: true },
  { emoji: "🔥", count: 1, isMine: false },
  { emoji: "🎓", count: 0, isMine: false },
  { emoji: "📝", count: 0, isMine: false },
  { emoji: "😴", count: 0, isMine: false },
  { emoji: "🚨", count: 0, isMine: false },
];

vi.mock("@/features/review/hooks/useReviews", () => ({
  useCourseEmojis: vi.fn(),
  useToggleCourseEmoji: vi.fn(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: vi.fn(),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setLoginModalOpen: (open: boolean) => void }) => unknown) =>
    selector({ setLoginModalOpen: mockSetLoginModalOpen }),
}));

const mockedUseCourseEmojis = vi.mocked(reviewHooks.useCourseEmojis);
const mockedUseToggleCourseEmoji = vi.mocked(reviewHooks.useToggleCourseEmoji);
const mockedUseUser = vi.mocked(userHooks.useUser);

describe("CourseReviewSection", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedUseCourseEmojis.mockReturnValue({
      data: EMOJI_STATS,
      status: "success",
    } as never);
    mockedUseToggleCourseEmoji.mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as never);
    mockedUseUser.mockReturnValue({
      data: { id: 1, email: "user@test.com", name: "테스트", role: "USER" },
      isPending: false,
    } as never);
  });

  it("이모지 통계와 본인 반응 상태를 보여준다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" />);

    expect(screen.getByRole("button", { name: "공감 3개" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "뜨거움 1개" })).toHaveTextContent("1");
    expect(screen.getByText("내 반응")).toBeInTheDocument();
  });

  it("로그인 상태에서는 이모지를 탭하면 토글 훅을 호출한다", () => {
    render(<CourseReviewSection courseKey="TEST-COURSE" />);

    fireEvent.click(screen.getByRole("button", { name: "뜨거움 1개" }));

    expect(mockMutate).toHaveBeenCalledWith("🔥");
    expect(mockSetLoginModalOpen).not.toHaveBeenCalled();
  });

  it("비로그인 상태에서는 로그인 모달을 연다", () => {
    mockedUseUser.mockReturnValueOnce({
      data: null,
      isPending: false,
    } as never);

    render(<CourseReviewSection courseKey="TEST-COURSE" />);

    fireEvent.click(screen.getByRole("button", { name: "뜨거움 1개" }));

    expect(mockSetLoginModalOpen).toHaveBeenCalledWith(true);
    expect(mockMutate).not.toHaveBeenCalled();
  });
});
