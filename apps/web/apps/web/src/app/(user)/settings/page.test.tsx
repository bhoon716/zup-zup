import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import SettingsRoutePage from "./page";

const mockUseSettingsPage = vi.fn();

vi.mock("./useSettingsPage", () => ({
  useSettingsPage: () => mockUseSettingsPage(),
}));

vi.mock("./settings-page", () => ({
  SettingsPage: ({ model }: { model: { title: string } }) => (
    <div data-testid="settings-page">{model.title}</div>
  ),
}));

describe("SettingsRoutePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseSettingsPage.mockReturnValue({ title: "settings-model" });
  });

  it("passes the hook model into the page component", () => {
    render(<SettingsRoutePage />);

    expect(screen.getByTestId("settings-page")).toHaveTextContent("settings-model");
    expect(mockUseSettingsPage).toHaveBeenCalledTimes(1);
  });
});
