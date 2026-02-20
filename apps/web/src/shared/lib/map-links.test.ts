import { describe, expect, it } from "vitest";
import { getCampusMapQuery, getMapSearchUrls } from "./map-links";

describe("map-links", () => {
  it("강의실 문자열에서 건물명 중심 검색어를 만든다", () => {
    expect(getCampusMapQuery("전주:미술관 2040")).toBe("전북대학교 전주 미술관");
    expect(getCampusMapQuery("공7-204")).toBe("전북대학교 공7");
  });

  it("온라인 강의는 지도 검색어를 만들지 않는다", () => {
    expect(getCampusMapQuery("온라인")).toBeNull();
  });

  it("카카오/네이버 검색 링크를 만든다", () => {
    const urls = getMapSearchUrls("인문대학1호관 404");
    expect(urls).not.toBeNull();
    expect(urls?.kakao).toContain("map.kakao.com");
    expect(urls?.naver).toContain("map.naver.com");
  });
});

