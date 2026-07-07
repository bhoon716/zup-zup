import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { CourseTable } from "./course-table";

const { mockSetLoginModalOpen, mockToggleWishlist, mockSubscribe, mockUnsubscribe, mockAddToTimetable } = vi.hoisted(() => ({
  mockSetLoginModalOpen: vi.fn(),
  mockToggleWishlist: vi.fn(),
  mockSubscribe: vi.fn(),
  mockUnsubscribe: vi.fn(),
  mockAddToTimetable: vi.fn(),
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setLoginModalOpen: (open: boolean) => void }) => unknown) =>
    selector({
      setLoginModalOpen: mockSetLoginModalOpen,
    }),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: () => ({
    data: null,
  }),
}));

vi.mock("@/features/timetable/hooks/useTimetable", () => ({
  useAddCourseToTimetable: () => ({
    mutate: mockAddToTimetable,
    isPending: false,
  }),
  useTimetables: () => ({
    data: [],
  }),
}));

vi.mock("@/features/subscription/hooks/useSubscriptions", () => ({
  useSubscribe: () => ({
    mutate: mockSubscribe,
    isPending: false,
  }),
  useSubscriptions: () => ({
    data: [],
  }),
  useUnsubscribe: () => ({
    mutate: mockUnsubscribe,
    isPending: false,
  }),
}));

vi.mock("@/features/wishlist/hooks/useWishlist", () => ({
  useToggleWishlist: () => ({
    mutate: mockToggleWishlist,
  }),
  useWishlist: () => ({
    data: [],
  }),
}));

vi.mock("./course-detail-dialog", () => ({
  CourseDetailDialog: () => null,
}));

describe("CourseTable", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    class MockIntersectionObserver {
      observe() {}
      disconnect() {}
      unobserve() {}
    }

    vi.stubGlobal("IntersectionObserver", MockIntersectionObserver as never);
  });

  it("카드 제목을 순서에 맞는 heading으로 렌더링하고 아이콘 버튼에 이름을 제공한다", () => {
    render(
      <CourseTable
        courses={[
          {
            courseKey: "TEST-COURSE",
            name: "Theories of International Relations",
            classification: "전공필수",
            credits: 3,
            subjectCode: "0000125258",
            professor: "이진영",
            classTime: "월 6",
            classroom: "공학관 101",
            capacity: 20,
            current: 10,
            available: 10,
            averageRating: 4.2,
            reviewCount: 13,
            isSubscribable: true,
          } as never,
        ]}
      />
    );

    expect(screen.getByRole("heading", { name: "Theories of International Relations", level: 2 })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations를 시간표에 추가하려면 로그인" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations 관심 강의 추가" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations 여석 알림 받기" })).toBeInTheDocument();
  });
});
