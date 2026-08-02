import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import ErrorBoundary from "./error";

const { mockReportClientError } = vi.hoisted(() => ({
  mockReportClientError: vi.fn(),
}));

vi.mock("@/shared/telemetry/client-error", () => ({
  reportClientError: mockReportClientError,
}));

describe("route error boundary", () => {
  it("보고 후 다시 시도 버튼으로 reset을 호출한다", () => {
    const error = Object.assign(new Error("render failure"), { digest: "route-digest" });
    const reset = vi.fn();

    render(<ErrorBoundary error={error} reset={reset} />);

    expect(mockReportClientError).toHaveBeenCalledWith(error, { source: "route-error-boundary" });
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(reset).toHaveBeenCalledTimes(1);
  });
});
