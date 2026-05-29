import { renderHook } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { usePWAInstall } from "./usePWAInstall";

if (!window.matchMedia) {
  window.matchMedia = vi.fn().mockReturnValue({
    matches: false,
    media: "(display-mode: standalone)",
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
  } as unknown as MediaQueryList);
}

describe("usePWAInstall", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
  });

  it("개발 환경에서는 beforeinstallprompt 리스너를 등록하지 않는다", () => {
    vi.stubEnv("NODE_ENV", "development");
    const addSpy = vi.spyOn(window, "addEventListener");

    renderHook(() => usePWAInstall());

    expect(addSpy).not.toHaveBeenCalledWith("beforeinstallprompt", expect.any(Function));
  });

  it("프로덕션 환경에서는 beforeinstallprompt 리스너를 등록하고 해제한다", () => {
    vi.stubEnv("NODE_ENV", "production");
    const addSpy = vi.spyOn(window, "addEventListener");
    const removeSpy = vi.spyOn(window, "removeEventListener");

    const { unmount } = renderHook(() => usePWAInstall());

    expect(addSpy).toHaveBeenCalledWith("beforeinstallprompt", expect.any(Function));

    unmount();

    expect(removeSpy).toHaveBeenCalledWith("beforeinstallprompt", expect.any(Function));
  });
});
