import { describe, expect, it } from "vitest";
import { getCampusMapQuery, getMapSearchUrls } from "./map-links";

describe("map-links", () => {
  it("다양한 호수/방 번호 형태의 강의실 문자열에서 건물명 중심 검색어를 만든다", () => {
    expect(getCampusMapQuery("전주:미술관 2040")).toBe("전북대학교 전주 미술관");
    expect(getCampusMapQuery("공대7호관 301호")).toBe("전북대학교 공대7호관");
    expect(getCampusMapQuery("인문대 101")).toBe("전북대학교 인문대");
    expect(getCampusMapQuery("상대1호관 204관")).toBe("전북대학교 상대1호관");
    expect(getCampusMapQuery("농대 502실")).toBe("전북대학교 농대");
    expect(getCampusMapQuery("의대 102호(실)")).toBe("전북대학교 의대");
    expect(getCampusMapQuery("자연대1호관 403-1")).toBe("전북대학교 자연대1호관");
    expect(getCampusMapQuery("진수당 351")).toBe("전북대학교 진수당");
    expect(getCampusMapQuery("공7-204")).toBe("전북대학교 공7");
  });

  it("온라인 강의는 지도 검색어를 만들지 않는다", () => {
    expect(getCampusMapQuery("온라인")).toBeNull();
    expect(getCampusMapQuery("원격수업")).toBeNull();
    expect(getCampusMapQuery("비대면 (zoom)")).toBeNull();
  });

  it("카카오/네이버 검색 링크를 만든다", () => {
    const urls = getMapSearchUrls("인문대학1호관 404");
    expect(urls).not.toBeNull();
    expect(urls?.kakao).toContain("map.kakao.com");
    expect(urls?.naver).toContain("map.naver.com");
  });
});
