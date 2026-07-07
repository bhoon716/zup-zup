import { describe, expect, it } from "vitest";
import {
  formatClassification,
  formatGradingMethod,
  formatLanguage,
  formatRelativeTime,
} from "./formatters";

describe("formatters", () => {
  it("분류 코드를 한글 라벨로 변환한다", () => {
    expect(formatClassification("MAJOR_REQUIRED")).toBe("전공필수");
    expect(formatClassification("교양")).toBe("교양");
    expect(formatClassification(undefined)).toBe("-");
  });

  it("언어 코드를 한글 라벨로 변환한다", () => {
    expect(formatLanguage("EN")).toBe("영어");
    expect(formatLanguage("한국어")).toBe("한국어");
    expect(formatLanguage("")).toBe("-");
  });

  it("평가 방식을 표준 라벨로 변환한다", () => {
    expect(formatGradingMethod("RELATIVE_2")).toBe("상대평가Ⅱ");
    expect(formatGradingMethod("Pass/Fail")).toBe("Pass/Fail");
    expect(formatGradingMethod(undefined)).toBe("-");
  });

  it("상대 시간 포맷은 잘못된 입력을 안전하게 처리한다", () => {
    expect(formatRelativeTime("not-a-date")).toBe("-");
    expect(formatRelativeTime(undefined)).toBe("-");
    expect(formatRelativeTime("2026-01-01T00:00:00+09:00")).not.toBe("-");
  });
});
