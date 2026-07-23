import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThirdPartyAnalytics } from "./third-party-analytics";

vi.mock("next/navigation", () => ({
  usePathname: () => "/feedbacks",
}));

vi.mock("next/script", () => ({
  default: ({ strategy, ...props }: React.ComponentProps<"script"> & { strategy?: string }) => {
    void strategy;
    return <script {...props} />;
  },
}));

describe("ThirdPartyAnalytics", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/feedbacks?token=secret&message=private");
    delete window.gtag;
    window.dataLayer = [];
  });

  it("production에서 설정된 Clarity와 GA4만 한 번 초기화한다", () => {
    const { rerender } = render(
      <ThirdPartyAnalytics
        environment="production"
        clarityProjectId="clarity123"
        gaMeasurementId="G-ABC123"
      />,
    );

    expect(screen.getAllByTestId("clarity-script")).toHaveLength(1);
    expect(screen.getAllByTestId("ga-loader")).toHaveLength(1);
    expect(screen.getAllByTestId("ga-bootstrap")).toHaveLength(1);
    expect(screen.getByTestId("ga-loader")).toHaveAttribute(
      "src",
      "https://www.googletagmanager.com/gtag/js?id=G-ABC123",
    );

    rerender(
      <ThirdPartyAnalytics
        environment="production"
        clarityProjectId="clarity123"
        gaMeasurementId="G-ABC123"
      />,
    );

    expect(screen.getAllByTestId("clarity-script")).toHaveLength(1);
    expect(screen.getAllByTestId("ga-loader")).toHaveLength(1);
  });

  it.each(["development", "test", "preview"])("%s 환경에서는 외부 분석 스크립트를 렌더링하지 않는다", (environment) => {
    render(
      <ThirdPartyAnalytics
        environment={environment}
        clarityProjectId="clarity123"
        gaMeasurementId="G-ABC123"
      />,
    );

    expect(screen.queryByTestId("clarity-script")).not.toBeInTheDocument();
    expect(screen.queryByTestId("ga-loader")).not.toBeInTheDocument();
  });

  it("production이어도 ID가 없거나 형식이 잘못되면 해당 공급자를 초기화하지 않는다", () => {
    render(
      <ThirdPartyAnalytics
        environment="production"
        clarityProjectId=""
        gaMeasurementId="not-a-ga-id"
      />,
    );

    expect(screen.queryByTestId("clarity-script")).not.toBeInTheDocument();
    expect(screen.queryByTestId("ga-loader")).not.toBeInTheDocument();
  });

  it("GA4 page_view에는 query나 사용자 식별값 없이 경로만 전송한다", async () => {
    render(
      <ThirdPartyAnalytics
        environment="production"
        clarityProjectId=""
        gaMeasurementId="G-ABC123"
      />,
    );

    await waitFor(() => expect(window.dataLayer.length).toBeGreaterThan(0));
    const pageView = window.dataLayer
      .map((entry) => Array.from(entry))
      .find((entry) => entry[0] === "event" && entry[1] === "page_view");

    expect(pageView?.[2]).toEqual({
      page_location: "http://localhost:3000/feedbacks",
      page_path: "/feedbacks",
      send_to: "G-ABC123",
    });
    expect(JSON.stringify(pageView)).not.toContain("token");
    expect(JSON.stringify(pageView)).not.toContain("private");
  });

  it("GA4 bootstrap은 Google Signals와 광고 개인화를 비활성화한다", () => {
    render(
      <ThirdPartyAnalytics
        environment="production"
        clarityProjectId=""
        gaMeasurementId="G-ABC123"
      />,
    );

    expect(screen.getByTestId("ga-bootstrap").innerHTML)
      .toContain('"allow_google_signals":false')
      .toContain('"allow_ad_personalization_signals":false')
      .toContain('"send_page_view":false');
  });
});
