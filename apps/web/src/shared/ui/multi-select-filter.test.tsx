import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { MultiSelectFilter } from "./multi-select-filter";

// Radix UI DropdownMenu needs PointerEvent to work in JSDOM
// or we can mock the component for simpler testing.
// Here we mock the Radix UI DropdownMenu for easier testing of the MultiSelectFilter logic.
vi.mock("@/shared/ui/dropdown-menu", () => ({
  DropdownMenu: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  DropdownMenuContent: ({ children }: { children: React.ReactNode }) => <div data-testid="dropdown-content">{children}</div>,
  DropdownMenuItem: ({
    children,
    onClick,
    "aria-checked": ariaChecked,
    ...props
  }: {
    children: React.ReactNode;
    onClick?: () => void;
    "aria-checked"?: boolean;
    [key: string]: unknown;
  }) => (
    <div 
      data-testid="checkbox-item"
      role="menuitemcheckbox"
      aria-checked={ariaChecked}
      onClick={onClick}
      {...props}
    >
      {children}
    </div>
  ),
}));

function FilterHarness({ initial }: { initial: string[] }) {
  const [selected, setSelected] = useState<string[]>(initial);
  const options = [
    { label: "옵션1", value: "OPT1" },
    { label: "옵션2", value: "OPT2" },
    { label: "옵션3", value: "OPT3" },
  ];

  return (
    <div>
      <MultiSelectFilter
        options={options}
        selected={selected}
        onChange={setSelected}
        placeholder="테스트 선택"
      />
      <p data-testid="selected-count">{selected.length}</p>
    </div>
  );
}

describe("MultiSelectFilter", () => {
  it("아무것도 선택되지 않았을 때 플레이스홀더를 표시한다", () => {
    render(<FilterHarness initial={[]} />);
    expect(screen.getByText("테스트 선택")).toBeInTheDocument();
  });

  it("옵션을 선택하면 선택된 개수가 업데이트된다", () => {
    render(<FilterHarness initial={[]} />);
    
    const items = screen.getAllByTestId("checkbox-item");
    fireEvent.click(items[0]); // OPT1 선택

    expect(screen.getByTestId("selected-count")).toHaveTextContent("1");
    expect(items[0]).toHaveAttribute("aria-checked", "true");
    // Trigger should now show "옵션1"
    const trigger = screen.getByRole("button", { name: "옵션1" });
    expect(trigger).toHaveTextContent("옵션1");
  });

  it("여러 옵션을 선택하면 '외 n개' 형식이 표시된다", () => {
    render(<FilterHarness initial={["OPT1", "OPT2"]} />);
    
    expect(screen.getByText("옵션1 외 1개")).toBeInTheDocument();
    expect(screen.getByTestId("selected-count")).toHaveTextContent("2");
  });

  it("배지의 X 버튼을 누르면 선택이 해제된다", () => {
    render(<FilterHarness initial={["OPT1"]} />);
    
    const xButton = screen.getByRole("button", { name: "옵션1 선택 해제" });
    fireEvent.click(xButton);

    expect(screen.getByTestId("selected-count")).toHaveTextContent("0");
  });
});
