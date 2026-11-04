import { expect, test } from "@playwright/test";
import type { UserSettingsRequest } from "../src/shared/types/api";

const profile = {
  id: 72, name: "가상 사용자", email: "legacy@example.invalid", role: "USER",
  notificationEmail: "legacy@example.invalid", onboardingCompleted: true,
  emailEnabled: true, webPushEnabled: false, fcmEnabled: true, discordEnabled: false,
};

test.beforeEach(async ({ page, baseURL }) => {
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
});

for (const serverVersion of ["old", "new"] as const) {
  test(`과거 FCM 값은 ${serverVersion} 서버 모의 환경에서도 지원 채널 저장 후 유지된다`, async ({ page }) => {
    let stored = { ...profile };
    const saved: UserSettingsRequest[] = [];
    const unexpectedWrites: string[] = [];
    await page.route("**/api/**", route => {
      const request = route.request();
      const path = new URL(request.url()).pathname;
      if (request.method() === "GET" && path === "/api/v1/users/me") {
        return route.fulfill({ json: stored });
      }
      if (request.method() === "GET" && path === "/api/auth/csrf") {
        return route.fulfill({ json: { token: "local-test-csrf" } });
      }
      if (request.method() === "GET" && path === "/api/v1/users/devices") {
        return route.fulfill({ json: [] });
      }
      if (request.method() === "PATCH" && path === "/api/v1/users/settings") {
        const payload = request.postDataJSON() as UserSettingsRequest;
        saved.push(payload);
        // 신 서버는 기존 true의 echo만 허용하고 신규 활성화는 거절한다.
        if (serverVersion === "new" && payload.fcmEnabled && !stored.fcmEnabled) {
          return route.fulfill({ status: 400, json: { code: "INVALID_INPUT" } });
        }
        stored = {
          ...stored,
          ...payload,
          fcmEnabled: serverVersion === "old" ? payload.fcmEnabled : stored.fcmEnabled,
        };
        return route.fulfill({ json: stored });
      }
      if (request.method() !== "GET") {
        unexpectedWrites.push(`${request.method()} ${path}`);
        return route.fulfill({ status: 500, json: { code: "UNEXPECTED_WRITE" } });
      }
      return route.fulfill({ json: [] });
    });

    await page.goto("/settings");
    const save = page.getByRole("button", { name: "설정 저장하기", exact: true });
    await expect(save).toBeEnabled();
    const emailSwitch = page.getByText("이메일 알림 활성화", { exact: true })
      .locator("..").locator(":scope > div");
    await expect(emailSwitch).toHaveCount(1);
    await emailSwitch.click();
    await save.click();
    await expect(page.getByText("알림 설정이 저장되었습니다.", { exact: true })).toBeVisible();
    expect(saved).toHaveLength(1);
    expect(saved[0]).toMatchObject({ fcmEnabled: true, emailEnabled: false, webPushEnabled: false });
    expect(stored.fcmEnabled).toBe(true);
    expect(stored.emailEnabled).toBe(false);
    expect(unexpectedWrites).toEqual([]);
  });
}
