import { expect, test } from "@playwright/test";

test.use({ userAgent: "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15" });

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
    if (pathname === "/api/v1/notifications/inbox") {
      return route.fulfill({ json: { content: [], number: 0, totalPages: 0, totalElements: 0 } });
    }
    return route.fulfill({ json: [] });
  });

  await page.setViewportSize({ width: 1280, height: 1254 });
  await page.goto("/notifications");
  const header = page.getByRole("banner");
  const menuTrigger = header.getByRole("button", { name: "메뉴 열기" });
  const desktopNav = header.getByRole("navigation", { name: "주요 메뉴" });
  await expect(header.getByRole("button", { name: "로그아웃" })).toBeVisible();
  await page.evaluate(() => document.fonts.ready);

  for (const width of [820, 1256, 1280, 1536]) {
    await page.setViewportSize({ width, height: 1254 });
    const isCompact = width < 1280;
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

  await page.setViewportSize({ width: 820, height: 1254 });
  await menuTrigger.click();
  const compactMenu = page.getByRole("dialog");
  await expect(compactMenu.getByRole("link", { name: "알림 / 구독" })).toHaveAttribute("href", "/notifications");
  await expect(compactMenu.getByRole("link", { name: "대시보드" })).toHaveAttribute("href", "/admin");
  await expect(compactMenu.getByRole("button", { name: "로그아웃" })).toBeVisible();
});
