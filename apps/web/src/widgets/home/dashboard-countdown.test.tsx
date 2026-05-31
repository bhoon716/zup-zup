import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardCountdown } from "./dashboard-countdown";
import { useUpcomingSchedules } from "@/features/schedule/hooks/useSchedules";

vi.mock("@/features/schedule/hooks/useSchedules", () => ({
  useUpcomingSchedules: vi.fn(),
}));

describe("DashboardCountdown", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useUpcomingSchedules).mockReturnValue({ data: undefined } as never);
  });

  it("부모가 일정 조회를 이미 수행 중이면 추가 조회를 하지 않는다", () => {
    render(
      <DashboardCountdown
        suppressFetch
      />
    );

    expect(useUpcomingSchedules).toHaveBeenCalledWith(false);
  });
});
