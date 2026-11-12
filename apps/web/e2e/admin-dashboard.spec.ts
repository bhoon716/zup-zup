import { expect, test } from "@playwright/test";

const trafficPath = "/api/v1/admin/notifications/traffic";

test("관리자는 알림 발송량을 종류별로 확인하고 24시간과 7일을 전환할 수 있다", async ({ page, baseURL }) => {
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: {
        id: 1, name: "관리자", email: "admin@example.invalid", role: "ADMIN", onboardingCompleted: true,
      } });
    }
    if (pathname === "/api/v1/admin/dashboard") {
      return route.fulfill({ json: {
        overview: {
          totalUsers: 300,
          totalActiveSubscriptions: 42,
          crawlingStatus: "RUNNING",
          lastCrawledAt: "2026-11-13T07:00:00",
          jbnuLatencyMs: 123,
          serverTime: "2026-11-13T07:05:00",
        },
        crawlTarget: { year: "2026", semester: "U211600020" },
      } });
    }
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === "/api/v1/admin/notifications/test") return route.fulfill({ status: 200 });
    if (pathname === trafficPath) {
      const range = searchParams.get("range");
      if (range === "LAST_24_HOURS") {
        return route.fulfill({ json: [
          { label: "06:00", bucketStart: "2026-11-13T06:00:00+09:00", seatOpened: 1, feedbackReply: 2, announcement: 0 },
          { label: "07:00", bucketStart: "2026-11-13T07:00:00+09:00", seatOpened: 4, feedbackReply: 0, announcement: 2 },
        ] });
      }
      if (range === "LAST_7_DAYS") {
        return route.fulfill({ json: [
          { label: "11/12", bucketStart: "2026-11-12T00:00:00+09:00", seatOpened: 8, feedbackReply: 3, announcement: 1 },
          { label: "11/13", bucketStart: "2026-11-13T00:00:00+09:00", seatOpened: 5, feedbackReply: 2, announcement: 2 },
        ] });
      }
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.setViewportSize({ width: 1368, height: 1254 });
  await page.goto("/admin");
  const [testRequest] = await Promise.all([
    page.waitForRequest((request) => new URL(request.url()).pathname === "/api/v1/admin/notifications/test"),
    page.getByRole("button", { name: "자가 알림 테스트" }).click(),
  ]);
  expect(testRequest.method()).toBe("POST");
  await expect(page.getByText("테스트 알림이 성공적으로 전송되었습니다.")).toBeVisible();

  const activeSubscriptions = page.locator("dd").filter({ hasText: "42건" });
  await expect(activeSubscriptions.getByText("2026학년도 2학기", { exact: true })).toBeVisible();
  await expect(page.getByRole("heading", { name: "알림 트래픽 분석" })).toBeVisible();
  await expect(page.getByText("06:00", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("빈자리", { exact: true })).toBeVisible();
  await expect(page.getByText("문의 답변", { exact: true })).toBeVisible();
  await expect(page.getByText("중요 공지", { exact: true })).toBeVisible();
  const firstBucket = page.getByRole("button", { name: "06:00, 빈자리 1건, 문의 답변 2건, 중요 공지 0건" });
  const tooltip = page.getByRole("tooltip");
  await expect(tooltip).toHaveCount(0);
  await firstBucket.hover();
  await expect(tooltip).toContainText("문의 답변");
  await expect(tooltip).toContainText("2건");
  const heading = await page.getByRole("heading", { name: "알림 트래픽 분석" }).boundingBox();
  expect(heading).not.toBeNull();
  if (!heading) throw new Error("차트 제목의 위치를 찾을 수 없습니다.");
  await page.mouse.move(heading.x + 8, heading.y + 8, { steps: 10 });
  await expect(tooltip).toHaveCount(0);

  await firstBucket.focus();
  await expect(tooltip).toContainText("문의 답변");
  await page.keyboard.press("Escape");
  await expect(tooltip).toHaveCount(0);
  const colors = await firstBucket.locator("span").evaluateAll((segments) =>
    segments.map((segment) => getComputedStyle(segment).backgroundColor),
  );
  expect(new Set(colors).size).toBe(3);
  await expect(page.getByText("시스템 현황 관제", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "시스템 활동 로그" })).toHaveCount(0);
  await expect(page.getByText("로그인 됨", { exact: true })).toHaveCount(0);

  const week = page.getByRole("button", { name: "7일", exact: true });
  const day = page.getByRole("button", { name: "24시간", exact: true });
  await week.click();
  await expect(week).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText("11/12", { exact: true }).first()).toBeVisible();
  await expect(page.getByText("06:00", { exact: true })).toHaveCount(0);

  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
  await day.click();
  await expect(day).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText("06:00", { exact: true }).first()).toBeVisible();
});
