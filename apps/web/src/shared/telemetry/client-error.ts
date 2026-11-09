"use client";

import { track } from "@vercel/analytics";

const MAX_LABEL_LENGTH = 64;
const TRANSPORT_CODES = new Set([
  "ERR_NETWORK", "ECONNABORTED", "ETIMEDOUT", "ERR_CANCELED", "ERR_BAD_REQUEST", "ERR_BAD_RESPONSE",
]);
const reportedErrors = new WeakSet<object>();

export type ClientErrorSource =
  | "route-error-boundary"
  | "global-error-boundary"
  | "react-query"
  | "auth"
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

function readObjectProperty(value: unknown, property: string): unknown {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return undefined;
  }
  return (value as Record<string, unknown>)[property];
}

function serverErrorMetadata(error: unknown): ClientErrorEvent {
  const body = readObjectProperty(readObjectProperty(error, "response"), "data");
  const metadata: ClientErrorEvent = {};
  const correlationId = readObjectProperty(body, "correlationId");
  const code = readObjectProperty(body, "code");
  // 원문을 치환하거나 잘라서 다른 식별자로 만들지 않는다.
  if (typeof correlationId === "string" && correlationId.length > 0
      && correlationId.length <= MAX_LABEL_LENGTH && /^[a-zA-Z0-9]/.test(correlationId)
      && !/[^a-zA-Z0-9._-]/.test(correlationId)) {
    metadata.correlation_id = correlationId;
  }
  if (typeof code === "string" && code.length > 0 && code.length <= 32
      && /^[A-Z]/.test(code) && !/[^A-Z0-9_]/.test(code)) {
    metadata.error_code = code;
  }
  return metadata;
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
    ...serverErrorMetadata(error),
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

  const transportCode = readErrorProperty(error, "code");
  if (typeof transportCode === "string" && TRANSPORT_CODES.has(transportCode)) {
    event.transport_code = transportCode;
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
  const message = `[ClientError] 화면 작업 실패. source=${event.source} type=${event.error_type}`;
  console.error(message, event);

  if (typeof window === "undefined") {
    return;
  }

  const analyticsWindow = window as unknown as AnalyticsGlobals;
  try {
    track("client_error", event);
    const exceptionEvent: ClientErrorEvent = {
      description: `${event.source}:${event.error_type}`,
      fatal: event.fatal,
      ...("status_code" in event ? { status_code: event.status_code } : {}),
      ...("transport_code" in event ? { transport_code: event.transport_code } : {}),
    };

    for (const field of ["correlation_id", "error_code"]) {
      if (field in event) {
        exceptionEvent[field] = event[field];
      }
    }

    if (analyticsWindow.gtag) {
      analyticsWindow.gtag("event", "exception", exceptionEvent);
    } else {
      analyticsWindow.dataLayer = analyticsWindow.dataLayer || [];
      analyticsWindow.dataLayer.push(["event", "exception", exceptionEvent]);
    }
    analyticsWindow.clarity?.("event", "client_error");
  } catch {
  }
}
