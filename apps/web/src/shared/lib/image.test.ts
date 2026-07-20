import { afterEach, describe, expect, it, vi } from "vitest";
import { compressImage, isSupportedImageType } from "./image";

describe("compressImage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("피드백 첨부를 JPEG로 압축한다", async () => {
    let outputType = "";
    class MockImage {
      width = 640;
      height = 480;
      onload: (() => void) | null = null;
      onerror: ((error: unknown) => void) | null = null;

      set src(_value: string) {
        queueMicrotask(() => this.onload?.());
      }
    }
    vi.stubGlobal("Image", MockImage);

    const context = { drawImage: vi.fn() };
    vi.spyOn(document, "createElement").mockImplementation((tagName: string) => {
      if (tagName !== "canvas") {
        return document.createElementNS("http://www.w3.org/1999/xhtml", tagName);
      }
      return {
        width: 0,
        height: 0,
        getContext: () => context,
        toBlob: (callback: BlobCallback, type?: string) => {
          outputType = type ?? "";
          callback(new Blob(["compressed"], { type: outputType }));
        },
      } as unknown as HTMLCanvasElement;
    });

    const compressed = await compressImage(new File(["source"], "capture.jpg", { type: "image/jpeg" }));

    expect(outputType).toBe("image/jpeg");
    expect(compressed.type).toBe("image/jpeg");
    expect(compressed.name).toBe("capture.jpg");
    expect(context.drawImage).toHaveBeenCalled();
  });

  it("WebP를 허용 포맷으로 취급하지 않는다", () => {
    expect(isSupportedImageType(new File(["webp"], "capture.webp", { type: "image/webp" }))).toBe(false);
    expect(isSupportedImageType(new File(["png"], "capture.png", { type: "image/png" }))).toBe(true);
  });
});
