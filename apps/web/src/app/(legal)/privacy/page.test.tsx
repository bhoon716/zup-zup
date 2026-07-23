import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import PrivacyPage from "./page";

describe("PrivacyPage", () => {
  it("외부 분석 도구와 비식별·마스킹 정책을 안내한다", () => {
    render(<PrivacyPage />);

    expect(screen.getAllByText(/Microsoft Clarity/)).not.toHaveLength(0);
    expect(screen.getAllByText(/Google Analytics 4/)).not.toHaveLength(0);
    expect(screen.getByText(/텍스트를 마스킹/)).toBeInTheDocument();
    expect(screen.getByText(/광고 개인화/)).toBeInTheDocument();
  });
});
