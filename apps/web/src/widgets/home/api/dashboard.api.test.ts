import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api/client", () => ({
  default: {
    get: vi.fn(),
  },
}));

const loadModules = async () => {
  const apiModule = await import("@/shared/api/client");
  const dashboardApi = await import("./dashboard.api");
  return {
    api: apiModule.default as { get: ReturnType<typeof vi.fn> },
    dashboardApi,
  };
};

describe("dashboard.api", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  it("대시보드 스냅샷 요청은 인증 갱신 재시도를 스킵한다", async () => {
    const { api, dashboardApi } = await loadModules();
    api.get.mockResolvedValue({
      data: {
        code: "SUCCESS",
        message: "ok",
        data: null,
      },
    });

    await dashboardApi.getDashboardSnapshot();

    expect(api.get).toHaveBeenCalledWith(
      "/api/v1/dashboard",
      expect.objectContaining({ skipAuthRefresh: true })
    );
  });
});
