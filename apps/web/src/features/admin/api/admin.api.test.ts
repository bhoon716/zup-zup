import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api/client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

type MockedApiClient = {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
};

const loadModules = async () => {
  const apiModule = await import("@/shared/api/client");
  const adminApi = await import("./admin.api");
  return {
    api: apiModule.default as unknown as MockedApiClient,
    adminApi,
  };
};

describe("admin notification delivery API", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  it("DLQ 목록을 페이지 파라미터와 함께 요청한다", async () => {
    const { api, adminApi } = await loadModules();
    api.get.mockResolvedValue({ data: { code: "SUCCESS", message: "ok", data: {} } });

    await adminApi.getAdminDlqNotificationDeliveries(2, 30);

    expect(api.get).toHaveBeenCalledWith("/api/v1/admin/notification-deliveries/dlq", {
      params: { page: 2, size: 30 },
    });
  });

  it("선택한 delivery의 안전한 상세 DTO를 요청한다", async () => {
    const { api, adminApi } = await loadModules();
    api.get.mockResolvedValue({ data: { code: "SUCCESS", message: "ok", data: {} } });

    await adminApi.getAdminNotificationDelivery(41);

    expect(api.get).toHaveBeenCalledWith("/api/v1/admin/notification-deliveries/41");
  });

  it("재처리 요청은 명시한 SENT override 값만 전송한다", async () => {
    const { api, adminApi } = await loadModules();
    api.post.mockResolvedValue({ data: { code: "SUCCESS", message: "ok", data: {} } });

    await adminApi.replayAdminNotificationDelivery(41, { forceSentReplay: true });

    expect(api.post).toHaveBeenCalledWith("/api/v1/admin/notification-deliveries/41/replay", {
      forceSentReplay: true,
    });
  });
});
