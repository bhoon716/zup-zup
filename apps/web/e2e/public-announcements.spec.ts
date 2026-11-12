import { expect, test } from "@playwright/test";

test("공개 공지사항을 페이지 번호로 탐색하고 검색 시 첫 페이지로 돌아간다", async ({ page, baseURL }) => {
  const notices = Array.from({ length: 23 }, (_, index) => ({
    id: index + 1,
    title: `공개 공지 ${index + 1}`,
    previewContent: index < 12 ? "수강신청 안내" : "서비스 점검 안내",
    pinned: index === 0,
    createdAt: "2026-11-13T06:00:00",
  }));
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") return route.fulfill({ status: 401, json: { code: "A001" } });
    if (pathname === "/api/v1/announcements") {
      const number = Number(searchParams.get("page") ?? "0");
      const keyword = searchParams.get("keyword") ?? "";
      const matches = notices.filter(({ previewContent }) => previewContent.includes(keyword));
      return route.fulfill({ json: {
        content: matches.slice(number * 10, (number + 1) * 10),
        number,
        totalPages: Math.ceil(matches.length / 10),
      } });
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/announcements");
  const list = page.getByRole("region", { name: "공지 목록" });
  const pager = page.getByRole("navigation", { name: "공지 페이지 이동" });
  await expect(list.getByRole("listitem")).toHaveCount(10);
  await expect(list.getByRole("listitem").first().getByRole("link", { name: /상단 고정 공개 공지 1 / })).toBeVisible();
  await expect(list.getByText("중요 공지", { exact: true })).toHaveCount(0);
  const third = pager.getByRole("button", { name: "3 페이지", exact: true });
  await third.click();
  await expect(third).toHaveAttribute("aria-current", "page");
  await expect(list.getByRole("listitem")).toHaveCount(3);
  await expect(list.getByRole("link", { name: /공개 공지 23/ })).toHaveAttribute("href", "/announcements/23");

  await page.getByRole("searchbox", { name: "공지 검색어" }).fill("수강신청");
  await expect(pager.getByRole("button", { name: "1 페이지", exact: true })).toHaveAttribute("aria-current", "page");
  await expect(list.getByRole("listitem")).toHaveCount(10);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
});
