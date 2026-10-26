import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardCountdown } from "./dashboard-countdown";
import { useScheduleDdays } from "@/features/schedule/hooks/useSchedules";

vi.mock("@/features/schedule/hooks/useSchedules", () => ({ useScheduleDdays: vi.fn() }));
const selected = { id: 1, scheduleType: "수강 신청", dDay: "D-2", startDate: "2026-10-28", endDate: "2026-10-29", ddayEnabled: true, deletedAt: null };

describe("DashboardCountdown", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useScheduleDdays).mockReturnValue({ data: [] } as never);
  });

  it("부모가 조회를 보류한 동안 대표 일정을 추가 조회하지 않는다", () => {
    render(<DashboardCountdown suppressFetch />);
    expect(useScheduleDdays).toHaveBeenCalledWith(false);
    expect(useScheduleDdays).not.toHaveBeenCalledWith(true);
  });

  it("마운트 후 대표 목록을 조회하며 선택된 일정이 없으면 표시하지 않는다", async () => {
    const { container } = render(<DashboardCountdown />);
    await waitFor(() => expect(useScheduleDdays).toHaveBeenCalledWith(true));
    expect(container).toBeEmptyDOMElement();
  });

  it("서버가 정렬한 첫 대표 일정만 표시한다", () => {
    vi.mocked(useScheduleDdays).mockReturnValue({ data: [selected, { ...selected, id: 2, scheduleType: "두 번째 일정" }] } as never);
    render(<DashboardCountdown />);
    expect(screen.getByText("앞으로 수강 신청까지")).toBeInTheDocument();
    expect(screen.getByText("D-2")).toBeInTheDocument();
    expect(screen.queryByText(/두 번째 일정/)).not.toBeInTheDocument();
  });

  it("진행 중인 대표 일정은 미래 일정으로 안내하지 않는다", () => {
    vi.mocked(useScheduleDdays).mockReturnValue({ data: [{ ...selected, dDay: "진행 중" }] } as never);
    render(<DashboardCountdown />);
    expect(screen.getByText("수강 신청")).toBeInTheDocument();
    expect(screen.getByText("진행 중")).toBeInTheDocument();
    expect(screen.queryByText(/앞으로/)).not.toBeInTheDocument();
  });
});
