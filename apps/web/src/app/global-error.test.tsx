import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import GlobalError from "./global-error";

const { mockReportClientError } = vi.hoisted(() => ({
  mockReportClientError: vi.fn(),
}));

vi.mock("@/shared/telemetry/client-error", () => ({
  reportClientError: mockReportClientError,
}));

describe("global error boundary", () => {
  it("html fallback을 렌더링하고 reset을 호출한다", () => {
    const error = Object.assign(new Error("root failure"), { digest: "root-digest" });
    const reset = vi.fn();

    render(<GlobalError error={error} reset={reset} />);

    expect(mockReportClientError).toHaveBeenCalledWith(error, { source: "global-error-boundary" });
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(reset).toHaveBeenCalledTimes(1);
  });
});
