import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { CourseSmartFilters } from "./course-smart-filters";
import type { CourseSearchCondition } from "@/shared/types/api";

vi.mock("@/features/user/hooks/useUser", () => ({
  useUser: () => ({ data: { id: 1, name: "사용자" } }),
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

vi.mock("sonner", () => ({
  toast: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

describe("CourseSmartFilters", () => {
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
});
