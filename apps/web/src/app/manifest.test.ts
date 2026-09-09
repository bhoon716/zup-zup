import manifest from "../../public/manifest.json";
import { describe, expect, it } from "vitest";

describe("PWA manifest branding", () => {
  it("uses separate square any and maskable brand icons", () => {
    expect(manifest.icons).toEqual([
      {
        src: "/icons/zup-zup-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/icons/zup-zup-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/icons/zup-zup-192-maskable.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "maskable",
      },
      {
        src: "/icons/zup-zup-512-maskable.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ]);
  });
});
