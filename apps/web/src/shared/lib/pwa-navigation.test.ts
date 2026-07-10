import { describe, expect, it } from "vitest";
import { resolveAllowedPwaUrl } from "./pwa-navigation";

const ORIGIN = "https://zup-zup.com";

describe("resolveAllowedPwaUrl", () => {
  it.each([
    ["/", `${ORIGIN}/`],
    ["/notifications", `${ORIGIN}/notifications`],
    ["/courses/CLTR.0031-01?tab=reviews", `${ORIGIN}/courses/CLTR.0031-01?tab=reviews`],
    ["https://zup-zup.com/announcements/1", `${ORIGIN}/announcements/1`],
  ])("allows an approved same-origin destination: %s", (value, expected) => {
    expect(resolveAllowedPwaUrl(value, ORIGIN)).toBe(expected);
  });

  it.each([
    "https://evil.example/",
    "//evil.example/",
    "javascript:alert(1)",
    "data:text/html,evil",
    "/admin",
    "/courses/",
    "not a URL",
    undefined,
  ])("falls back for an unsafe destination: %s", (value) => {
    expect(resolveAllowedPwaUrl(value, ORIGIN)).toBe(`${ORIGIN}/`);
  });
});
