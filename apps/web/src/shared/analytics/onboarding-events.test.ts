import { AxiosError, CanceledError } from "axios";
import { createElement, StrictMode, useEffect } from "react";
import { renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const load = () => import("./onboarding-events");
const payload = {
  send_to: "G-LOCALTEST",
  page_path: "/onboarding",
  page_location: "http://localhost:3000/onboarding",
};
const httpError = (status: number) => new AxiosError("private@example.invalid token=secret", undefined,
  undefined, undefined, { status, data: { message: "private exception" } } as never);

describe("온보딩 GA4 이벤트", () => {
  beforeEach(() => {
    vi.resetModules();
    window.sessionStorage.clear();
    window.history.replaceState({}, "", "/onboarding?token=secret&discord=private");
    delete window.gtag;
    Reflect.deleteProperty(window, "ga-disable-G-LOCALTEST");
    window.dataLayer = [];
  });
  afterEach(() => vi.restoreAllMocks());

  it("production GA 초기화 전 이벤트를 보류하고 기존 경로가 활성화될 때 한 번 전달한다", async () => {
    const events = await load();
    events.trackOnboardingStarted(1);
    events.trackOnboardingStarted(1);
    expect(window.dataLayer).toEqual([]);
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    expect(window.dataLayer).toEqual([["event", "onboarding_started", payload]]);
  });

  it("재렌더·뒤로가기·새 문서 복귀에서 같은 탭/계정의 시작과 완료를 중복하지 않는다", async () => {
    let events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingStarted(1);
    vi.resetModules();
    events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingStarted(1);
    events.trackOnboardingCompleted(1);
    events.trackOnboardingCompleted(1);
    events.trackOnboardingStarted(1);
    events.trackOnboardingFailed(1, httpError(503));
    expect(window.dataLayer).toEqual([
      ["event", "onboarding_started", payload],
      ["event", "onboarding_completed", payload],
    ]);
  });

  it("같은 탭의 다른 계정을 별도 처리하되 계정 식별값은 payload에 넣지 않는다", async () => {
    const events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingStarted(1);
    events.trackOnboardingCompleted(1);
    events.trackOnboardingStarted(2);
    expect(window.dataLayer).toEqual([
      ["event", "onboarding_started", payload],
      ["event", "onboarding_completed", payload],
      ["event", "onboarding_started", payload],
    ]);
  });

  it("React StrictMode의 효과 재실행에서도 시작은 한 번만 보낸다", async () => {
    const events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    const { rerender } = renderHook(() => {
      useEffect(() => { events.trackOnboardingStarted(1); });
    }, { wrapper: ({ children }) => createElement(StrictMode, null, children) });
    rerender();
    expect(window.dataLayer).toEqual([["event", "onboarding_started", payload]]);
  });

  it("수동 재시도의 각 실패와 이후 실제 성공을 기록하며 원문 정보를 버린다", async () => {
    const events = await load();
    const gtag = vi.fn();
    window.gtag = gtag;
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingStarted(1);
    const error = httpError(503);
    events.trackOnboardingFailed(1, error);
    events.trackOnboardingFailed(1, error);
    events.trackOnboardingCompleted(1);
    expect(gtag.mock.calls).toEqual([
      ["event", "onboarding_started", payload],
      ["event", "onboarding_failed", { ...payload, failure_reason: "server" }],
      ["event", "onboarding_failed", { ...payload, failure_reason: "server" }],
      ["event", "onboarding_completed", payload],
    ]);
    expect(JSON.stringify(gtag.mock.calls)).not.toMatch(/secret|private|account|email|discord|token/);
  });

  it.each([
    [new AxiosError("private", "ECONNABORTED"), "network"],
    [new AxiosError("private", "ERR_NETWORK"), "network"],
    [httpError(401), "auth"], [httpError(403), "auth"],
    [httpError(400), "rejected"], [httpError(429), "rejected"],
    [httpError(503), "server"], [new Error("private"), "unknown"],
  ])("저장 요청의 최종 오류는 허용된 사유만 보낸다: %s", async (error, reason) => {
    const events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingFailed(1, error);
    expect(window.dataLayer).toEqual([
      ["event", "onboarding_failed", { ...payload, failure_reason: reason }],
    ]);
  });

  it("취소·미인증 조작·페이지 이탈을 저장 실패로 보내지 않는다", async () => {
    const events = await load();
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingFailed(1, new CanceledError());
    events.trackOnboardingFailed(undefined, httpError(503));
    events.trackOnboardingCompleted(undefined);
    window.dispatchEvent(new Event("beforeunload"));
    window.dispatchEvent(new Event("pagehide"));
    expect(window.dataLayer).toEqual([]);
  });

  it("기존 GA 비활성 플래그를 존중하고 consent/config를 새로 전송하지 않는다", async () => {
    const events = await load();
    Reflect.set(window, "ga-disable-G-LOCALTEST", true);
    events.trackOnboardingStarted(1);
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    events.trackOnboardingCompleted(1);
    expect(window.dataLayer).toEqual([]);
  });

  it("저장소 차단과 분석 도구 예외가 온보딩 동작을 중단하지 않는다", async () => {
    const events = await load();
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => { throw new Error(); });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => { throw new Error(); });
    window.gtag = vi.fn(() => { throw new Error(); });
    events.initializeOnboardingAnalytics("G-LOCALTEST");
    expect(() => {
      events.trackOnboardingStarted(1);
      events.trackOnboardingStarted(1);
      events.trackOnboardingCompleted(1);
    }).not.toThrow();
    expect(window.gtag).toHaveBeenCalledTimes(2);
  });
});
