import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CourseSmartFilters } from "./course-smart-filters";
import type { CourseSearchCondition } from "@/shared/types/api";

const { mockUseUser, mockSetLoginModalOpen } = vi.hoisted(() => ({
  mockUseUser: vi.fn(),
  mockSetLoginModalOpen: vi.fn(),
}));

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: () => mockUseUser(),
}));

vi.mock("@/features/timetable/hooks/useTimetable", () => ({
  useTimetables: () => ({ data: [], refetch: vi.fn() }),
}));

vi.mock("../time-table-selector", () => ({
  TimeTableSelector: () => <div data-testid="time-table-selector">시간표</div>,
}));

vi.mock("@/features/timetable/api/timetable.api", () => ({
  timetableApi: {
    getTimetable: vi.fn(),
  },
}));

vi.mock("@/features/auth/store/useAuthStore", () => ({
  useAuthStore: (selector: (state: { setLoginModalOpen: (open: boolean) => void }) => unknown) =>
    selector({ setLoginModalOpen: mockSetLoginModalOpen }),
}));

vi.mock("sonner", () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

describe("CourseSmartFilters", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseUser.mockReturnValue({ data: { id: 1, name: "사용자" } });
  });

  it("공강 설정 버튼으로 시간표 선택 영역을 토글한다", () => {
    function Harness() {
      const [condition, setCondition] = useState<CourseSearchCondition>({
        academicYear: "2026",
        semester: "U211600010",
      } as CourseSearchCondition);
      const [scheduleOpen, setScheduleOpen] = useState(false);

      return (
        <CourseSmartFilters
          condition={condition}
          setCondition={setCondition}
          scheduleOpen={scheduleOpen}
          setScheduleOpen={setScheduleOpen}
        />
      );
    }

    render(<Harness />);

    const toggleButton = screen.getByRole("button", { name: "공강 시간표 펼치기" });
    const selector = screen.getByTestId("time-table-selector").parentElement as HTMLElement;

    expect(selector).toHaveAttribute("hidden");

    fireEvent.click(toggleButton);
    expect(selector).not.toHaveAttribute("hidden");

    fireEvent.click(toggleButton);
    expect(selector).toHaveAttribute("hidden");
  });

  it("비로그인 상태에서는 시간표 메뉴 대신 로그인 모달을 연다", async () => {
    const user = userEvent.setup();
    mockUseUser.mockReturnValue({ data: null });

    render(
      <CourseSmartFilters
        condition={{ academicYear: "2026", semester: "U211600010" } as CourseSearchCondition}
        setCondition={vi.fn()}
        scheduleOpen={false}
        setScheduleOpen={vi.fn()}
      />
    );

    await user.click(screen.getByRole("button", { name: "내 시간표에서 공강 불러오기" }));

    expect(mockSetLoginModalOpen).toHaveBeenCalledWith(true);
    expect(screen.queryByText("로그인이 필요합니다.")).not.toBeInTheDocument();
  });
});
