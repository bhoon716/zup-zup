import { beforeEach, describe, expect, it, vi } from "vitest";

describe("next config rewrites", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
  });

  it("falls back to localhost when API_URL is not set", async () => {
    vi.stubEnv("API_URL", "");
    vi.stubEnv("NEXT_PUBLIC_API_URL", "");

    const { default: nextConfig } = await import("../../../next.config");
    const rewrites = await nextConfig.rewrites?.();

    expect(rewrites).toEqual([
      {
        source: "/api/login/oauth2/code/:path*",
        destination: "http://localhost:8080/login/oauth2/code/:path*",
      },
      {
        source: "/api/oauth2/:path*",
        destination: "http://localhost:8080/oauth2/:path*",
      },
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
      {
        source: "/uploads/:path*",
        destination: "http://localhost:8080/uploads/:path*",
      },
    ]);
  });

  it("prefers an explicit API URL over the fallback", async () => {
    vi.stubEnv("API_URL", "https://api.example.com");
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://ignored.example.com");

    const { default: nextConfig } = await import("../../../next.config");
    const rewrites = await nextConfig.rewrites?.();

    expect(rewrites?.[2]).toEqual({
      source: "/api/:path*",
      destination: "https://api.example.com/api/:path*",
    });
  });

  it("leaves standalone output disabled for Vercel", async () => {
    const { default: nextConfig } = await import("../../../next.config");

    expect(nextConfig.output).toBeUndefined();
  });
});
