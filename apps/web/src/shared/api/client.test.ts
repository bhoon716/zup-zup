import { beforeEach, describe, expect, it, vi } from "vitest";

describe("shared api client", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("로컬 API URL에서는 ngrok 경고 헤더를 붙이지 않는다", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://localhost:8080");

    const { default: api } = await import("./client");

    expect((api.defaults.headers.common as Record<string, unknown>)["ngrok-skip-browser-warning"]).toBeUndefined();
  });
});
