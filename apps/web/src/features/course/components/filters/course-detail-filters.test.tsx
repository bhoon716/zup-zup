import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { CourseDetailFilters } from "./course-detail-filters";
import type { CourseSearchCondition } from "@/shared/types/api";

// MultiSelectFilter 모킹
vi.mock("@/shared/ui/multi-select-filter", () => ({
  MultiSelectFilter: ({ options, selected, onChange, placeholder }: any) => (
    <div data-testid={`multi-select-${placeholder}`}>
      <button 
        data-testid={`btn-${placeholder}`}
        onClick={() => onChange([options[0].value])}
      >
        Select First
      </button>
    </div>
  ),
}));

function FiltersHarness() {
  const [condition, setCondition] = useState<CourseSearchCondition>({});

  return (
    <div>
      <CourseDetailFilters condition={condition} setCondition={setCondition} />
      <p data-testid="condition-json">{JSON.stringify(condition)}</p>
    </div>
  );
}

describe("CourseDetailFilters", () => {
  it("이수 구분을 선택하면 condition의 classifications가 업데이트된다", () => {
    render(<FiltersHarness />);
    
    const btn = screen.getByTestId("btn-이수 구분 선택");
    fireEvent.click(btn);

    const condition = JSON.parse(screen.getByTestId("condition-json").textContent || "{}");
    expect(condition.classifications).toBeDefined();
    expect(condition.classifications.length).toBeGreaterThan(0);
  });

  it("여러 필터를 동시에 업데이트할 수 있다", () => {
    render(<FiltersHarness />);
    
    const langBtn = screen.getByTestId("btn-강의 언어 선택");
    fireEvent.click(langBtn);

    const creditBtn = screen.getByTestId("btn-학점 선택");
    fireEvent.click(creditBtn);

    const condition = JSON.parse(screen.getByTestId("condition-json").textContent || "{}");
    expect(condition.lectureLanguages).toBeDefined();
    expect(condition.credits).toBeDefined();
  });
});
