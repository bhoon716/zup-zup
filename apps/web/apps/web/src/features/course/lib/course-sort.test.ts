import { describe, expect, it } from "vitest";
import { getDefaultCourseSortOrder } from "./course-sort";

describe("course sort", () => {
  it("인기순은 찜 수가 많은 강의가 먼저 오도록 내림차순을 기본값으로 사용한다", () => {
    expect(getDefaultCourseSortOrder("popular")).toBe("desc");
  });

  it("강의명 정렬은 가나다순으로 볼 수 있도록 오름차순을 기본값으로 사용한다", () => {
    expect(getDefaultCourseSortOrder("name")).toBe("asc");
  });
});
