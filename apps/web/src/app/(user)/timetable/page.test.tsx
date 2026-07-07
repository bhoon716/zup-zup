import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import TimetableRoutePage from "./page";

const mockUseTimetablePage = vi.fn();

vi.mock("./useTimetablePage", () => ({
  useTimetablePage: () => mockUseTimetablePage(),
}));

vi.mock("./timetable-page", () => ({
  TimetablePage: ({ model }: { model: { title: string } }) => (
    <div data-testid="timetable-page">{model.title}</div>
  ),
}));

describe("TimetableRoutePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseTimetablePage.mockReturnValue({ title: "timetable-model" });
  });

  it("passes the hook model into the page component", () => {
    render(<TimetableRoutePage />);

    expect(screen.getByTestId("timetable-page")).toHaveTextContent("timetable-model");
    expect(mockUseTimetablePage).toHaveBeenCalledTimes(1);
  });
});
