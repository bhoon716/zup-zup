import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import type { ScheduleCondition } from "@/shared/types/api";
import { TimeTableSelector } from "./time-table-selector";

function SelectorHarness({ initial }: { initial: ScheduleCondition[] }) {
  const [selected, setSelected] = useState<ScheduleCondition[]>(initial);

  return (
    <div>
      <TimeTableSelector selected={selected} onChange={setSelected} />
      <p data-testid="selected-count">{selected.length}</p>
      <p data-testid="selected-json">{JSON.stringify(selected)}</p>
    </div>
  );
}

describe("TimeTableSelector", () => {
  it("시간표 선택기는 13교시까지만 표시한다", () => {
    render(<SelectorHarness initial={[]} />);

    expect(screen.getByRole("button", { name: "월 13교시" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "월 14교시" })).not.toBeInTheDocument();
  });

  it("사각형 드래그로 여러 칸을 한 번에 선택한다", () => {
    render(<SelectorHarness initial={[]} />);

    const startCell = screen.getByRole("button", { name: "월 1교시" });
    const endCell = screen.getByRole("button", { name: "화 2교시" });

    fireEvent.mouseDown(startCell);
    fireEvent.mouseEnter(endCell);
    fireEvent.mouseUp(window);

    expect(screen.getByTestId("selected-count")).toHaveTextContent("4");

    const selected = JSON.parse(
      screen.getByTestId("selected-json").textContent ?? "[]",
    ) as ScheduleCondition[];
    expect(selected).toEqual([
      { dayOfWeek: "월", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "월", startTime: "10:00:00", endTime: "11:00:00" },
      { dayOfWeek: "화", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "화", startTime: "10:00:00", endTime: "11:00:00" },
    ]);
  });

  it("선택된 칸에서 시작한 사각형 드래그는 영역 전체를 해제한다", () => {
    const initial: ScheduleCondition[] = [
      { dayOfWeek: "월", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "월", startTime: "10:00:00", endTime: "11:00:00" },
      { dayOfWeek: "화", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "화", startTime: "10:00:00", endTime: "11:00:00" },
    ];

    render(<SelectorHarness initial={initial} />);

    const startCell = screen.getByRole("button", { name: "화 2교시" });
    const endCell = screen.getByRole("button", { name: "월 1교시" });

    fireEvent.mouseDown(startCell);
    fireEvent.mouseEnter(endCell);
    fireEvent.mouseUp(window);

    expect(screen.getByTestId("selected-count")).toHaveTextContent("0");
  });
});
