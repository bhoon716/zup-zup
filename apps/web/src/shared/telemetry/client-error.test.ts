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

  it.each(["ERR_NETWORK", "ECONNABORTED", "ETIMEDOUT", "ERR_CANCELED"])(
    "알려진 전송 코드 %s만 진단 필드로 보존한다",
    (code) => {
      const error = Object.assign(new Error("token=private-token"), { code });
      expect(buildClientErrorEvent(error, { source: "react-query" }).transport_code).toBe(code);
    },
  );

  it("임의 오류 코드와 원문 요청 정보를 전송하지 않는다", () => {
    const event = buildClientErrorEvent({
      name: "AxiosError", code: "private-token", message: "cookie=private-cookie",
      config: { url: "https://private.example?code=private-oauth", headers: { Authorization: "private-header" } },
    }, { source: "react-query" });
    expect(event).not.toHaveProperty("transport_code");
    expect(JSON.stringify(event)).not.toMatch(/private-token|private-cookie|private-oauth|private-header/);
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
  it("개발 모드에서도 오류 객체·요청 헤더·원문 메시지를 콘솔에 전달하지 않는다", () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const error = Object.assign(new Error("cookie=private-cookie token=private-token"), {
      config: { headers: { "X-XSRF-TOKEN": "private-csrf" } },
      response: { status: 403, data: { email: "private@example.com" } },
    });

    reportClientError(error, { source: "web-push", operation: "unsubscribe" });

    expect(consoleError).toHaveBeenCalledExactlyOnceWith(
      "[ClientError] 화면 작업 실패. source=web-push type=Error",
      { source: "web-push", error_type: "Error", fatal: false, operation: "unsubscribe", status_code: 403 },
    );
    expect(JSON.stringify(consoleError.mock.calls)).not.toMatch(/private-cookie|private-token|private-csrf|private@example/);
  });
  it("서버 오류 추적 ID와 오류 코드를 안전한 진단 필드로 연결한다", () => {
    const event = buildClientErrorEvent({
      name: "AxiosError", code: "ERR_BAD_RESPONSE",
      response: { status: 503, data: { correlationId: "b6fd02e1-8eb3-4b27-a001-04f61fce319f", code: "G001", message: "private", path: "/private" } },
    }, { source: "react-query" });
    expect(event).toMatchObject({ correlation_id: "b6fd02e1-8eb3-4b27-a001-04f61fce319f", error_code: "G001", transport_code: "ERR_BAD_RESPONSE" });
    expect(JSON.stringify(event)).not.toContain("private");
  });

  it("서버가 허용하는 점이 포함된 추적 ID도 그대로 연결한다", () => {
    const event = buildClientErrorEvent({ response: { data: { correlationId: "request.123", code: "G001" } } }, { source: "react-query" });
    expect(event.correlation_id).toBe("request.123");
  });

  it.each(["_trace", ".trace", "-trace"])("서버가 거부하는 시작 문자 %s는 전달하지 않는다", (correlationId) => {
    const event = buildClientErrorEvent({ response: { data: { correlationId } } }, { source: "react-query" });
    expect(event).not.toHaveProperty("correlation_id");
  });

  it.each(["", "https://private.example", "private@example.com", "token=secret", "abc\n", "한글", "a".repeat(65), 123, {}, null])(
    "유효하지 않은 서버 진단 값 %j는 변환하거나 잘라서 보내지 않는다",
    (value) => {
      const event = buildClientErrorEvent({ response: { data: { correlationId: value, code: value } } }, { source: "react-query" });
      expect(event).not.toHaveProperty("correlation_id");
      expect(event).not.toHaveProperty("error_code");
    },
  );

  it.each([null, "private response", [], 123])("잘못된 오류 응답 본문 %j를 무시한다", (data) => {
    const event = buildClientErrorEvent({ response: { data } }, { source: "react-query" });
    expect(event).not.toHaveProperty("correlation_id");
    expect(event).not.toHaveProperty("error_code");
  });

  it("추적 필드를 Vercel과 GA 큐에 전달하고 Clarity 이벤트 형식은 유지한다", () => {
    const clarity = vi.fn();
    (window as Window & { clarity?: (...args: unknown[]) => void }).clarity = clarity;
    reportClientError({ response: { data: { correlationId: "trace_123", code: "G001" } } }, { source: "react-query" });
    expect(mockTrack).toHaveBeenCalledWith("client_error", expect.objectContaining({ correlation_id: "trace_123", error_code: "G001" }));
    expect((window as Window & { dataLayer?: unknown[][] }).dataLayer).toEqual([
      ["event", "exception", { description: "react-query:UnknownError", fatal: false, correlation_id: "trace_123", error_code: "G001" }],
    ]);
    expect(clarity).toHaveBeenCalledExactlyOnceWith("event", "client_error");
  });

  it("서버 진단 값의 길이 경계와 직접 GA 전송을 검증한다", () => {
    const gtag = vi.fn();
    window.gtag = gtag;
    const correlationId = "a".repeat(64);
    const code = "G".repeat(32);
    reportClientError({ response: { data: { correlationId, code } } }, { source: "react-query" });
    expect(gtag).toHaveBeenCalledWith("event", "exception", expect.objectContaining({ correlation_id: correlationId, error_code: code }));
    const event = buildClientErrorEvent({ response: { data: { correlationId: "a".repeat(65), code: "G".repeat(33) } } }, { source: "react-query" });
    expect(event).not.toHaveProperty("correlation_id");
    expect(event).not.toHaveProperty("error_code");
  });

});
