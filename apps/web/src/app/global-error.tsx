"use client";

import { useEffect } from "react";
import { reportClientError } from "@/shared/telemetry/client-error";

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  useEffect(() => {
    reportClientError(error, { source: "global-error-boundary" });
  }, [error]);

  return (
    <html lang="ko">
      <body>
        <main style={{ alignItems: "center", display: "flex", flexDirection: "column", gap: "1rem", minHeight: "50vh", justifyContent: "center", padding: "1.5rem", textAlign: "center" }}>
          <h2>문제가 발생했습니다.</h2>
          <p>잠시 후 다시 시도해 주세요.</p>
          <button type="button" onClick={() => reset()}>
            다시 시도
          </button>
        </main>
      </body>
    </html>
  );
}
