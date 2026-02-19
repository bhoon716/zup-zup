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
    // 하위 컴포넌트는 자체 상태를 가짐
    const { getByRole, getByTestId } = render(<SelectorHarness initial={[]} />);

    const startCell = getByRole("button", { name: "월 1교시" });
    const endCell = getByRole("button", { name: "화 2교시" });

    fireEvent.mouseDown(startCell);
    fireEvent.mouseEnter(endCell);
    
    // fireEvent를 사용하여 window에 mouseup 이벤트를 트리거해야 함
    fireEvent.mouseUp(window);

    expect(getByTestId("selected-count")).toHaveTextContent("4");

    const selected = JSON.parse(
      getByTestId("selected-json").textContent ?? "[]",
    ) as ScheduleCondition[];
    
    // 올바른 셀이 선택되었는지 확인
    // 월 1, 월 2, 화 1, 화 2
    // We just check the count and structure mainly
    expect(selected).toHaveLength(4);
    expect(selected).toEqual(expect.arrayContaining([
      { dayOfWeek: "월", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "월", startTime: "10:00:00", endTime: "11:00:00" },
      { dayOfWeek: "화", startTime: "09:00:00", endTime: "10:00:00" },
      { dayOfWeek: "화", startTime: "10:00:00", endTime: "11:00:00" },
    ]));
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

  it("전체 선택 버튼을 누르면 모든 칸이 선택된다", () => {
    render(<SelectorHarness initial={[]} />);
    const selectAll = screen.getByRole("button", { name: "전체 선택" });
    fireEvent.click(selectAll);
    // 6 days * 13 slots = 78
    expect(screen.getByTestId("selected-count")).toHaveTextContent("78");
  });

  it("전체 해제 버튼을 누르면 모든 선택이 해제된다", () => {
    const initial: ScheduleCondition[] = [
      { dayOfWeek: "월", startTime: "09:00:00", endTime: "10:00:00" },
    ];
    render(<SelectorHarness initial={initial} />);
    const deselectAll = screen.getByRole("button", { name: "전체 해제" });
    fireEvent.click(deselectAll);
    expect(screen.getByTestId("selected-count")).toHaveTextContent("0");
  });
});
