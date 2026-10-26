import { expect, test, type BrowserContext, type Page } from "@playwright/test";

const admin = { id: 1, email: "admin@example.test", name: "관리자", role: "ADMIN", emailEnabled: false,
  webPushEnabled: false, fcmEnabled: false, discordEnabled: false, onboardingCompleted: true };

async function prepareServer(context: BrowserContext, baseURL: string) {
  const state = { session: "A", authenticated: true, csrfRequests: 0, logoutRequests: 0,
    errorCode: null as string | null, malformed: false, downloads: [] as (string | undefined)[], unexpected: [] as string[] };
  await context.addCookies([{ name: "SESSION", value: "A", url: baseURL, httpOnly: true, sameSite: "Lax" }]);
  await context.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL).origin
    ? route.continue() : route.abort());
  await context.route("**/api/**", route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const csrf = request.headers()["x-csrf-token"];
    const authenticated = state.authenticated && request.headers().cookie?.includes(`SESSION=${state.session}`);
    if (path === "/api/v1/users/me") return route.fulfill({ status: authenticated ? 200 : 401, json: authenticated ? admin : { code: "A001" } });
    if (path === "/api/auth/csrf") { state.csrfRequests += 1; return route.fulfill({ json: { token: state.session } }); }
    if (path === "/api/auth/logout" && request.method() === "POST") {
      if (!authenticated || csrf !== state.session) return route.fulfill({ status: 403, json: { code: "A003" } });
      state.logoutRequests += 1;
      state.authenticated = false;
      return route.fulfill({ status: 200, body: "", headers: { "set-cookie": "SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax" } });
    }
    if (path === "/api/v1/admin/feedbacks") return route.fulfill({ json: {
      content: [{ id: 7, type: "BUG", title: "첨부 확인", status: "PENDING", createdAt: "2026-07-13T10:00:00", hasReplies: false, deleted: false, deletedAt: null, authorLabel: "사용자" }],
      totalElements: 1, totalPages: 1, last: true, size: 30, number: 0, first: true, numberOfElements: 1, empty: false,
    } });
    if (path === "/api/v1/admin/feedbacks/7") return route.fulfill({ json: {
      id: 7, type: "BUG", title: "첨부 확인", content: "첨부가 있는 문의", status: "PENDING", createdAt: "2026-07-13T10:00:00", deleted: false, deletedAt: null,
      authorLabel: "사용자", attachments: [{ id: 9 }], replies: [],
    } });
    if (path === "/api/v1/admin/feedbacks/7/attachments/9/download" && request.method() === "POST") {
      expect(request.postDataJSON()?.confirmed).toBe(true);
      state.downloads.push(csrf);
      if (!authenticated) return route.fulfill({ status: 401, json: { code: "A001" } });
      if (csrf !== state.session) return route.fulfill({ status: 403, json: { code: "A003" } });
      if (state.errorCode) return route.fulfill({ status: 403, json: { code: state.errorCode } });
      if (state.malformed) return route.fulfill({ status: 403, contentType: "application/json", body: "{" });
      return route.fulfill({ contentType: "image/png", body: Buffer.from("fake-png") });
    }
    state.unexpected.push(`${request.method()} ${path}`);
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });
  await context.route("**/oauth2/authorization/google", route => {
    state.session = "B";
    state.authenticated = true;
    return route.fulfill({ status: 302, headers: { location: "/login", "set-cookie": "SESSION=B; Path=/; HttpOnly; SameSite=Lax" } });
  });
  return state;
}

async function openAttachment(page: Page) {
  page.once("dialog", dialog => dialog.accept());
  await page.getByRole("button", { name: "첨부파일 1", exact: true }).click();
}

test("another tab relogin recovers a Blob CSRF failure on the next click without replay", async ({ context, page, baseURL }) => {
  const state = await prepareServer(context, baseURL!);
  await page.goto("/admin/feedbacks");
  await page.getByRole("heading", { name: "[버그] 첨부 확인", exact: true }).click();
  await openAttachment(page);
  await expect(page.getByRole("dialog", { name: "첨부파일 미리보기" })).toBeVisible();
  await page.keyboard.press("Escape");

  const other = await context.newPage();
  await other.goto("/login");
  const otherMenu = other.getByRole("group", { name: "사용자 메뉴", exact: true });
  await expect(otherMenu.getByText("관리자 님", { exact: true })).toBeVisible();
  await otherMenu.getByRole("button", { name: "로그아웃", exact: true }).click();
  await expect(otherMenu.getByRole("link", { name: "로그인", exact: true })).toBeVisible();
  await other.goto("/oauth2/authorization/google");
  await expect(otherMenu.getByText("관리자 님", { exact: true })).toBeVisible();
  expect(state.logoutRequests).toBe(1);

  await openAttachment(page);
  await expect(page.getByText("로그인 상태가 변경되었습니다. 다시 시도해 주세요.", { exact: true })).toBeVisible();
  expect(state.downloads).toEqual(["A", "A"]);
  expect(state.csrfRequests).toBe(2);
  await expect(page.getByRole("dialog", { name: "첨부파일 미리보기" })).toHaveCount(0);

  await openAttachment(page);
  await expect(page.getByRole("dialog", { name: "첨부파일 미리보기" })).toBeVisible();
  expect(state.downloads).toEqual(["A", "A", "B"]);
  expect(state.csrfRequests).toBe(3);
  expect(state.unexpected).toEqual([]);
});

test("permission denial and malformed Blob errors stay rejected without token refresh or replay", async ({ context, page, baseURL }) => {
  const state = await prepareServer(context, baseURL!);
  state.errorCode = "A002";
  await page.goto("/admin/feedbacks");
  await page.getByRole("heading", { name: "[버그] 첨부 확인", exact: true }).click();
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const denied = page.waitForResponse(response => response.url().endsWith("/attachments/9/download") && response.status() === 403);
    await openAttachment(page);
    await denied;
    await expect(page.getByText("첨부파일을 열람할 수 없습니다.", { exact: true }).first()).toBeVisible();
  }
  state.errorCode = null;
  state.malformed = true;
  const malformed = page.waitForResponse(response => response.url().endsWith("/attachments/9/download") && response.status() === 403);
  await openAttachment(page);
  await malformed;
  await expect(page.getByRole("button", { name: "첨부파일 1", exact: true })).toBeEnabled();
  expect(state.downloads).toEqual(["A", "A", "A"]);
  expect(state.csrfRequests).toBe(1);
  expect(state.unexpected).toEqual([]);
  await expect(page.getByText("로그인 상태가 변경되었습니다. 다시 시도해 주세요.", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("dialog", { name: "첨부파일 미리보기" })).toHaveCount(0);
  await expect(page.getByRole("group", { name: "사용자 메뉴", exact: true }).getByText("관리자 님", { exact: true })).toBeVisible();
});
