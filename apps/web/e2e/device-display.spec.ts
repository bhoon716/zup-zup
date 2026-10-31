import { expect, test } from "@playwright/test";

const profile = {
  id: 31, name: "로컬 사용자", email: "device@example.invalid", role: "USER",
  notificationEmail: "device@example.invalid", onboardingCompleted: true,
  emailEnabled: true, webPushEnabled: false, fcmEnabled: false, discordEnabled: false,
};
const devices = [
  { id: 11, type: "WEB", alias: "내 맥북", registeredAt: "2026-10-31T15:00:00Z" },
  { id: 12, type: "WEB", alias: "내 아이폰", registeredAt: "2026-10-31T16:00:00Z" },
];

test.beforeEach(async ({ page, baseURL }) => {
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
});

test("계정 전체 기기 목록은 순서가 바뀌어도 첫 기기를 현재 기기로 표시하지 않는다", async ({ page }) => {
  let listed = devices;
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") return route.fulfill({ json: profile });
    if (path === "/api/v1/users/devices") return route.fulfill({ json: listed });
    return route.fulfill({ json: [] });
  });
  await page.goto("/settings");
  for (const order of [devices, [...devices].reverse()]) {
    listed = order;
    await page.reload();
    await expect(page.getByRole("heading", { name: "내 계정에 등록된 기기" })).toBeVisible();
    await expect(page.getByText("같은 계정에 등록한 다른 기기도 함께 표시됩니다.")).toBeVisible();
    await expect(page.getByText("내 맥북", { exact: true })).toBeVisible();
    await expect(page.getByText("내 아이폰", { exact: true })).toBeVisible();
    await expect(page.getByText("THIS", { exact: true })).toHaveCount(0);
  }
});

for (const [name, userAgent, expectedAlias] of [
  ["맥북", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15", "Safari on Mac"],
  ["아이폰", "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1", "Safari on iOS Device"],
]) {
  test(`${name} UA의 기본 기기 이름과 구독 주소를 각각 등록한다`, async ({ page }) => {
    const registered: Record<string, unknown>[] = [];
    await page.addInitScript(({ userAgent, name }) => {
      Object.defineProperty(navigator, "userAgent", { configurable: true, get: () => userAgent });
      const subscription = { toJSON: () => ({
        endpoint: `https://example.invalid/${name}/push`, keys: { p256dh: "fake", auth: "fake" },
      }) };
      const registration = { pushManager: {
        getSubscription: async () => null, subscribe: async () => subscription,
      } };
      Object.defineProperty(window, "PushManager", { configurable: true, value: class {} });
      Object.defineProperty(navigator, "serviceWorker", { configurable: true, value: {
        register: async () => registration, ready: Promise.resolve(registration),
        addEventListener: () => {}, removeEventListener: () => {},
      } });
    }, { userAgent, name });
    await page.route("**/api/**", route => {
      const request = route.request();
      const path = new URL(request.url()).pathname;
      if (path === "/api/auth/csrf") return route.fulfill({ json: { token: "local-test-csrf" } });
      if (path === "/api/v1/users/me") return route.fulfill({ json: { ...profile, onboardingCompleted: false } });
      if (path === "/api/v1/users/devices" && request.method() === "POST") {
        registered.push(request.postDataJSON());
        return route.fulfill({ status: 200, body: "" });
      }
      return route.fulfill({ json: [] });
    });
    await page.goto("/onboarding");
    await page.getByRole("switch", { name: "웹 푸시 (현재 기기)" }).click();
    await expect.poll(() => registered.length).toBe(1);
    expect(registered[0]).toMatchObject({
      type: "WEB", alias: expectedAlias, token: `https://example.invalid/${name}/push`,
    });
  });
}
