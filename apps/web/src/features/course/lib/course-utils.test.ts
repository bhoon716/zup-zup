import { describe, expect, it } from "vitest";
import type { CourseSearchCondition } from "@/shared/types/api";
import { resolveVisibleSearchCondition } from "./course-utils";

const FALLBACK_DEFAULT_CONDITION: CourseSearchCondition = {
  academicYear: "2026",
  semester: "U211600010",
  disclosure: "공개",
  sortBy: "name",
  sortOrder: "asc",
};

describe("resolveVisibleSearchCondition", () => {
  it("기본 상태면 서버 기본 학기를 보여준다", () => {
    const resolvedDefaultCondition: CourseSearchCondition = {
      ...FALLBACK_DEFAULT_CONDITION,
      semester: "U211600015",
    };

    const result = resolveVisibleSearchCondition(
      { ...FALLBACK_DEFAULT_CONDITION },
      resolvedDefaultCondition,
      FALLBACK_DEFAULT_CONDITION,
    );

    expect(result.semester).toBe("U211600015");
  });

  it("사용자가 필터를 건드린 상태면 draft를 유지한다", () => {
    const draftCondition: CourseSearchCondition = {
      ...FALLBACK_DEFAULT_CONDITION,
      department: "컴퓨터공학부",
    };

    const resolvedDefaultCondition: CourseSearchCondition = {
      ...FALLBACK_DEFAULT_CONDITION,
      semester: "U211600015",
    };

    const result = resolveVisibleSearchCondition(
      draftCondition,
      resolvedDefaultCondition,
      FALLBACK_DEFAULT_CONDITION,
    );

    expect(result.department).toBe("컴퓨터공학부");
    expect(result.semester).toBe("U211600010");
  });
});
