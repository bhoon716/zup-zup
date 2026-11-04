import { expect, test } from '@playwright/test';

const initialUser = {
  id: 1, email: 'onboarding@example.invalid', name: '테스트 사용자', role: 'USER',
  emailEnabled: true, webPushEnabled: false, fcmEnabled: false, discordEnabled: false,
  onboardingCompleted: false,
};

test.beforeEach(async ({ page, baseURL }) => {
  await page.route('**/*', route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
});

for (const mode of ['unsupported', 'denied', 'supported', 'retry'] as const) {
  test(`onboarding push ${mode}: explicit opt-in and safe completion`, async ({ page }) => {
    let completed = false;
    let devices = 0;
    const saved: Record<string, unknown>[] = [];
    await page.addInitScript(({ mode }) => {
      if (mode === 'unsupported') {
        Reflect.deleteProperty(window, 'PushManager');
        return;
      }
      const subscription = {
        toJSON: () => ({ endpoint: 'https://example.invalid/push', keys: { p256dh: 'fake', auth: 'fake' } }),
      };
      const registration = {
        pushManager: {
          getSubscription: async () => null,
          subscribe: async () => {
            if (mode === 'denied') throw new DOMException('Permission denied', 'NotAllowedError');
            return subscription;
          },
        },
      };
      Object.defineProperty(window, 'PushManager', { configurable: true, value: class {} });
      Object.defineProperty(navigator, 'serviceWorker', { configurable: true, value: {
        register: async () => registration,
        ready: Promise.resolve(registration),
        addEventListener: () => {}, removeEventListener: () => {},
      } });
    }, { mode });
    await page.route('**/api/**', async route => {
      const request = route.request();
      const path = new URL(request.url()).pathname;
      if (path === '/api/auth/csrf') return route.fulfill({ json: { token: 'local-test-csrf' } });
      if (path === '/api/v1/users/me') return route.fulfill({ json: { ...initialUser, onboardingCompleted: completed } });
      if (path === '/api/v1/users/devices' && request.method() === 'POST') {
        devices++;
        if (mode === 'retry' && devices === 1) return route.fulfill({ status: 503, json: { message: '기기 등록 요청에 실패했습니다.' } });
        return route.fulfill({ status: 200, body: '' });
      }
      if (path === '/api/v1/users/onboard') {
        const payload = request.postDataJSON();
        saved.push(payload);
        completed = true;
        return route.fulfill({ json: { ...initialUser, ...payload, onboardingCompleted: true } });
      }
      return route.fulfill({ json: [] });
    });
    await page.goto('/onboarding');
    const push = page.getByRole('switch', { name: '웹 푸시 (현재 기기)' });
    await expect(push).not.toBeChecked();
    expect(devices).toBe(0);
    await page.getByPlaceholder('기기 이름 (예: 내 노트북, 스마트폰)').fill('로컬 테스트');
    await push.click();
    if (mode === 'unsupported' || mode === 'denied') {
      await expect(page.getByText(mode === 'unsupported' ? /이 브라우저에서는 웹 푸시/ : /웹 푸시 권한이 허용되지/)).toBeVisible();
      await expect(push).not.toBeChecked();
      expect(devices).toBe(0);
    } else if (mode === 'retry') {
      await expect(page.getByText('Request failed with status code 503', { exact: true })).toBeVisible();
      await expect(push).not.toBeChecked();
      expect(saved).toHaveLength(0);
      await push.click();
      await expect(push).toBeChecked();
      expect(devices).toBe(2);
    } else {
      await expect(push).toBeChecked();
      expect(devices).toBe(1);
    }
    await page.getByRole('button', { name: '완료', exact: true }).click();
    await expect(page).toHaveURL(/\/$/);
    expect(saved).toHaveLength(1);
    expect(saved[0]).toMatchObject({ emailEnabled: true, webPushEnabled: mode === 'supported' || mode === 'retry' });
  });
}
