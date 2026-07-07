import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { CourseBasicFilters } from "./course-basic-filters";
import type { CourseSearchCondition } from "@/shared/types/api";

vi.mock("@/shared/ui/select", () => ({
  Select: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SelectContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SelectItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SelectTrigger: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  SelectValue: () => <span />,
}));

function FiltersHarness() {
  const [condition, setCondition] = useState<CourseSearchCondition>({});

  return (
    <div>
      <CourseBasicFilters condition={condition} setCondition={setCondition} />
      <p data-testid="condition-json">{JSON.stringify(condition)}</p>
    </div>
  );
}

describe("CourseBasicFilters", () => {
  it("교수명과 학과명 입력란에 다중 입력 안내 문구를 보여준다", () => {
    render(<FiltersHarness />);

    expect(
      screen.getByText("쉼표(,)로 여러 명을 입력하면 OR 조건으로 검색됩니다."),
    ).toBeInTheDocument();
    expect(
      screen.getByText("쉼표(,)로 여러 학과를 입력하면 OR 조건으로 검색됩니다."),
    ).toBeInTheDocument();
  });

  it("교수명과 학과명 입력값을 상태에 반영한다", () => {
    render(<FiltersHarness />);

    fireEvent.change(screen.getByPlaceholderText("예: 홍길동, 김철수"), {
      target: { value: "김교수, 이교수" },
    });

    fireEvent.change(screen.getByPlaceholderText("예: 소프트웨어, 컴퓨터공학부"), {
      target: { value: "컴퓨터공학부, 전자공학부" },
    });

    const condition = JSON.parse(screen.getByTestId("condition-json").textContent || "{}");
    expect(condition.professor).toBe("김교수, 이교수");
    expect(condition.department).toBe("컴퓨터공학부, 전자공학부");
  });
});
