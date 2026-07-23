import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";
import type { ScheduleResponse } from "@/shared/types/api";
import { DashboardDDayBlock } from "./dashboard-dday-block";

vi.mock("@/features/schedule/hooks/useSchedules", () => ({
  useUpcomingSchedules: vi.fn(),
}));

function createSchedule(id: number): ScheduleResponse {
  return {
    id,
    scheduleType: `주요 일정 ${id}`,
    startDate: "2026-07-23",
    endDate: "2026-07-23",
    dDay: `D-${id}`,
  };
}

describe("DashboardDDayBlock", () => {
  beforeEach(() => {
    vi.mocked(useUpcomingSchedules).mockReturnValue({
      data: undefined,
      isLoading: false,
    } as never);
  });

  it("일정이 많으면 모든 항목을 유지한 채 반응형 최대 높이 안에서 세로로 스크롤한다", () => {
    const schedules = Array.from({ length: 8 }, (_, index) => createSchedule(index + 1));

    render(<DashboardDDayBlock upcomingSchedules={schedules} />);

    const list = screen.getByRole("list", { name: "주요 일정 목록" });
    expect(list).toHaveClass(
      "max-h-[24rem]",
      "md:max-h-[30rem]",
      "overflow-y-auto",
      "overflow-x-hidden",
    );
    expect(list).toHaveAttribute("tabindex", "0");
    expect(screen.getAllByRole("listitem")).toHaveLength(8);
    expect(screen.getByText("주요 일정 8")).toBeInTheDocument();
  });

  it("일정이 적으면 고정 높이를 강제하지 않고 모든 항목을 표시한다", () => {
    render(<DashboardDDayBlock upcomingSchedules={[createSchedule(1), createSchedule(2)]} />);

    const list = screen.getByRole("list", { name: "주요 일정 목록" });
    expect(list).not.toHaveClass("h-[24rem]", "md:h-[30rem]");
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
  });

  it("빈 목록은 기존 안내 상태를 유지하고 불필요한 키보드 정지점을 만들지 않는다", () => {
    render(<DashboardDDayBlock upcomingSchedules={[]} />);

    expect(screen.getByText("현재 등록된 주요 일정이 없습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "주요 일정 목록" })).not.toBeInTheDocument();
  });

  it("로딩 중에는 기존 높이를 유지하면서 진행 상태를 알린다", () => {
    vi.mocked(useUpcomingSchedules).mockReturnValue({
      data: undefined,
      isLoading: true,
    } as never);

    render(<DashboardDDayBlock />);

    expect(screen.getByRole("status", { name: "주요 일정 불러오는 중" })).toHaveClass(
      "min-h-[150px]",
    );
    expect(screen.queryByRole("list", { name: "주요 일정 목록" })).not.toBeInTheDocument();
  });
});
