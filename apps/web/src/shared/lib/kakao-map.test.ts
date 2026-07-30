import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { escapeHtml, renderKakaoMapByKeyword } from "./kakao-map";

describe("kakao-map escapeHtml 및 InfoWindow XSS 검증", () => {
  it("HTML 특수 문자(&, <, >, \", ')를 올바르게 이스케이프한다", () => {
    expect(escapeHtml("<script>alert('xss & \"test\"')</script>")).toBe(
      "&lt;script&gt;alert(&#39;xss &amp; &quot;test&quot;&#39;)&lt;/script&gt;"
    );
  });

  describe("renderKakaoMapByKeyword InfoWindow 이스케이프", () => {
    let mockInfoWindowContent = "";
    let dummyContainer: HTMLElement;

    beforeEach(() => {
      mockInfoWindowContent = "";
      dummyContainer = document.createElement("div");

      class MockLatLng {
        lat: number;
        lng: number;
        constructor(lat: number, lng: number) {
          this.lat = lat;
          this.lng = lng;
        }
        getLat() {
          return this.lat;
        }
        getLng() {
          return this.lng;
        }
      }

      class MockMap {
        setCenter() {}
        setLevel() {}
      }

      class MockMarker {
        setMap() {}
      }

      class MockInfoWindow {
        content: string;
        constructor(options: { content: string }) {
          this.content = options.content;
          mockInfoWindowContent = options.content;
        }
        open() {}
        close() {}
      }

      const mockKeywordSearch = vi
        .fn()
        .mockImplementation((keyword: string, callback: (data: unknown[], status: string) => void) => {
          callback(
            [
              {
                id: "1",
                place_name: `<img src=x onerror=alert('xss')> & "건물"`,
                address_name: "전북 전주시 덕진구",
                x: "127.129558",
                y: "35.846521",
              },
            ],
            "OK"
          );
        });

      class MockPlaces {
        keywordSearch = mockKeywordSearch;
      }

      window.kakao = {
        maps: {
          load: (cb: () => void) => cb(),
          LatLng: MockLatLng as any,
          Map: MockMap as any,
          Marker: MockMarker as any,
          InfoWindow: MockInfoWindow as any,
          services: {
            Status: { OK: "OK", ZERO_RESULT: "ZERO_RESULT", ERROR: "ERROR" },
            Places: MockPlaces as any,
          },
        },
      };
    });

    afterEach(() => {
      delete window.kakao;
    });

    it("외부 장소명(place_name)에 포함된 악성 HTML/script 태그를 InfoWindow content 생성 시 이스케이프한다", async () => {
      const result = await renderKakaoMapByKeyword({
        container: dummyContainer,
        appKey: "test-app-key",
        keyword: "인문대",
      });

      expect(result.status).toBe("OK");
      // InfoWindow HTML content 내 place_name이 raw HTML이 아닌 이스케이프된 형태여야 함
      expect(mockInfoWindowContent).not.toContain("<img src=x onerror=alert('xss')>");
      expect(mockInfoWindowContent).toContain("&lt;img src=x onerror=alert(&#39;xss&#39;)&gt; &amp; &quot;건물&quot;");
    });
  });
});
