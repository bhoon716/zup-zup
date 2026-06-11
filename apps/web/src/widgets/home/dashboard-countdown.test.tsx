import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardCountdown } from "./dashboard-countdown";
import { useActiveDday } from "@/features/admin/hooks/useDday";

vi.mock("@/features/admin/hooks/useDday", () => ({
  useActiveDday: vi.fn(),
}));

describe("DashboardCountdown", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useActiveDday).mockReturnValue({ data: undefined } as never);
  });

  it("부모가 일정 조회를 이미 수행 중이면 추가 조회를 하지 않는다", () => {
    render(
      <DashboardCountdown
        suppressFetch
      />
    );

    expect(useActiveDday).toHaveBeenCalledWith(false);
  });
});
