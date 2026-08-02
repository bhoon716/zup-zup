import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { buildClientErrorEvent, reportClientError } from "./client-error";

const { mockTrack } = vi.hoisted(() => ({
  mockTrack: vi.fn(),
}));

vi.mock("@vercel/analytics", () => ({
  track: mockTrack,
}));

describe("client error telemetry", () => {
  beforeEach(() => {
    mockTrack.mockReset();
    delete (window as Window & { gtag?: unknown }).gtag;
    delete (window as Window & { clarity?: unknown }).clarity;
    (window as Window & { dataLayer?: unknown[][] }).dataLayer = [];
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("원문 메시지 대신 안전한 오류 메타데이터를 만든다", () => {
    const error = Object.assign(new Error("token=secret"), {
      digest: "digest:123",
      response: { status: 503 },
    });

    expect(buildClientErrorEvent(error, { source: "react-query", operation: "query" })).toEqual({
      source: "react-query",
      error_type: "Error",
      fatal: false,
      operation: "query",
      error_digest: "digest_123",
      status_code: 503,
    });
  });

  it("Vercel Analytics, GA4, Clarity에 동일한 클라이언트 오류 이벤트를 전달한다", () => {
    const gtag = vi.fn();
    const clarity = vi.fn();
    window.gtag = gtag;
    (window as Window & { clarity?: (...args: unknown[]) => void }).clarity = clarity;

    reportClientError(new Error("private detail"), {
      source: "route-error-boundary",
      operation: "render",
    });

    expect(mockTrack).toHaveBeenCalledWith("client_error", {
      source: "route-error-boundary",
      error_type: "Error",
      fatal: false,
      operation: "render",
    });
    expect(gtag).toHaveBeenCalledWith("event", "exception", {
      description: "route-error-boundary:Error",
      fatal: false,
    });
    expect(clarity).toHaveBeenCalledWith("event", "client_error");
  });

  it("GA4가 아직 초기화되지 않았으면 dataLayer에 이벤트를 큐잉한다", () => {
    reportClientError(new Error("not sent to console only"), { source: "global-error-boundary" });

    expect((window as Window & { dataLayer?: unknown[][] }).dataLayer).toEqual([
      ["event", "exception", { description: "global-error-boundary:Error", fatal: true }],
    ]);
  });

  it("같은 Error 객체의 중복 보고를 억제한다", () => {
    const error = new Error("same failure");

    reportClientError(error, { source: "timetable", operation: "export-image" });
    reportClientError(error, { source: "timetable", operation: "export-image" });

    expect(mockTrack).toHaveBeenCalledTimes(1);
  });
});
