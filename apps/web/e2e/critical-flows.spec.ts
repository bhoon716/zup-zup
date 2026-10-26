import { expect, test } from "@playwright/test";

const response = (data: unknown) => (data);
test.beforeEach(async ({ page, baseURL }) => {
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
  await page.route("**/api/**", route => route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } }));
  await page.route("**/api/auth/csrf", route => route.fulfill({ json: { token: "test-csrf" } }));
});

test("session expiry opens login guidance without replaying the request", async ({ page }) => {
  let announcementCalls = 0;
  await page.route("**/api/v1/users/me", route => route.fulfill({ status: 401, json: { code: "A001" } }));
  await page.route("**/api/v1/announcements**", (route) => {
    announcementCalls += 1;
    return route.fulfill({ status: 401, json: { code: "A001", message: "expired" } });
  });
  await page.goto("/announcements");
  await expect(page.getByRole("dialog", { name: "로그인이 필요한 서비스입니다" })).toBeVisible();
  expect(announcementCalls).toBe(1);
});

test("renders an authenticated admin feedback attachment preview", async ({ page }) => {
  const admin = { id: 1, email: "admin@example.com", name: "관리자", role: "ADMIN", emailEnabled: false,
    webPushEnabled: false, fcmEnabled: false, discordEnabled: false, onboardingCompleted: true };
  await page.route("**/api/v1/users/me", (route) => route.fulfill({ json: response(admin) }));
  await page.route("**/api/v1/admin/feedbacks?**", (route) => route.fulfill({ json: response({
    content: [{ id: 7, type: "BUG", title: "첨부 확인", status: "PENDING", createdAt: "2026-07-13T10:00:00", hasReplies: false, deleted: false, deletedAt: null, authorLabel: "사용자" }],
    totalElements: 1, totalPages: 1, last: true, size: 30, number: 0, first: true, numberOfElements: 1, empty: false,
  }) }));
  await page.route("**/api/v1/admin/feedbacks/7", (route) => route.fulfill({ json: response({
    id: 7, type: "BUG", title: "첨부 확인", content: "첨부가 있는 문의", status: "PENDING", createdAt: "2026-07-13T10:00:00", deleted: false, deletedAt: null,
    authorLabel: "사용자", attachments: [{ id: 9 }], replies: [],
  }) }));
  let downloadConfirmed = false;
  let downloadCsrf = "";
  await page.route("**/api/v1/admin/feedbacks/7/attachments/9/download", (route) => {
    downloadConfirmed = route.request().postDataJSON()?.confirmed === true;
    downloadCsrf = route.request().headers()["x-csrf-token"];
    return route.fulfill({ status: 200, contentType: "image/png", body: Buffer.from("fake-png") });
  });
  await page.goto("/admin/feedbacks");
  await expect(page.getByRole("group", { name: "사용자 메뉴", exact: true }).getByText("관리자 님", { exact: true })).toBeVisible();
  await page.getByText("첨부 확인").click();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "첨부파일 1" }).click();
  await expect(page.getByText("첨부파일 미리보기")).toBeVisible();
  expect(downloadConfirmed).toBe(true);
  expect(downloadCsrf).toBe("test-csrf");
});
