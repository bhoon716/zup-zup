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

const readBlobText = (blob: Blob) => new Promise<string>((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(String(reader.result));
  reader.onerror = () => reject(reader.error);
  reader.readAsText(blob);
});

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

  it("피드백 생성 요청은 JSON metaInfo 문자열을 multipart feedback 파트에 담는다", async () => {
    const { api, feedbackApi } = await loadModules();
    api.post.mockResolvedValue({ data: { code: "SUCCESS", message: "ok", data: 10 } });

    await feedbackApi.createFeedback({
      type: "BUG",
      title: "제목",
      content: "내용",
      metaInfo: JSON.stringify({ os: "MacIntel", language: "ko-KR" }),
    }, []);

    const formData = api.post.mock.calls[0][1] as FormData;
    const feedbackPart = formData.get("feedback") as Blob;
    const request = JSON.parse(await readBlobText(feedbackPart));

    expect(JSON.parse(request.metaInfo)).toEqual({ os: "MacIntel", language: "ko-KR" });
    expect(api.post).toHaveBeenCalledWith(
      "/api/v1/feedbacks",
      expect.any(FormData),
      { headers: { "Content-Type": "multipart/form-data" } }
    );
  });
});
