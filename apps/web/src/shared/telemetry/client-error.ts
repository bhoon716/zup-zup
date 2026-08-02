"use client";

import { track } from "@vercel/analytics";

const MAX_LABEL_LENGTH = 64;
const reportedErrors = new WeakSet<object>();

export type ClientErrorSource =
  | "route-error-boundary"
  | "global-error-boundary"
  | "react-query"
  | "service-worker"
  | "web-push"
  | "admin-schedule"
  | "custom-schedule"
  | "settings"
  | "timetable";

export interface ClientErrorContext {
  source: ClientErrorSource;
  operation?: string;
}

export type ClientErrorEvent = Record<string, string | number | boolean>;

interface AnalyticsGlobals {
  clarity?: (...args: unknown[]) => void;
  dataLayer?: unknown[][];
  gtag?: (...args: unknown[]) => void;
}

function sanitizeLabel(value: string) {
  return value.replace(/[^a-zA-Z0-9._-]/g, "_").slice(0, MAX_LABEL_LENGTH) || "unknown";
}

function readErrorProperty(error: unknown, property: string) {
  if (typeof error !== "object" || error === null) {
    return undefined;
  }

  const value = (error as Record<string, unknown>)[property];
  return typeof value === "string" || typeof value === "number" ? value : undefined;
}

function errorType(error: unknown) {
  if (error instanceof Error && error.name) {
    return sanitizeLabel(error.name);
  }

  const namedError = readErrorProperty(error, "name");
  return typeof namedError === "string" ? sanitizeLabel(namedError) : "UnknownError";
}

function statusCode(error: unknown) {
  const directStatus = readErrorProperty(error, "status");
  if (typeof directStatus === "number" && Number.isInteger(directStatus)) {
    return directStatus;
  }

  const response = typeof error === "object" && error !== null
    ? (error as Record<string, unknown>).response
    : undefined;
  const responseStatus = readErrorProperty(response, "status");
  return typeof responseStatus === "number" && Number.isInteger(responseStatus)
    ? responseStatus
    : undefined;
}

export function buildClientErrorEvent(error: unknown, context: ClientErrorContext): ClientErrorEvent {
  const event: ClientErrorEvent = {
    source: context.source,
    error_type: errorType(error),
    fatal: context.source === "global-error-boundary",
  };

  if (context.operation) {
    event.operation = sanitizeLabel(context.operation);
  }

  const digest = readErrorProperty(error, "digest");
  if (typeof digest === "string" && digest.trim()) {
    event.error_digest = sanitizeLabel(digest);
  }

  const status = statusCode(error);
  if (status !== undefined) {
    event.status_code = status;
  }

  return event;
}

export function reportClientError(error: unknown, context: ClientErrorContext) {
  if (typeof error === "object" && error !== null) {
    if (reportedErrors.has(error)) {
      return;
    }
    reportedErrors.add(error);
  }

  const event = buildClientErrorEvent(error, context);
  const message = `[ClientError] source=${event.source} type=${event.error_type}`;
  if (process.env.NODE_ENV === "production") {
    console.error(message, event);
  } else {
    console.error(message, event, error);
  }

  if (typeof window === "undefined") {
    return;
  }

  const analyticsWindow = window as unknown as AnalyticsGlobals;
  try {
    track("client_error", event);
    const exceptionEvent = {
      description: `${event.source}:${event.error_type}`,
      fatal: event.fatal,
      ...("status_code" in event ? { status_code: event.status_code } : {}),
    };

    if (analyticsWindow.gtag) {
      analyticsWindow.gtag("event", "exception", exceptionEvent);
    } else {
      analyticsWindow.dataLayer = analyticsWindow.dataLayer || [];
      analyticsWindow.dataLayer.push(["event", "exception", exceptionEvent]);
    }
    analyticsWindow.clarity?.("event", "client_error");
  } catch {
    // Client telemetry must never change the fallback or request error behavior.
  }
}
