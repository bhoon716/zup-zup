import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { getAnnouncementMetadata, getCourseMetadata } from "../lib/public-metadata";

describe("public metadata branding", () => {
  beforeEach(() => {
    vi.stubEnv("API_URL", "");
    vi.stubEnv("NEXT_PUBLIC_API_URL", "");
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("강의·공지 fallback metadata에도 공유 이미지를 유지한다", async () => {
    const courseMetadata = await getCourseMetadata("CS101");
    const announcementMetadata = await getAnnouncementMetadata(1);

    expect(courseMetadata.openGraph).toEqual(expect.objectContaining({
      images: [{
        url: "/icons/zup-zup-512.png",
        width: 512,
        height: 512,
        alt: "줍줍 로고",
      }],
    }));
    expect(courseMetadata.twitter).toEqual(expect.objectContaining({
      images: ["/icons/zup-zup-512.png"],
    }));
    expect(announcementMetadata.openGraph).toEqual(expect.objectContaining({
      images: [{
        url: "/icons/zup-zup-512.png",
        width: 512,
        height: 512,
        alt: "줍줍 로고",
      }],
    }));
    expect(announcementMetadata.twitter).toEqual(expect.objectContaining({
      images: ["/icons/zup-zup-512.png"],
    }));
  });
});
