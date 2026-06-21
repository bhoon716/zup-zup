import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as adminApi from "@/features/admin/api/admin.api";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import { useAdminDashboardSnapshot } from "./useAdminDashboard";

vi.mock("@/features/admin/api/admin.api", () => ({
  getDashboardSnapshot: vi.fn(),
}));

describe("useAdminDashboardSnapshot hook", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("관리자 개요와 크롤링 타겟을 한 번에 조회한다", async () => {
    vi.mocked(adminApi.getDashboardSnapshot).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
        overview: {
          totalUsers: 100,
          totalActiveSubscriptions: 50,
          todayNotificationCount: 20,
          crawlingStatus: "RUNNING",
          lastCrawledAt: "2026-01-01T00:00:00",
          jbnuLatencyMs: 42,
          serverTime: "2026-01-01T00:01:00",
          notificationTraffic: [],
          recentLogs: [],
        },
        crawlTarget: {
          year: "2026",
          semester: "U211600020",
        },
      },
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useAdminDashboardSnapshot(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.overview.totalUsers).toBe(100);
    expect(result.current.data?.crawlTarget.year).toBe("2026");
    expect(adminApi.getDashboardSnapshot).toHaveBeenCalledTimes(1);
  });
});
