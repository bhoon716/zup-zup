import { expect, test } from "@playwright/test";
import type { UserSettingsRequest } from "../src/shared/types/api";

const profile = {
  id: 72, name: "가상 사용자", email: "user@example.invalid", role: "USER",
  notificationEmail: "user@example.invalid", onboardingCompleted: true,
  emailEnabled: true, webPushEnabled: false, discordEnabled: false,
};

test.beforeEach(async ({ page, baseURL }) => {
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
});

for (const devicesFail of [false, true]) {
  test(`기기 조회 ${devicesFail ? "실패 시 저장을 차단한다" : "성공 시 지원 채널 설정을 저장한다"}`, async ({ page }) => {
    let stored = { ...profile };
    const saved: UserSettingsRequest[] = [];
    const tested: boolean[] = [];
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
        return route.fulfill({ status: devicesFail ? 503 : 200, json: devicesFail ? { code: "UNAVAILABLE" } : [] });
      }
      if (request.method() === "PATCH" && path === "/api/v1/users/settings") {
        const payload = request.postDataJSON() as UserSettingsRequest;
        saved.push(payload);
        stored = { ...stored, ...payload };
        return route.fulfill({ json: stored });
      }
      if (request.method() === "POST" && path === "/api/v1/notifications/test") {
        tested.push(stored.emailEnabled);
        return route.fulfill({ status: 204 });
      }
      if (request.method() !== "GET") {
        unexpectedWrites.push(`${request.method()} ${path}`);
        return route.fulfill({ status: 500, json: { code: "UNEXPECTED_WRITE" } });
      }
      return route.fulfill({ json: [] });
    });

    await page.goto("/settings");
    const save = page.getByRole("button", { name: "변경 저장", exact: true });
    await expect(save).toBeDisabled();
    if (devicesFail) {
      await expect(page.getByText("프로필 정보를 확인한 뒤 다시 시도해 주세요.", { exact: true })).toBeVisible();
      expect(saved).toEqual([]);
      expect(stored).toEqual(profile);
    } else {
      const emailSwitch = page.getByRole("switch", { name: "이메일 알림", exact: true });
      await expect(emailSwitch).toBeChecked();
      await expect(page.getByRole("img", { name: "인증된 이메일" })).toBeVisible();
      const email = page.getByRole("textbox", { name: "수신 이메일" });
      const cancel = page.getByRole("button", { name: "변경 취소" });
      await email.fill("changed@example.invalid");
      await expect(page.getByRole("button", { name: "인증하기" })).toBeVisible();
      await cancel.click();
      await expect(page).toHaveURL(/\/settings$/);
      await expect(email).toHaveValue(profile.email);
      await expect(cancel).toBeDisabled();

      await emailSwitch.focus();
      await emailSwitch.press("Space");
      await expect(emailSwitch).not.toBeChecked();
      const alias = page.getByRole("textbox", { name: "기기 별칭" });
      await alias.fill("   ");
      await alias.press("Enter");
      await expect(alias).toHaveAccessibleDescription("기기 별칭을 입력해 주세요.");
      expect(saved).toEqual([]);

      await page.getByRole("button", { name: "알림 테스트" }).click();
      await expect(page.getByText("알림 테스트를 전송했습니다.", { exact: true })).toBeVisible();
      expect(tested).toEqual([true]);

      await save.click();
      await expect(page.getByText("알림 설정이 저장되었습니다.", { exact: true })).toBeVisible();
      expect(saved).toEqual([{
        notificationEmail: profile.email, emailEnabled: false, webPushEnabled: false, discordEnabled: false,
      }]);
      expect(stored.emailEnabled).toBe(false);
      await expect(save).toBeDisabled();
    }
    expect(unexpectedWrites).toEqual([]);
  });
}
