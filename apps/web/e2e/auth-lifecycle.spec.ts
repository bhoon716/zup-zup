import { expect, test } from "@playwright/test";

type SessionLifecycleWindow = Window & typeof globalThis & {
  __sessionLifecycle: { notificationRequests: number; csrfLoadEvents: number };
  __sessionLifecycleCsrfSettled: () => Promise<void>;
};

const user = {
  id: 1, email: "lifecycle@example.com", name: "인증 흐름 사용자", role: "USER",
  emailEnabled: false, webPushEnabled: false, fcmEnabled: false, discordEnabled: false,
  onboardingCompleted: true,
};

test.describe.configure({ retries: 0 });

test.beforeEach(async ({ page, baseURL }) => {
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
  await page.route("**/api/**", route => route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } }));
});

test("authenticated logout sends CSRF and clears the displayed and persisted user", async ({ page }) => {
  let authenticated = true;
  let csrfRequests = 0;
  let logoutRequests = 0;
  let logoutCsrf: string | undefined;
  const unexpected: string[] = [];
  await page.route("**/api/**", route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/v1/users/me") {
      return route.fulfill({ status: authenticated ? 200 : 401, json: authenticated ? user : { code: "A001" } });
    }
    if (path === "/api/auth/csrf") { csrfRequests += 1; return route.fulfill({ json: { token: "logout-csrf" } }); }
    if (path === "/api/auth/logout" && request.method() === "POST") {
      logoutRequests += 1;
      logoutCsrf = request.headers()["x-csrf-token"];
      authenticated = false;
      return route.fulfill({ status: 200, body: "" });
    }
    unexpected.push(`${request.method()} ${path}`);
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });
  await page.goto("/login");
  const menu = page.getByRole("group", { name: "사용자 메뉴", exact: true });
  await expect(menu.getByText(`${user.name} 님`, { exact: true })).toBeVisible();
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user.id)).toBe(user.id);
  await menu.getByRole("button", { name: "로그아웃", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await expect(menu.getByRole("link", { name: "로그인", exact: true })).toBeVisible();
  await expect(menu.getByText(`${user.name} 님`, { exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user)).toBeNull();
  expect({ csrfRequests, logoutRequests, logoutCsrf }).toEqual({ csrfRequests: 1, logoutRequests: 1, logoutCsrf: "logout-csrf" });
  expect(unexpected).toEqual([]);
});

test("session expiry cancels a write waiting for an old CSRF response", async ({ page }) => {
  let releaseCsrf!: () => void;
  const csrfGate = new Promise<void>(resolve => { releaseCsrf = resolve; });
  let signalCsrfRequested!: () => void;
  const csrfRequested = new Promise<void>(resolve => { signalCsrfRequested = resolve; });
  let signalCsrfSettled!: () => void;
  const csrfSettled = new Promise<void>(resolve => { signalCsrfSettled = resolve; });
  let notificationRequests = 0;
  let csrfRequests = 0;
  const unexpected: string[] = [];

  await page.exposeFunction("__sessionLifecycleCsrfSettled", signalCsrfSettled);
  await page.addInitScript(() => {
    const observed = window as SessionLifecycleWindow;
    observed.__sessionLifecycle = { notificationRequests: 0, csrfLoadEvents: 0 };
    const open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (
      this: XMLHttpRequest, method: string, url: string | URL, async: boolean = true,
      username?: string | null, password?: string | null,
    ) {
      open.call(this, method, url, async, username, password);
      const path = new URL(String(url), window.location.origin).pathname;
      if (path === "/api/v1/notifications/test") observed.__sessionLifecycle.notificationRequests += 1;
      if (path === "/api/auth/csrf") {
        this.addEventListener("loadend", () => {
          observed.__sessionLifecycle.csrfLoadEvents += 1;
          queueMicrotask(() => requestAnimationFrame(() => { void observed.__sessionLifecycleCsrfSettled(); }));
        }, { once: true });
      }
    };
  });
  await page.route("**/api/**", async route => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/v1/users/me") { await route.fulfill({ json: user }); return; }
    if (path === "/api/v1/users/devices") { await route.fulfill({ json: [] }); return; }
    if (path === "/api/auth/csrf") {
      csrfRequests += 1;
      signalCsrfRequested();
      await csrfGate;
      await route.fulfill({ json: { token: "old-session-csrf" } });
      return;
    }
    if (path === "/api/v1/announcements") { await route.fulfill({ status: 401, json: { code: "A001" } }); return; }
    if (path === "/api/v1/notifications/test") { notificationRequests += 1; await route.fulfill({ body: "" }); return; }
    unexpected.push(`${request.method()} ${path}`);
    await route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });

  try {
    await page.goto("/settings");
    const menu = page.getByRole("group", { name: "사용자 메뉴", exact: true });
    await expect(menu.getByText(`${user.name} 님`, { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "알림 테스트", exact: true }).click();
    await csrfRequested;
    expect(notificationRequests).toBe(0);
    await page.getByRole("navigation", { name: "주요 메뉴" }).getByRole("link", { name: "공지사항", exact: true }).click();
    await expect(page.getByRole("dialog", { name: "로그인이 필요한 서비스입니다" })).toBeVisible();
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user)).toBeNull();
    releaseCsrf();
    await csrfSettled;
    expect(await page.evaluate(() => (window as SessionLifecycleWindow).__sessionLifecycle)).toEqual({
      notificationRequests: 0, csrfLoadEvents: 1,
    });
    expect(notificationRequests).toBe(0);
    expect(csrfRequests).toBe(1);
    expect(unexpected).toEqual([]);
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user)).toBeNull();
  } finally {
    releaseCsrf();
  }
});
