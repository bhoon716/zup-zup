import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AdminSchedulePanel } from "./admin-schedule-panel";

const mockAdminSchedules = vi.fn();
const mockCreateSchedule = vi.fn();
const mockUpdateSchedule = vi.fn();
const mockDeleteSchedule = vi.fn();
const mockRestoreSchedule = vi.fn();

vi.mock("@/features/schedule/hooks/useSchedules", () => ({
  useAdminSchedules: () => ({ data: mockAdminSchedules(), isLoading: false }),
  useCreateSchedule: () => ({ mutate: mockCreateSchedule, isPending: false }),
  useUpdateSchedule: () => ({ mutate: mockUpdateSchedule, isPending: false }),
  useDeleteSchedule: () => ({ mutate: mockDeleteSchedule, isPending: false }),
  useRestoreSchedule: () => ({ mutate: mockRestoreSchedule, isPending: false }),
}));

const schedule = {
  id: 1, scheduleType: "새 학사 일정", startDate: "2026-07-01", endDate: "2026-07-02",
  startTime: "09:00", endTime: "18:00", dDay: "D-1", ddayEnabled: true, deletedAt: null,
};

beforeEach(() => {
  vi.clearAllMocks();
  mockAdminSchedules.mockReturnValue([schedule]);
});

describe("AdminSchedulePanel", () => {
  it("커스텀 일정의 날짜를 수정해도 기존 대표 지정과 시간을 보존한다", () => {
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));
    expect(screen.getByPlaceholderText("입력")).toHaveValue(schedule.scheduleType);
    expect(screen.getByRole("checkbox", { name: "대표 D-day로 표시" })).toBeChecked();
    fireEvent.change(screen.getByLabelText("시작일"), { target: { value: "2026-07-02" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(mockUpdateSchedule.mock.calls[0][0]).toEqual({
      id: 1, request: {
        scheduleType: schedule.scheduleType, startDate: "2026-07-02", endDate: schedule.endDate,
        startTime: "09:00", endTime: "18:00", ddayEnabled: true,
      },
    });
  });

  it("대표 지정을 해제할 수 있고 새 일정에는 이전 편집 값이 남지 않는다", () => {
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));
    fireEvent.click(screen.getByRole("checkbox", { name: "대표 D-day로 표시" }));
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(mockUpdateSchedule.mock.calls[0][0].request.ddayEnabled).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: "취소" }));
    fireEvent.click(screen.getByRole("button", { name: "일정 추가" }));
    expect(screen.getByRole("checkbox", { name: "대표 D-day로 표시" })).not.toBeChecked();
    expect(screen.getByLabelText("시작일")).toHaveValue("");
  });

  it("삭제를 취소하면 요청하지 않고, 확인한 삭제는 복원 가능함을 안내한다", () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 삭제" }));
    expect(mockDeleteSchedule).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 삭제" }));
    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("복원할 수 있습니다"));
    expect(mockDeleteSchedule.mock.calls[0][0]).toBe(1);
    confirm.mockRestore();
  });

  it("삭제된 일정의 대표 지정은 표시하고 수정 대신 복원을 제공한다", () => {
    mockAdminSchedules.mockReturnValue([{ ...schedule, deletedAt: "2026-07-01T10:00:00" }]);
    render(<AdminSchedulePanel />);
    expect(screen.getByText("삭제됨")).toBeInTheDocument();
    expect(screen.getByText("대표 D-day")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "새 학사 일정 수정" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 복원" }));
    expect(mockRestoreSchedule.mock.calls[0][0]).toBe(1);
  });

  it("저장 실패는 입력을 유지하고 다시 시도할 수 있게 알린다", () => {
    mockUpdateSchedule.mockImplementationOnce((_request, options) => options.onError());
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(screen.getByRole("alert")).toHaveTextContent("저장하지 못했습니다");
    expect(screen.getByPlaceholderText("입력")).toHaveValue(schedule.scheduleType);
    expect(screen.getByRole("button", { name: "저장" })).toBeEnabled();
  });
});
