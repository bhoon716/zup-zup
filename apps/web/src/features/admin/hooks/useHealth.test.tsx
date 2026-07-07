import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderHook, waitFor } from "@testing-library/react";
import * as healthApi from "@/features/admin/api/health.api";
import { useHealth } from "./useHealth";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";

vi.mock("@/features/admin/api/health.api", () => ({
  checkHealth: vi.fn(),
}));

describe("useHealth hook", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("헬스체크 데이터를 조회한다", async () => {
    vi.mocked(healthApi.checkHealth).mockResolvedValue({
      code: "SUCCESS",
      message: "ok",
      data: {
        status: "UP",
        version: "1.0.0",
        buildTime: "2026-02-17T00:00:00",
        timestamp: "2026-02-17T12:00:00",
      },
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useHealth(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.status).toBe("UP");
  });
});
