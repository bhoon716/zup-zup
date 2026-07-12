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
};

const loadModules = async () => {
  const apiModule = await import("@/shared/api/client");
  const feedbackApi = await import("./feedback.api");
  return {
    api: apiModule.default as unknown as MockedApiClient,
    feedbackApi,
  };
};

describe("feedback.api", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  it("관리자 목록 요청에 삭제 상태 필터를 포함한다", async () => {
    const { api, feedbackApi } = await loadModules();
    api.get.mockResolvedValue({ data: { code: "SUCCESS", message: "ok", data: {} } });

    await feedbackApi.getFeedbacksForAdmin(2, "DELETED");

    expect(api.get).toHaveBeenCalledWith("/api/v1/admin/feedbacks", {
      params: { page: 2, size: 20, deletion: "DELETED" },
    });
  });

  it("관리자 첨부파일 다운로드는 확인 값을 담은 POST와 blob 응답을 사용한다", async () => {
    const { api, feedbackApi } = await loadModules();
    const blob = new Blob(["attachment"]);
    api.post.mockResolvedValue({ data: blob });

    const result = await feedbackApi.downloadFeedbackAttachmentForAdmin(10, 20);

    expect(result).toBe(blob);
    expect(api.post).toHaveBeenCalledWith(
      "/api/v1/admin/feedbacks/10/attachments/20/download",
      { confirmed: true },
      { responseType: "blob" }
    );
  });
});
