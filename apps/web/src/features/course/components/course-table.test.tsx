import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { CourseTable } from "./course-table";
import type { Course } from "@/shared/types/api";

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
  credits: "3",
  classNumber: "01",
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
} as Course;

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

  it.each([
    { enabled: true, disclosure: "공개", visible: true },
    { enabled: false, disclosure: "공개", visible: false },
    { enabled: true, disclosure: "비공개", visible: false },
    { enabled: true, disclosure: undefined, visible: false },
  ])("공개 맥락 $enabled / 공개 상태 $disclosure의 Clarity 예외를 제한한다", ({ enabled, disclosure, visible }) => {
    const { container } = render(<CourseTable courses={[{ ...testCourse, disclosure }]} unmaskPublicCourseInfo={enabled} />);
    expect(container.querySelectorAll("[data-clarity-unmask]")).toHaveLength(visible ? 3 : 0);
    if (visible) {
      for (const element of [screen.getByRole("heading", { name: testCourse.name }), screen.getByText("3학점"), screen.getByText("월 14:00–15:00")]) {
        expect(element).toHaveAttribute("data-clarity-unmask", "true");
      }
    }
    for (const element of [screen.getByText(testCourse.professor!), screen.getByText("공학관 101"), screen.getByRole("button", { name: `${testCourse.name} 관심 강의 추가` }), screen.getByRole("button", { name: `${testCourse.name} 여석 알림 받기` })]) {
      expect(element.closest("[data-clarity-unmask]")).toBeNull();
    }
  });

  it("연속 교시가 합쳐져 실제 시간으로 표시되고 공강은 유지된다", () => {
    const classTime = "수 6-A,수 11-B,수 6-B,수 7-A,수 10-B,수 11-A";
    render(<CourseTable courses={[{ ...testCourse, classTime }]} />);

    expect(screen.getByText("수 14:00–15:30, 수 18:30–20:00")).toBeVisible();
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

  it("페이지 추가가 다시 가능해지면 계속 보이는 목록 끝을 다시 관찰한다", () => {
    const loadMore = vi.fn();
    const disconnect = vi.fn();
    vi.stubGlobal("IntersectionObserver", class {
      constructor(private readonly callback: IntersectionObserverCallback) {}

      observe() {
        this.callback([{ isIntersecting: true } as IntersectionObserverEntry], this as unknown as IntersectionObserver);
      }

      disconnect = disconnect;
    });

    const courses = [testCourse];
    const { rerender } = render(<CourseTable courses={courses} hasMore />);
    expect(loadMore).not.toHaveBeenCalled();

    rerender(<CourseTable courses={courses} hasMore onLoadMore={loadMore} />);

    expect(disconnect).toHaveBeenCalledOnce();
    expect(loadMore).toHaveBeenCalledOnce();
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
