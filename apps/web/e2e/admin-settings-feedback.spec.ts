import { expect, test } from "@playwright/test";

test("관리자 설정에서 자가 알림을 테스트하고 종료된 일정은 전체 관리에서 확인한다", async ({ page, baseURL }) => {
  const schedules = [
    { id: 1, scheduleType: "지난 일정", startDate: "2026-11-12", endDate: "2026-11-13", endTime: "08:00", dDay: "D-Day", ddayEnabled: false, deletedAt: null },
    { id: 2, scheduleType: "진행 중인 일정", startDate: "2026-11-13", endDate: "2026-11-13", dDay: "D-Day", ddayEnabled: false, deletedAt: null },
  ];
  await page.clock.setFixedTime(new Date("2026-11-13T00:00:00Z"));
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: {
        id: 1, name: "관리자", email: "admin@example.invalid", role: "ADMIN", onboardingCompleted: true,
      } });
    }
    if (pathname === "/api/v1/admin/dashboard") {
      return route.fulfill({ json: { crawlTarget: { year: "2026", semester: "U211600020" } } });
    }
    if (pathname === "/api/v1/admin/schedules") return route.fulfill({ json: schedules });
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === "/api/v1/admin/notifications/test") return route.fulfill({ status: 200 });
    return route.fulfill({ status: 404, json: {} });
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/admin/settings");
  const crawlTarget = page.getByRole("region", { name: "크롤링 대상 제어" });
  await expect(crawlTarget.getByRole("button", { name: "이 설정으로 실행" })).toBeVisible();
  await expect(crawlTarget.getByRole("button", { name: "지정 값으로 실행" })).toBeVisible();
  await expect(page.getByText("학기 선택:", { exact: false })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "강의 데이터 재수집" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "대시보드 새로고침" })).toHaveCount(0);
  await expect(page.getByText("진행 중인 일정", { exact: true })).toBeVisible();
  await expect(page.getByText("지난 일정", { exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);

  const [testRequest] = await Promise.all([
    page.waitForRequest((request) => new URL(request.url()).pathname === "/api/v1/admin/notifications/test"),
    crawlTarget.getByRole("button", { name: "자가 알림 테스트" }).click(),
  ]);
  expect(testRequest.method()).toBe("POST");

  await page.getByRole("link", { name: "전체 일정 관리" }).click();
  await expect(page.getByText("지난 일정", { exact: true })).toBeVisible();
});
