import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import { FilterSection } from "./filter-section";

describe("FilterSection", () => {
  it("버튼을 누르면 내용 표시를 토글한다", () => {
    function Harness() {
      const [open, setOpen] = useState(false);

      return (
        <FilterSection
          idBase="sample-filter"
          title="스마트 필터"
          icon={<span data-testid="icon">i</span>}
          open={open}
          onOpenChange={setOpen}
        >
          <div data-testid="content">내용</div>
        </FilterSection>
      );
    }

    render(<Harness />);

    const trigger = screen.getByRole("button");
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    expect(screen.getByTestId("content").parentElement).toHaveAttribute("hidden");

    fireEvent.click(trigger);

    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByTestId("content").parentElement).not.toHaveAttribute("hidden");
  });
});
