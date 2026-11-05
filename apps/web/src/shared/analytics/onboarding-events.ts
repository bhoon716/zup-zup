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
  }
  return phases.get(accountId);
}

function writePhase(accountId: number, phase: Phase) {
  phases.set(accountId, phase);
  try {
    window.sessionStorage.setItem(`onboarding-analytics:${accountId}`, phase);
  } catch {
  }
}

function send(event: OnboardingEvent) {
  if (typeof window === "undefined") return;
  if (!destination) {
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
    if (window.gtag) {
      window.gtag("event", event.name, payload);
    } else {
      window.dataLayer = window.dataLayer || [];
      window.dataLayer.push(["event", event.name, payload]);
    }
  } catch {
  }
}

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
