import { expect, test } from "@playwright/test";

const inboxPath = "/api/v1/notifications/inbox";
const inboxName = "알림함";

test.use({ userAgent: "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15" });

test("비로그인 새로고침에서는 프로필 응답을 기다리는 동안에도 공개 헤더가 보인다", async ({ page }) => {
  let finishProfile = () => {};
  const inboxRequests: string[] = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname === inboxPath) inboxRequests.push(request.url());
  });
  const pendingProfile = new Promise<void>((resolve) => { finishProfile = resolve; });
  await page.route("**/api/v1/users/me", async (route) => {
    await pendingProfile;
    await route.fulfill({ status: 401, json: { code: "A001" } });
  });
  await page.setViewportSize({ width: 1440, height: 900 });
  try {
    const profileRequest = page.waitForRequest("**/api/v1/users/me");
    await page.goto("/", { waitUntil: "domcontentloaded" });
    await profileRequest;
    const header = page.getByRole("banner");
    await expect(header.getByRole("link", { name: "로그인", exact: true })).toBeVisible();
    await expect(header.getByRole("link", { name: "강의 검색" })).toBeVisible();
    await expect(header.getByRole("link", { name: "내 시간표" })).toHaveCount(0);
    await expect(header.locator(".animate-pulse")).toHaveCount(0);
    await expect(header.getByRole("button", { name: inboxName })).toHaveCount(0);
    expect(inboxRequests).toEqual([]);
  } finally {
    finishProfile();
  }
});

test("관리자 헤더는 화면 너비에 맞춰 메뉴를 전환하고 버튼이 겹치지 않는다", async ({ page, baseURL }) => {
  const admin = {
    id: 1,
    name: "이름이아주길어도메뉴버튼과겹치지않는관리자",
    email: "admin@example.invalid",
    role: "ADMIN",
    onboardingCompleted: true,
  };
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") return route.fulfill({ json: admin });
    if (pathname === inboxPath) {
      return route.fulfill({ json: { content: [], number: 0, totalPages: 0, totalElements: 0 } });
    }
    return route.fulfill({ json: [] });
  });

  await page.setViewportSize({ width: 1280, height: 1254 });
  await page.goto("/notifications");
  const header = page.getByRole("banner");
  const inboxTrigger = header.getByRole("button", { name: inboxName });
  const menuTrigger = header.getByRole("button", { name: "메뉴 열기" });
  const desktopNav = header.getByRole("navigation", { name: "주요 메뉴" });
  await expect(header.getByRole("button", { name: "로그아웃" })).toBeVisible();
  await page.evaluate(() => document.fonts.ready);

  for (const width of [820, 1256, 1280, 1536]) {
    await page.setViewportSize({ width, height: 1254 });
    const isCompact = width < 1280;
    await expect(inboxTrigger).toBeVisible();
    if (isCompact) {
      await expect(menuTrigger).toBeVisible();
      await expect(desktopNav).toBeHidden();
    } else {
      await expect(menuTrigger).toBeHidden();
      await expect(desktopNav).toBeVisible();
      await expect(header.getByRole("button", { name: "공유 → 홈 추가" })).toBeVisible();
      await expect(header.getByRole("button", { name: "로그아웃" })).toBeVisible();
    }

    const geometry = await header.evaluate((element) => {
      const controls = [...element.querySelectorAll("a, button")]
        .filter((control) => control.getClientRects().length > 0)
        .map((control) => control.getBoundingClientRect())
        .sort((first, second) => first.left - second.left);
      return {
        pageWidth: document.documentElement.scrollWidth,
        controlsFit: controls.every((control, index) =>
          control.left >= 0 && control.right <= document.documentElement.clientWidth
          && (index === 0 || controls[index - 1].right <= control.left),
        ),
      };
    });
    expect(geometry, `${width}px 헤더`).toEqual({ pageWidth: width, controlsFit: true });
  }

  await desktopNav.getByRole("button", { name: "관리자" }).click();
  await expect(page.getByRole("menuitem", { name: "대시보드" })).toBeVisible();
  await page.keyboard.press("Escape");

  await page.setViewportSize({ width: 390, height: 500 });
  await menuTrigger.click();
  const compactMenu = page.getByRole("dialog");
  await expect(compactMenu.getByText(`${admin.name} 님`)).toBeVisible();
  await expect(compactMenu.getByRole("heading", { level: 3 })).toHaveText(["메뉴", "바로가기", "관리 메뉴"]);
  await expect(compactMenu.getByRole("link", { name: "대시보드" })).toHaveAttribute("href", "/admin");
  const logout = compactMenu.getByRole("button", { name: "로그아웃" });
  await logout.scrollIntoViewIfNeeded();
  await expect(logout).toBeInViewport();
  await page.keyboard.press("Escape");
  await expect(compactMenu).toBeHidden();
  await expect(menuTrigger).toBeFocused();
});

