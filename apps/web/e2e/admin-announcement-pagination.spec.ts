import { expect, test } from "@playwright/test";

test("관리 공지 목록에서 페이지 번호를 눌러 다른 공지로 이동한다", async ({ page, baseURL }) => {
  const notices = Array.from({ length: 13 }, (_, index) => ({
    id: index + 1,
    title: `관리 공지 ${index + 1}`,
    content: "공지 본문",
    published: true,
    pinned: false,
    createdAt: "2026-11-13T06:00:00",
    updatedAt: "2026-11-13T06:00:00",
    firstPublishedAt: "2026-11-13T06:00:00",
    deletedAt: null,
  }));
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: {
        id: 1, name: "관리자", email: "admin@example.invalid", role: "ADMIN", onboardingCompleted: true,
      } });
    }
    if (pathname === "/api/v1/admin/announcements") {
      const number = Number(searchParams.get("page") ?? "0");
      return route.fulfill({ json: {
        content: notices.slice(number * 5, (number + 1) * 5), number, totalPages: 3, totalElements: notices.length,
      } });
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto("/admin/announcements");
  const pager = page.getByRole("navigation", { name: "관리 공지 페이지" });
  await expect(pager.getByRole("button", { name: "1 페이지", exact: true })).toHaveAttribute("aria-current", "page");
  await expect(page.getByRole("button", { name: /^관리 공지 \d/ })).toHaveCount(5);

  const third = pager.getByRole("button", { name: "3 페이지", exact: true });
  await third.click();
  await expect(third).toHaveAttribute("aria-current", "page");
  await expect(page.getByRole("button", { name: /^관리 공지 \d/ })).toHaveCount(3);
  await page.getByRole("button", { name: /^관리 공지 13/ }).click();
  await expect(page.getByPlaceholder("공지 제목")).toHaveValue("관리 공지 13");
});
