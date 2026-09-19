import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Textarea } from "./textarea";

describe("Textarea", () => {
  it("모바일 자동 확대를 막기 위해 모바일 16px, 데스크톱 14px를 사용한다", () => {
    render(<Textarea aria-label="내용" />);

    expect(screen.getByRole("textbox", { name: "내용" })).toHaveClass("text-base", "md:text-sm");
  });
});
