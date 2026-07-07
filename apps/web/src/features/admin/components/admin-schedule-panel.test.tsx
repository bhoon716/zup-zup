import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AdminSchedulePanel } from "./admin-schedule-panel";

const mockAdminSchedules = vi.fn();
const mockCreateSchedule = vi.fn();
const mockUpdateSchedule = vi.fn();
const mockDeleteSchedule = vi.fn();

vi.mock("@/features/admin/hooks/useAdminSchedules", () => ({
  useAdminSchedules: () => ({
    data: mockAdminSchedules(),
    isLoading: false,
  }),
  useCreateSchedule: () => ({
    mutate: mockCreateSchedule,
    isPending: false,
  }),
  useUpdateSchedule: () => ({
    mutate: mockUpdateSchedule,
    isPending: false,
  }),
  useDeleteSchedule: () => ({
    mutate: mockDeleteSchedule,
    isPending: false,
  }),
}));

describe("AdminSchedulePanel", () => {
  it("커스텀 일정 수정 시 직접 입력 모드로 전환한다", () => {
    mockAdminSchedules.mockReturnValue([
      {
        id: 1,
        scheduleType: "새 학사 일정",
        startDate: "2026-07-01",
        endDate: "2026-07-02",
        startTime: "09:00",
        endTime: "18:00",
        dDay: "D-1",
      },
    ]);

    render(<AdminSchedulePanel />);

    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));

    expect(screen.getByPlaceholderText("입력")).toBeInTheDocument();
  });
});