test("알림함 버튼은 열었을 때 최신 다섯 알림을 조회하고 전체 알림함으로 이동한다", async ({ page, baseURL }) => {
  const viewer = {
    id: 1,
    name: "사용자",
    email: "user@example.invalid",
    role: "USER",
    onboardingCompleted: true,
  };
  const notifications = Array.from({ length: 6 }, (_, index) => ({
    id: 6 - index,
    type: "ANNOUNCEMENT",
    title: `공지 ${6 - index}`,
    message: null,
    readAt: index === 5 ? null : "2026-11-12T10:00:00",
    occurredAt: `2026-11-0${6 - index}T10:00:00`,
    targetId: null,
    available: true,
  }));
  const inboxRequests: string[] = [];

  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") return route.fulfill({ json: viewer });
    if (pathname === "/api/v1/announcements") {
      return route.fulfill({ json: { content: [], number: 0, totalPages: 0, totalElements: 0 } });
    }
    if (pathname.startsWith(inboxPath)) {
      inboxRequests.push(searchParams.get("read") ?? "ALL");
      if (pathname === inboxPath) {
        expect(searchParams.get("page")).toBe("0");
        expect(searchParams.get("sort")).toBe("NEWEST");
      }
      const content = searchParams.get("read") === "UNREAD"
        ? notifications.filter(({ readAt }) => readAt === null)
        : notifications;
      return route.fulfill({ json: { content, number: 0, totalPages: 1, totalElements: content.length } });
    }
    return route.fulfill({ json: [] });
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/announcements");
  const inboxTrigger = page.getByRole("banner").getByRole("button", { name: inboxName });
  await expect(inboxTrigger).toBeVisible();
  await expect(inboxTrigger).toHaveAccessibleDescription("읽지 않은 알림이 있습니다.");
  expect(inboxRequests).toEqual(["UNREAD"]);

  await inboxTrigger.press("Enter");
  const preview = page.getByRole("menu", { name: inboxName });
  await expect(preview.getByRole("listitem")).toHaveText([
    /공지 6/, /공지 5/, /공지 4/, /공지 3/, /공지 2/,
  ]);
  await expect(preview.getByRole("menuitem", { name: "모두 보기" })).toHaveAttribute("href", "/notifications");
  expect(inboxRequests.filter((filter) => filter === "ALL")).toHaveLength(1);

  await page.keyboard.press("Escape");
  await expect(preview).toBeHidden();
  await expect(inboxTrigger).toBeFocused();

  await page.setViewportSize({ width: 1440, height: 900 });
  await expect(inboxTrigger).toHaveAccessibleDescription("읽지 않은 알림이 있습니다.");
  await inboxTrigger.click();
  await preview.getByRole("menuitem", { name: "모두 보기" }).click();
  await expect(page).toHaveURL(/\/notifications$/);
  await expect(page.getByRole("heading", { name: inboxName, exact: true })).toBeVisible();
});
