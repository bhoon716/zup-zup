import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/shared/api/client", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

type MockedApiClient = {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  delete: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
};

const loadModules = async () => {
  const apiModule = await import("@/shared/api/client");
  const userApi = await import("./user.api");
  return {
    api: apiModule.default as unknown as MockedApiClient,
    userApi,
  };
};

describe("user.api", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    vi.useRealTimers();
  });

  it("getMyProfile는 동시 호출을 하나의 요청으로 합친다", async () => {
    vi.useFakeTimers();
    const { api, userApi } = await loadModules();
    api.get.mockResolvedValue({
      data: {
        code: "SUCCESS",
        message: "ok",
        data: { id: 1, name: "테스터" },
      },
    });

    const first = userApi.getMyProfile();
    const second = userApi.getMyProfile();

    expect(api.get).toHaveBeenCalledTimes(1);
    await Promise.all([first, second]);

    vi.advanceTimersByTime(101);
    api.get.mockResolvedValueOnce({
      data: {
        code: "SUCCESS",
        message: "ok",
        data: { id: 1, name: "테스터" },
      },
    });

    await userApi.getMyProfile();
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it("unregisterDevice는 토큰을 URL 인코딩해 삭제 요청을 보낸다", async () => {
    const { api, userApi } = await loadModules();
    api.delete.mockResolvedValue({
      data: {
        code: "SUCCESS",
        message: "ok",
        data: undefined,
      },
    });

    await userApi.unregisterDevice("token/with?unsafe=value");

    expect(api.delete).toHaveBeenCalledWith(
      "/api/v1/users/devices/token/token%2Fwith%3Funsafe%3Dvalue"
    );
  });

  it("sendTestNotification은 사용자 테스트 알림 API를 호출한다", async () => {
    const { api, userApi } = await loadModules();
    api.post.mockResolvedValue({
      data: {
        code: "SUCCESS",
        message: "ok",
        data: undefined,
      },
    });

    await userApi.sendTestNotification();

    expect(api.post).toHaveBeenCalledWith("/api/v1/notifications/test");
  });
});
