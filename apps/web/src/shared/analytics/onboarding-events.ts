import { isAxiosError, isCancel } from "axios";

type OnboardingEvent =
  | { name: "onboarding_started" | "onboarding_completed" }
  | { name: "onboarding_failed"; reason: "network" | "auth" | "rejected" | "server" | "unknown" };
type Phase = "started" | "completed";

const phases = new Map<number, Phase>();
const pending: OnboardingEvent[] = [];
let destination: string | undefined;

function readPhase(accountId: number): Phase | undefined {
  try {
    const value = window.sessionStorage.getItem(`onboarding-analytics:${accountId}`);
    if (value === "started" || value === "completed") return value;
  } catch {
    // 저장소가 차단돼도 현재 문서에서는 중복을 막는다.
  }
  return phases.get(accountId);
}

function writePhase(accountId: number, phase: Phase) {
  phases.set(accountId, phase);
  try {
    // 계정 구분은 탭 내부 중복 방지에만 쓰며 이벤트에 포함하지 않는다.
    window.sessionStorage.setItem(`onboarding-analytics:${accountId}`, phase);
  } catch {
    // 계측 때문에 온보딩을 중단하지 않는다.
  }
}

function send(event: OnboardingEvent) {
  if (typeof window === "undefined") return;
  if (!destination) {
    // production의 기존 GA 초기화 효과보다 먼저 실행된 화면 효과만 대기한다.
    // GA가 꺼진 환경에서는 dataLayer나 외부 전송을 만들지 않는다.
    if (pending.length < 20) pending.push(event);
    return;
  }
  if (Reflect.get(window, `ga-disable-${destination}`) === true) return;

  const payload = {
    send_to: destination,
    page_path: "/onboarding",
    page_location: `${window.location.origin}/onboarding`,
    ...(event.name === "onboarding_failed" ? { failure_reason: event.reason } : {}),
  };
  try {
    // 기존 gtag와 consent 처리를 그대로 사용하며 별도 config/consent를 만들지 않는다.
    if (window.gtag) {
      window.gtag("event", event.name, payload);
    } else {
      window.dataLayer = window.dataLayer || [];
      window.dataLayer.push(["event", event.name, payload]);
    }
  } catch {
    // 분석 도구 오류가 저장 결과를 바꾸지 않도록 한다.
  }
}

/** 유효한 production GA 설정을 가진 기존 초기화 경로에서만 호출한다. */
export function initializeOnboardingAnalytics(measurementId: string) {
  destination = measurementId;
  pending.splice(0).forEach(send);
}

export function trackOnboardingStarted(accountId: number) {
  if (readPhase(accountId)) return;
  writePhase(accountId, "started");
  send({ name: "onboarding_started" });
}

export function trackOnboardingCompleted(accountId: number | undefined) {
  if (accountId === undefined || readPhase(accountId) === "completed") return;
  writePhase(accountId, "completed");
  send({ name: "onboarding_completed" });
}

export function trackOnboardingFailed(accountId: number | undefined, error: unknown) {
  if (accountId === undefined || readPhase(accountId) === "completed" || isCancel(error)) return;
  let reason: Extract<OnboardingEvent, { name: "onboarding_failed" }>["reason"] = "unknown";
  if (isAxiosError(error)) {
    const status = error.response?.status;
    if (!error.response) reason = "network";
    else if (status === 401 || status === 403) reason = "auth";
    else if (status !== undefined && status >= 500) reason = "server";
    else if (status !== undefined && status >= 400) reason = "rejected";
  }
  send({ name: "onboarding_failed", reason });
}
