import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
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
  CourseDetailDialog: ({
    course,
    open,
  }: {
    course: { name: string } | null;
    open: boolean;
  }) => open && course ? <div role="dialog" aria-label={`${course.name} 상세`} /> : null,
}));

const testCourse = {
  courseKey: "TEST-COURSE",
  name: "Theories of International Relations",
  classification: "전공필수",
  credits: 3,
  subjectCode: "0000125258",
  stdtrNo: "GECO178",
  professor: "이진영",
  classTime: "월 6",
  classroom: "공학관 101",
  capacity: 20,
  current: 10,
  available: 10,
  averageRating: 4.2,
  reviewCount: 13,
  isSubscribable: true,
} as never;

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
        courses={[testCourse]}
      />
    );

    expect(screen.getByRole("heading", { name: "Theories of International Relations", level: 2 })).toBeInTheDocument();
    expect(screen.getByText("GECO178")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations를 시간표에 추가하려면 로그인" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations 관심 강의 추가" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Theories of International Relations 여석 알림 받기" })).toBeInTheDocument();
  });

  it.each([
    { interaction: "포인터 클릭", key: null },
    { interaction: "Enter", key: "{Enter}" },
    { interaction: "Space", key: " " },
  ])("$interaction으로 강의 제목에서 상세를 연다", async ({ key }) => {
    const user = userEvent.setup();
    render(<CourseTable courses={[testCourse]} />);

    const detailTrigger = screen.getByRole("button", {
      name: "Theories of International Relations",
    });

    if (key) {
      detailTrigger.focus();
      expect(detailTrigger).toHaveFocus();
      await user.keyboard(key);
    } else {
      await user.click(detailTrigger);
    }

    expect(
      screen.getByRole("dialog", {
        name: "Theories of International Relations 상세",
      }),
    ).toBeInTheDocument();
  });

  it("카드 내부 액션 버튼은 강의 상세를 열지 않는다", async () => {
    const user = userEvent.setup();
    render(
      <CourseTable
        courses={[testCourse]}
        initialUser={{ id: 1, name: "테스트 사용자" } as never}
        initialSubscriptions={[]}
        initialWishlist={[]}
        initialTimetables={[]}
      />,
    );

    await user.click(
      screen.getByRole("button", {
        name: "Theories of International Relations 관심 강의 추가",
      }),
    );

    expect(mockToggleWishlist).toHaveBeenCalledWith("TEST-COURSE");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
