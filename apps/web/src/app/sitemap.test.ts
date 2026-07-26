import { describe, expect, it } from "vitest";
import sitemap from "@/app/sitemap";
import robots from "@/app/robots";

describe("App Route Metadata Suite", () => {
  it("returns sitemap entries with baseUrl and correct priority", async () => {
    const result = await sitemap();
    expect(result).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ url: "https://zup-zup.com", priority: 1.0 }),
        expect.objectContaining({ url: "https://zup-zup.com/search", priority: 0.9 }),
        expect.objectContaining({ url: "https://zup-zup.com/announcements", priority: 0.7 }),
      ])
    );
  });

  it("returns robots configuration with sitemap and userAgent rules", () => {
    const config = robots();
    expect(config.sitemap).toBe("https://zup-zup.com/sitemap.xml");
    expect(config.rules).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ userAgent: "*", allow: "/" }),
      ])
    );
  });
});
