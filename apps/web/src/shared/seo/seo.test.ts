import { describe, expect, it } from "vitest";
import { generateArticleJsonLd, generateCourseJsonLd, generateWebsiteJsonLd } from "@/shared/seo/json-ld";
import sitemap from "@/app/sitemap";
import robots from "@/app/robots";

describe("SEO & Metadata Suite", () => {
  it("generates Website & SoftwareApplication JSON-LD schema", () => {
    const jsonLd = generateWebsiteJsonLd();
    expect(jsonLd["@context"]).toBe("https://schema.org");
    expect(jsonLd["@graph"]).toHaveLength(2);

    const website = jsonLd["@graph"][0];
    expect(website["@type"]).toBe("WebSite");
    expect(website.url).toBe("https://zup-zup.com");

    const app = jsonLd["@graph"][1];
    expect(app["@type"]).toBe("SoftwareApplication");
    expect(app.name).toBe("줍줍");
  });

  it("generates Course JSON-LD schema", () => {
    const courseJsonLd = generateCourseJsonLd({
      name: "컴퓨팅사고와코딩",
      code: "CS101",
      professor: "홍길동",
      department: "소프트웨어공학과",
      credits: "3.0",
    });

    expect(courseJsonLd["@context"]).toBe("https://schema.org");
    expect(courseJsonLd["@type"]).toBe("Course");
    expect(courseJsonLd.name).toBe("컴퓨팅사고와코딩");
    expect(courseJsonLd.courseCode).toBe("CS101");
  });

  it("generates Article JSON-LD schema", () => {
    const articleJsonLd = generateArticleJsonLd({
      title: "수강신청 안내",
      description: "2026학년도 수강신청 일정 안내입니다.",
      url: "https://zup-zup.com/announcements/1",
    });

    expect(articleJsonLd["@context"]).toBe("https://schema.org");
    expect(articleJsonLd["@type"]).toBe("NewsArticle");
    expect(articleJsonLd.headline).toBe("수강신청 안내");
    expect(articleJsonLd.url).toBe("https://zup-zup.com/announcements/1");
  });

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
