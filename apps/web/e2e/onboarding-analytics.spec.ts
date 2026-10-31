import { expect, test, type Page } from "@playwright/test";

const response = (data: unknown) => data;
const profile = {
  id: 71, email: "onboarding@example.invalid", name: "로컬 사용자", role: "USER",
  emailEnabled: true, webPushEnabled: false, fcmEnabled: false, discordEnabled: false,
  onboardingCompleted: false,
};

async function captureEvents(page: Page, baseURL: string) {
  const events: unknown[][] = [];
  await page.exposeFunction("captureLocalGa", (...args: unknown[]) => {
    if (args[0] === "event" && String(args[1]).startsWith("onboarding_")) events.push(args);
  });
  await page.addInitScript(() => {
    window.gtag = (...args: unknown[]) => {
      void (window as unknown as { captureLocalGa: (...args: unknown[]) => Promise<void> }).captureLocalGa(...args);
    };
    Reflect.deleteProperty(window, "PushManager");
  });
  // 분석 스크립트/수집을 포함한 모든 외부 요청을 차단한다.
  await page.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL).origin
    ? route.continue() : route.abort());
  return events;
}

test("온보딩 시작은 새로고침·뒤로가기·앞으로가기에서 중복되지 않고 이탈은 실패가 아니다", async ({ page, baseURL }) => {
  const events = await captureEvents(page, baseURL!);
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") return route.fulfill({ json: response(profile) });
    return route.fulfill({ json: response([]) });
  });
  await page.goto("/onboarding?discord=error&token=private");
  await expect(page.getByRole("button", { name: "완료", exact: true })).toBeVisible();
  await expect.poll(() => events.map(event => event[1])).toEqual(["onboarding_started"]);
  await page.reload();
  await expect(page.getByRole("button", { name: "완료", exact: true })).toBeVisible();
  // 가드가 있는 앱 화면 대신 로컬 정적 문서로 이동하여 실제 문서 복귀를 확인한다.
  await page.goto("/manifest.json");
  await page.goBack();
  await expect(page.getByRole("button", { name: "완료", exact: true })).toBeVisible();
  await page.goForward();
  expect(events.map(event => event[1])).toEqual(["onboarding_started"]);
  expect(events[0][2]).toEqual({
    send_to: "G-LOCALTEST", page_path: "/onboarding", page_location: `${baseURL}/onboarding`,
  });
});

test("저장 대기·중복 제출·503 실패 후 재시도 성공을 구분하고 푸시 미지원은 실패로 세지 않는다", async ({ page, baseURL }) => {
  const events = await captureEvents(page, baseURL!);
  let completed = false;
  let saves = 0;
  let release!: () => void;
  const gate = new Promise<void>(resolve => { release = resolve; });
  await page.route("**/api/**", async route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/auth/csrf") return route.fulfill({ json: { token: "local-test-csrf" } });
    if (path === "/api/v1/users/me") return route.fulfill({ json: response({ ...profile, onboardingCompleted: completed }) });
    if (path === "/api/v1/users/onboard") {
      saves++;
      if (saves === 1) {
        await gate;
        return route.fulfill({ status: 503, json: { message: "로컬 저장 오류" } });
      }
      completed = true;
      return route.fulfill({ json: response({ ...profile, ...route.request().postDataJSON(), onboardingCompleted: true }) });
    }
    return route.fulfill({ json: response([]) });
  });
  await page.goto("/onboarding?token=private");
  const complete = page.locator('form button[type="submit"]');
  await expect.poll(() => events.map(event => event[1])).toEqual(["onboarding_started"]);
  await page.getByPlaceholder("기기 이름 (예: 내 노트북, 스마트폰)").fill("개인 기기 이름");
  await page.getByRole("switch", { name: "웹 푸시 (현재 기기)" }).click();
  await expect(page.getByText(/이 브라우저에서는 웹 푸시/)).toBeVisible();
  await complete.click();
  await expect.poll(() => saves).toBe(1);
  await expect(complete).toBeDisabled();
  await complete.evaluate(button => button.closest("form")!.requestSubmit());
  expect(saves).toBe(1);
  expect(events.map(event => event[1])).toEqual(["onboarding_started"]);
  release();
  await expect.poll(() => events.map(event => event[1])).toEqual(["onboarding_started", "onboarding_failed"]);
  await expect(complete).toBeEnabled();
  await complete.click();
  await expect(page).toHaveURL(/\/$/);
  await expect.poll(() => events.map(event => event[1])).toEqual([
    "onboarding_started", "onboarding_failed", "onboarding_completed",
  ]);
  expect(saves).toBe(2);
  expect(events.map(event => event[2])).toEqual([
    { send_to: "G-LOCALTEST", page_path: "/onboarding", page_location: `${baseURL}/onboarding` },
    { send_to: "G-LOCALTEST", page_path: "/onboarding", page_location: `${baseURL}/onboarding`, failure_reason: "server" },
    { send_to: "G-LOCALTEST", page_path: "/onboarding", page_location: `${baseURL}/onboarding` },
  ]);
  // 완료된 사용자가 다시 들어와도 시작/완료를 추가하지 않는다.
  await page.goto("/onboarding");
  await expect(page).toHaveURL(/\/$/);
  expect(events).toHaveLength(3);
});

test("웹 푸시 권한 거부 후 이메일로 완료하면 저장 실패 이벤트가 없다", async ({ page, baseURL }) => {
  const events = await captureEvents(page, baseURL!);
  await page.addInitScript(() => {
    const registration = { pushManager: {
      getSubscription: async () => null,
      subscribe: async () => { throw new DOMException("Permission denied", "NotAllowedError"); },
    } };
    Object.defineProperty(window, "PushManager", { configurable: true, value: class {} });
    Object.defineProperty(navigator, "serviceWorker", { configurable: true, value: {
      register: async () => registration, ready: Promise.resolve(registration),
      addEventListener: () => {}, removeEventListener: () => {},
    } });
  });
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/auth/csrf") return route.fulfill({ json: { token: "local-test-csrf" } });
    if (path === "/api/v1/users/me") return route.fulfill({ json: response(profile) });
    if (path === "/api/v1/users/onboard") {
      return route.fulfill({ json: response({ ...profile, ...route.request().postDataJSON(), onboardingCompleted: true }) });
    }
    return route.fulfill({ json: response([]) });
  });
  await page.goto("/onboarding");
  const push = page.getByRole("switch", { name: "웹 푸시 (현재 기기)" });
  await push.click();
  await expect(page.getByText(/웹 푸시 권한이 허용되지/)).toBeVisible();
  await expect(push).not.toBeChecked();
  await page.getByRole("button", { name: "완료", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  await expect.poll(() => events.map(event => event[1])).toEqual(["onboarding_started", "onboarding_completed"]);
});
