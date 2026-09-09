import { describe, expect, it, vi } from "vitest";

const { mockFont } = vi.hoisted(() => ({
  mockFont: vi.fn(() => ({ variable: "mock-font" })),
}));

vi.mock("next/font/google", () => ({
  Geist_Mono: mockFont,
  Noto_Sans_KR: mockFont,
}));
vi.mock("@/widgets/header/header", () => ({ Header: () => null }));
vi.mock("@/app/providers", () => ({ default: () => null }));
vi.mock("@vercel/analytics/next", () => ({ Analytics: () => null }));
vi.mock("@vercel/speed-insights/next", () => ({ SpeedInsights: () => null }));
vi.mock("@/shared/analytics/third-party-analytics", () => ({ ThirdPartyAnalytics: () => null }));
vi.mock("@/shared/seo/json-ld", () => ({ generateWebsiteJsonLd: () => ({}) }));

import { metadata } from "./layout";

describe("root metadata branding", () => {
  it("표준 favicon, shortcut, Apple icon과 공유 이미지를 선언한다", () => {
    expect(metadata.icons).toEqual(expect.objectContaining({
      icon: [
        { url: "/icons/zup-zup-32.png", sizes: "32x32", type: "image/png" },
        { url: "/icons/zup-zup-192.png", sizes: "192x192", type: "image/png" },
      ],
      shortcut: [{ url: "/icons/zup-zup-32.png", sizes: "32x32", type: "image/png" }],
      apple: [{ url: "/icons/zup-zup-180.png", sizes: "180x180", type: "image/png" }],
    }));
    expect(metadata.openGraph).toEqual(expect.objectContaining({
      images: [{
        url: "/icons/zup-zup-512.png",
        width: 512,
        height: 512,
        alt: "줍줍 로고",
      }],
    }));
    expect(metadata.twitter).toEqual(expect.objectContaining({
      images: ["/icons/zup-zup-512.png"],
    }));
  });
});
