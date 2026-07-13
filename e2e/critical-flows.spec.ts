import { expect, test } from "@playwright/test";

const response = (data: unknown) => ({ code: "SUCCESS", message: "ok", data });
const announcementPage = {
  content: [{ id: 1, title: "점검 안내", previewContent: "오늘 밤 점검합니다.", pinned: true, createdAt: "2026-07-13T10:00:00" }],
  pageable: { pageNumber: 0, pageSize: 20, offset: 0 }, totalElements: 1, totalPages: 1, last: true,
  size: 20, number: 0, first: true, numberOfElements: 1, empty: false,
};

test("refreshes the session after a 401 and retries the public request", async ({ page }) => {
  let refreshCalls = 0;
  let announcementCalls = 0;
  await page.route("**/api/v1/users/me", (route) => route.fulfill({ json: response(null) }));
  await page.route("**/api/auth/refresh", (route) => { refreshCalls += 1; return route.fulfill({ json: response(null) }); });
  await page.route("**/api/v1/announcements**", (route) => {
    announcementCalls += 1;
    return announcementCalls === 1
      ? route.fulfill({ status: 401, json: { code: "A001", message: "expired" } })
      : route.fulfill({ json: response(announcementPage) });
  });
  await page.goto("/announcements");
  await expect(page.getByText("점검 안내")).toBeVisible();
  expect(refreshCalls).toBe(1);
  expect(announcementCalls).toBe(2);
});

test("renders an authenticated admin feedback attachment preview", async ({ page }) => {
  const admin = { id: 1, email: "admin@example.com", name: "관리자", role: "ADMIN", emailEnabled: false,
    webPushEnabled: false, fcmEnabled: false, discordEnabled: false, onboardingCompleted: true };
  await page.route("**/api/v1/users/me", (route) => route.fulfill({ json: response(admin) }));
  await page.route("**/api/v1/admin/feedbacks?**", (route) => route.fulfill({ json: response({
    content: [{ id: 7, type: "BUG", title: "첨부 확인", status: "PENDING", createdAt: "2026-07-13T10:00:00", hasReplies: false, deleted: false, deletedAt: null, authorLabel: "사용자" }],
    totalElements: 1, totalPages: 1, last: true, size: 20, number: 0, first: true, numberOfElements: 1, empty: false,
  }) }));
  await page.route("**/api/v1/admin/feedbacks/7", (route) => route.fulfill({ json: response({
    id: 7, type: "BUG", title: "첨부 확인", content: "첨부가 있는 문의", status: "PENDING", createdAt: "2026-07-13T10:00:00", deleted: false, deletedAt: null,
    authorLabel: "사용자", attachments: [{ id: 9 }], replies: [],
  }) }));
  let downloadConfirmed = false;
  await page.route("**/api/v1/admin/feedbacks/7/attachments/9/download", (route) => {
    downloadConfirmed = route.request().postDataJSON()?.confirmed === true;
    return route.fulfill({ status: 200, contentType: "image/png", body: Buffer.from("fake-png") });
  });
  await page.goto("/admin/feedbacks");
  await page.getByText("첨부 확인").click();
  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "첨부파일 1" }).click();
  await expect(page.getByText("첨부파일 미리보기")).toBeVisible();
  expect(downloadConfirmed).toBe(true);
});
