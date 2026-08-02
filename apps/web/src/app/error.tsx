"use client";

import { useEffect } from "react";
import { reportClientError } from "@/shared/telemetry/client-error";

interface ErrorBoundaryProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function ErrorBoundary({ error, reset }: ErrorBoundaryProps) {
  useEffect(() => {
    reportClientError(error, { source: "route-error-boundary" });
  }, [error]);

  return (
    <main className="flex min-h-[50vh] flex-col items-center justify-center gap-4 px-6 text-center">
      <h2 className="text-2xl font-bold text-gray-900 dark:text-white">문제가 발생했습니다.</h2>
      <p className="text-sm text-gray-500 dark:text-gray-400">잠시 후 다시 시도해 주세요.</p>
      <button
        type="button"
        onClick={() => reset()}
        className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white"
      >
        다시 시도
      </button>
    </main>
  );
}
