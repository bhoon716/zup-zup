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
  startTime: "20:01", endTime: "23:57", dDay: "D-1", ddayEnabled: true, deletedAt: null,
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
    expect(screen.getByLabelText("시작 시간")).toHaveValue("20:01");
    expect(screen.getByLabelText("종료 시간")).toHaveValue("23:57");
    fireEvent.change(screen.getByLabelText("시작일"), { target: { value: "2026-07-02" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(mockUpdateSchedule.mock.calls[0][0]).toEqual({
      id: 1, request: {
        scheduleType: schedule.scheduleType, startDate: "2026-07-02", endDate: schedule.endDate,
        startTime: "20:01", endTime: "23:57", ddayEnabled: true,
      },
    });
  });

  it.each([
    ["20:00", "23:55"],
    ["09:01", "23:57"],
    ["00:00", "23:59"],
  ])("기본 시간 입력으로 24시간·분 단위 값을 저장한다 (%s→%s)", (startTime, endTime) => {
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));
    const start = screen.getByLabelText("시작 시간");
    const end = screen.getByLabelText("종료 시간");
    expect(start).toHaveAttribute("type", "time");
    expect(start).toHaveAttribute("step", "60");
    fireEvent.change(start, { target: { value: startTime } });
    fireEvent.change(end, { target: { value: endTime } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(mockUpdateSchedule.mock.calls[0][0].request).toMatchObject({ startTime, endTime, ddayEnabled: true });
  });

  it("시간을 초기화하면 날짜와 대표 지정을 유지하고 선택 시간을 생략한다", () => {
    render(<AdminSchedulePanel />);
    fireEvent.click(screen.getByRole("button", { name: "새 학사 일정 수정" }));
    fireEvent.click(screen.getByRole("button", { name: "시작 시간 초기화" }));
    fireEvent.click(screen.getByRole("button", { name: "종료 시간 초기화" }));
    expect(screen.getByLabelText("시작 시간")).toHaveValue("");
    expect(screen.getByLabelText("종료 시간")).toHaveValue("");
    fireEvent.click(screen.getByRole("button", { name: "저장" }));

    expect(mockUpdateSchedule.mock.calls[0][0].request).toEqual({
      scheduleType: schedule.scheduleType, startDate: schedule.startDate, endDate: schedule.endDate,
      startTime: undefined, endTime: undefined, ddayEnabled: true,
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
    expect(screen.getByLabelText("시작 시간")).toHaveValue("");
    expect(screen.getByLabelText("종료 시간")).toHaveValue("");
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
