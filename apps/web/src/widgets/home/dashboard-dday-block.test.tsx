import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ScheduleResponse } from "@/shared/types/api";
import { DashboardDDayBlock } from "./dashboard-dday-block";

function createSchedule(id: number): ScheduleResponse {
  return {
    id,
    scheduleType: `주요 일정 ${id}`,
    startDate: "2026-07-23",
    endDate: "2026-07-23",
    dDay: `D-${id}`,
    ddayEnabled: false,
    deletedAt: null,
  };
}

describe("DashboardDDayBlock", () => {
  it("일정이 많아도 모든 항목을 표시하고 키보드로 목록에 접근할 수 있다", () => {
    const schedules = Array.from({ length: 8 }, (_, index) => createSchedule(index + 1));

    render(<DashboardDDayBlock upcomingSchedules={schedules} />);

    const list = screen.getByRole("list", { name: "주요 일정 목록" });
    expect(list).toHaveAttribute("tabindex", "0");
    expect(screen.getAllByRole("listitem")).toHaveLength(8);
    expect(screen.getByText("주요 일정 8")).toBeInTheDocument();
  });

  it("빈 목록은 기존 안내 상태를 유지하고 불필요한 키보드 정지점을 만들지 않는다", () => {
    render(<DashboardDDayBlock upcomingSchedules={[]} />);

    expect(screen.getByText("현재 등록된 주요 일정이 없습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "주요 일정 목록" })).not.toBeInTheDocument();
  });

  it("대표 지정되지 않은 일정과 진행 중인 일정도 일반 목록에 표시한다", () => {
    render(<DashboardDDayBlock upcomingSchedules={[{ ...createSchedule(1), dDay: "진행 중" }, createSchedule(2)]} />);
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText("진행 중")).toBeInTheDocument();
    expect(screen.getByText("주요 일정 2")).toBeInTheDocument();
  });
});
