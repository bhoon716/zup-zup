import { describe, expect, it } from "vitest";

import { buildFeedbackMetadata } from "./feedback-metadata";

describe("buildFeedbackMetadata", () => {
  it("sends only the allowed, normalized browser fields", () => {
    expect(JSON.parse(buildFeedbackMetadata(" MacIntel ", " ko-KR "))).toEqual({
      os: "MacIntel",
      language: "ko-KR",
    });
  });

  it("omits unavailable or oversized values rather than sending an invalid payload", () => {
    expect(JSON.parse(buildFeedbackMetadata(" ", "x".repeat(36)))).toEqual({});
  });
});
