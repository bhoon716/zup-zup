import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import sitemap from "@/app/sitemap";
import robots from "@/app/robots";

describe("App Route Metadata Suite", () => {
  beforeEach(() => {
    vi.stubEnv("API_URL", "");
    vi.stubEnv("NEXT_PUBLIC_API_URL", "");
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
  });

  it("직접 반환된 공지 페이지의 content로 상세 사이트맵 URL을 만든다", async () => {
    vi.stubEnv("API_URL", "http://metadata.test");
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [{ id: 7, updatedAt: "2026-10-26T10:00:00Z" }], last: true }),
    }));
    const routes = await sitemap();
    expect(routes).toContainEqual(expect.objectContaining({
      url: "https://www.zup-zup.com/announcements/7",
      lastModified: new Date("2026-10-26T10:00:00Z"),
    }));
  });
  it("returns sitemap entries with baseUrl and correct priority", async () => {
    const result = await sitemap();
    expect(result).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ url: "https://www.zup-zup.com", priority: 1.0 }),
        expect.objectContaining({ url: "https://www.zup-zup.com/search", priority: 0.9 }),
        expect.objectContaining({ url: "https://www.zup-zup.com/announcements", priority: 0.7 }),
      ])
    );
  });

  it("returns robots configuration with sitemap and userAgent rules", () => {
    const config = robots();
    expect(config.sitemap).toBe("https://www.zup-zup.com/sitemap.xml");
    expect(config.rules).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ userAgent: "*", allow: "/" }),
      ])
    );
  });
});
