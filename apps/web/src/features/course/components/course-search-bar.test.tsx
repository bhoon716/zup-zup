import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CourseSearchBar } from "./course-search-bar";

vi.mock("./filters/filter-section", () => ({
  FilterSection: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock("./filters/course-smart-filters", () => ({
  CourseSmartFilters: () => <div data-testid="smart-filters" />,
}));

vi.mock("./filters/course-basic-filters", () => ({
  CourseBasicFilters: () => <div data-testid="basic-filters" />,
}));

vi.mock("./filters/course-detail-filters", () => ({
  CourseDetailFilters: () => <div data-testid="detail-filters" />,
}));

describe("CourseSearchBar", () => {
  it("초기 조건이 바뀌면 입력값을 effect로 동기화한다", async () => {
    const onSearch = vi.fn();
    const { rerender } = render(
      <CourseSearchBar
        onSearch={onSearch}
        initialCondition={{ academicYear: "2026", semester: "U211600010", name: "첫번째" }}
      />,
    );

    expect(screen.getByPlaceholderText("강의명 또는 코드")).toHaveValue("첫번째");

    rerender(
      <CourseSearchBar
        onSearch={onSearch}
        initialCondition={{ academicYear: "2026", semester: "U211600010", name: "두번째" }}
      />,
    );

    await waitFor(() => {
      expect(screen.getByPlaceholderText("강의명 또는 코드")).toHaveValue("두번째");
    });
  });

  it("검색 버튼을 누르면 현재 조건으로 검색을 실행한다", () => {
    const onSearch = vi.fn();
    render(
      <CourseSearchBar
        onSearch={onSearch}
        initialCondition={{ academicYear: "2026", semester: "U211600010", name: "강의명" }}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "검색" }));

    expect(onSearch).toHaveBeenCalledWith(
      expect.objectContaining({
        academicYear: "2026",
        semester: "U211600010",
        name: "강의명",
      }),
    );
  });
});
