import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
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
    // JSDOM does not implement elementFromPoint, so we mock it
    const originalElementFromPoint = document.elementFromPoint;
    document.elementFromPoint = vi.fn();

    const { getByRole, getByTestId } = render(<SelectorHarness initial={[]} />);

    const startCell = getByRole("button", { name: "월 1교시" });
    const endCell = getByRole("button", { name: "화 2교시" });

    // Mock elementFromPoint to return endCell when moving
    vi.mocked(document.elementFromPoint).mockReturnValue(endCell);

    fireEvent.pointerDown(startCell, { pointerId: 1, pointerType: "mouse", button: 0 });
    fireEvent.pointerMove(endCell, { pointerId: 1, pointerType: "mouse", clientX: 100, clientY: 100 });
    fireEvent.pointerUp(window, { pointerId: 1, pointerType: "mouse" });

    // restore
    document.elementFromPoint = originalElementFromPoint;

    expect(getByTestId("selected-count")).toHaveTextContent("4");

    const selected = JSON.parse(
      getByTestId("selected-json").textContent ?? "[]",
    ) as ScheduleCondition[];

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

    // JSDOM does not implement elementFromPoint, so we mock it
    const originalElementFromPoint = document.elementFromPoint;
    document.elementFromPoint = vi.fn();

    const startCell = screen.getByRole("button", { name: "화 2교시" });
    const endCell = screen.getByRole("button", { name: "월 1교시" });

    // Mock elementFromPoint to return endCell when moving
    vi.mocked(document.elementFromPoint).mockReturnValue(endCell);

    fireEvent.pointerDown(startCell, { pointerId: 2, pointerType: "mouse", button: 0 });
    fireEvent.pointerMove(endCell, { pointerId: 2, pointerType: "mouse", clientX: 50, clientY: 50 });
    fireEvent.pointerUp(window, { pointerId: 2, pointerType: "mouse" });

    // restore
    document.elementFromPoint = originalElementFromPoint;

    expect(screen.getByTestId("selected-count")).toHaveTextContent("0");
  });

  it("전체 선택 버튼을 누르면 모든 칸이 선택된다", () => {
    render(<SelectorHarness initial={[]} />);
    const selectAll = screen.getByRole("button", { name: "전체 선택" });
    fireEvent.click(selectAll);
    // 6 days * 14 slots = 84
    expect(screen.getByTestId("selected-count")).toHaveTextContent("84");
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
