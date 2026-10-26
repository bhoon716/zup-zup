import { expect, test } from "@playwright/test";

type AuthLifecycleWindow = Window & typeof globalThis & {
  __authLifecycle: { announcementRequests: number; refreshLoadEvents: number };
  __authLifecycleRefreshSettled: () => Promise<void>;
};

test.describe.configure({ retries: 0 });

test("logout prevents a delayed refresh from replaying the previous request", async ({ page, baseURL }) => {
  const appOrigin = new URL(baseURL!).origin;
  const user = {
    id: 1,
    email: "lifecycle@example.com",
    name: "인증 흐름 사용자",
    role: "USER",
    emailEnabled: false,
    webPushEnabled: false,
    fcmEnabled: false,
    discordEnabled: false,
    onboardingCompleted: true,
  };
  let authenticated = true;
  let announcementRequests = 0;
  let refreshRequests = 0;
  let logoutRequests = 0;
  const unexpectedApiRequests: string[] = [];
  let releaseRefresh!: () => void;
  const refreshGate = new Promise<void>((resolve) => { releaseRefresh = resolve; });
  let signalRefreshRequested!: () => void;
  const refreshRequested = new Promise<void>((resolve) => { signalRefreshRequested = resolve; });
  let signalRefreshSettled!: () => void;
  const refreshSettled = new Promise<void>((resolve) => { signalRefreshSettled = resolve; });

  await page.exposeFunction("__authLifecycleRefreshSettled", signalRefreshSettled);
  await page.addInitScript(() => {
    const observedWindow = window as AuthLifecycleWindow;
    observedWindow.__authLifecycle = { announcementRequests: 0, refreshLoadEvents: 0 };
    const open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (
      this: XMLHttpRequest,
      method: string,
      url: string | URL,
      async: boolean = true,
      username?: string | null,
      password?: string | null,
    ) {
      open.call(this, method, url, async, username, password);
      const pathname = new URL(String(url), window.location.origin).pathname;
      if (pathname === "/api/v1/announcements") {
        observedWindow.__authLifecycle.announcementRequests += 1;
      }
      if (pathname === "/api/auth/refresh") {
        this.addEventListener("loadend", () => {
          observedWindow.__authLifecycle.refreshLoadEvents += 1;
          queueMicrotask(() => {
            requestAnimationFrame(() => {
              void observedWindow.__authLifecycleRefreshSettled();
            });
          });
        }, { once: true });
      }
    };
  });

  await page.route("**/*", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const headers = {
      "access-control-allow-origin": appOrigin,
      "access-control-allow-credentials": "true",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type",
    };
    if (request.method() === "OPTIONS" && url.pathname.startsWith("/api/")) {
      await route.fulfill({ headers, status: 204, body: "" });
      return;
    }
    if (url.pathname === "/api/v1/users/me") {
      await route.fulfill({ headers, status: authenticated ? 200 : 401, json: authenticated ? user : { code: "A001", message: "guest" } });
      return;
    }
    if (url.pathname === "/api/v1/announcements") {
      announcementRequests += 1;
      await route.fulfill({ headers, status: 401, json: { code: "A001", message: "expired" } });
      return;
    }
    if (url.pathname === "/api/auth/refresh") {
      refreshRequests += 1;
      signalRefreshRequested();
      await refreshGate;
      await route.fulfill({ headers, json: "fake-late-access-token" });
      return;
    }
    if (url.pathname === "/api/auth/logout") {
      logoutRequests += 1;
      authenticated = false;
      await route.fulfill({ headers, status: 200, body: "" });
      return;
    }
    if (url.pathname.startsWith("/api/")) {
      unexpectedApiRequests.push(`${request.method()} ${url.pathname}`);
      await route.abort("blockedbyclient");
      return;
    }
    if (url.origin !== appOrigin) {
      await route.abort("blockedbyclient");
      return;
    }
    await route.continue();
  });

  try {
    await page.goto("/login");
    const userMenu = page.getByRole("group", { name: "사용자 메뉴", exact: true });
    await expect(userMenu.getByText(`${user.name} 님`, { exact: true })).toBeVisible();
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user.id)).toBe(user.id);

    await page.getByRole("navigation", { name: "주요 메뉴" }).getByRole("link", { name: "공지사항", exact: true }).click();
    await refreshRequested;
    expect(announcementRequests).toBe(1);

    const logoutResponse = page.waitForResponse((response) => new URL(response.url()).pathname === "/api/auth/logout");
    await userMenu.getByRole("button", { name: "로그아웃", exact: true }).click();
    const response = await logoutResponse;
    expect(response.request().method()).toBe("POST");
    expect(response.status()).toBe(200);
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole("button", { name: "Google로 시작하기", exact: true })).toBeVisible();
    await expect(userMenu.getByText(`${user.name} 님`, { exact: true })).toHaveCount(0);
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user)).toBeNull();

    releaseRefresh();
    await refreshSettled;
    expect(await page.evaluate(() => (window as AuthLifecycleWindow).__authLifecycle)).toEqual({
      announcementRequests: 1,
      refreshLoadEvents: 1,
    });
    expect(announcementRequests).toBe(1);
    expect(refreshRequests).toBe(1);
    expect(logoutRequests).toBe(1);
    expect(unexpectedApiRequests).toEqual([]);
    expect(await page.evaluate(() => JSON.parse(localStorage.getItem("auth-storage")!).state.user)).toBeNull();
    await expect(page).toHaveURL(/\/login$/);
  } finally {
    releaseRefresh();
  }
});
