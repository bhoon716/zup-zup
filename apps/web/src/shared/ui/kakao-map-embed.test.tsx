import { render, act } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { KakaoMapEmbed } from "./kakao-map-embed";

const mockRenderKakaoMapByKeyword = vi.fn();

vi.mock("@/shared/lib/kakao-map", () => ({
  renderKakaoMapByKeyword: (...args: unknown[]) => mockRenderKakaoMapByKeyword(...args),
}));

describe("KakaoMapEmbed", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
    process.env.NEXT_PUBLIC_KAKAO_MAP_JS_KEY = "test-key";
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("짧은 mount/unmount 재실행에서도 장소 검색은 한 번만 호출한다", async () => {
    mockRenderKakaoMapByKeyword.mockResolvedValue({
      status: "OK",
      place: {
        id: "1",
        place_name: "테스트 장소",
        address_name: "전북 전주",
        x: "127.1",
        y: "35.8",
      },
    });

    const first = render(<KakaoMapEmbed query="전북대학교 전주캠퍼스" />);
    first.unmount();

    render(<KakaoMapEmbed query="전북대학교 전주캠퍼스" />);

    await act(async () => {
      vi.runOnlyPendingTimers();
      await Promise.resolve();
    });

    expect(mockRenderKakaoMapByKeyword).toHaveBeenCalledTimes(1);
  });
});
