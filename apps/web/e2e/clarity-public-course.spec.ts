import { expect, test } from "@playwright/test";
import type { Course } from "../src/shared/types/api";

const course: Course = {
  courseKey: "2026:U211600010:PUBLIC:01", subjectCode: "PUBLIC", classNumber: "01", name: "공개 강의", disclosure: "공개", credits: "3",
  professor: "담당 교수", classroom: "공학관 101", classTime: "월 6", academicYear: "2026",
  semester: "U211600010", capacity: 30, current: 20, available: 10, isSubscribable: true,
  schedules: [{ dayOfWeek: "월", startTime: "09:00", endTime: "10:00" }],
};
const unmasked = "[data-clarity-unmask]";

test.beforeEach(async ({ page, context, baseURL }) => {
  await context.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin ? route.continue() : route.abort());
  await page.route("**/api/**", route => {
    const path = decodeURIComponent(new URL(route.request().url()).pathname);
    const method = route.request().method();
    const fulfill = (data: unknown) => route.fulfill({ json: data });
    if (method !== "GET" && !(method === "POST" && path === "/api/v1/courses/search")) {
      return route.fulfill({ status: 500, json: { code: "WRITE_FORBIDDEN_IN_MASKING_TEST" } });
    }
    if (path === "/api/v1/users/me") return fulfill({ id: 1, name: "가상 사용자", email: "mask-test@example.invalid", role: "USER", onboardingCompleted: true });
    if (path === "/api/auth/csrf") return fulfill({ token: "local-test-csrf" });
    if (path === "/api/v1/courses/search-default-semester") return fulfill({ semester: course.semester });
    if (path === "/api/v1/courses/search") return fulfill({ content: [course, { ...course, courseKey: "PRIVATE", name: "비공개 강의", disclosure: "비공개" }], last: true, number: 0, size: 30 });
    if (path === "/api/v1/subscriptions") return fulfill([{ id: 1, courseKey: course.courseKey, courseName: course.name, professorName: course.professor }]);
    if (path === "/api/v1/wishlist") return fulfill([]);
    if (path === "/api/v1/notifications/history" || path.endsWith("/reviews")) return fulfill({ content: [], last: true, number: 0 });
    if (path.endsWith("/emojis")) return fulfill([]);
    if (path === "/api/v1/timetables") return fulfill([{ id: 1, name: "개인 시간표", primary: true }]);
    if (path === "/api/v1/timetables/1" || path === "/api/v1/timetables/primary") return fulfill({ id: 1, name: "개인 시간표", primary: true, courses: [course], customSchedules: [], totalCredits: "3" });
    if (path === `/api/v1/courses/${course.courseKey}`) return fulfill(course);
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });
  await page.addInitScript(() => localStorage.setItem("course-guide-modal-seen-v1", "seen"));
});

test("공개 검색의 허용 텍스트와 popup만 예외이며 개인 필터·검색어는 보호한다", async ({ page }) => {
  await page.goto("/search");
  await expect(page.locator("body")).toHaveAttribute("data-clarity-mask", "true");
  const card = page.locator("article").filter({ has: page.getByRole("heading", { name: course.name, exact: true }) });
  await expect(card.locator(unmasked)).toHaveCount(3);
  await expect(page.locator("article").filter({ hasText: "비공개 강의" }).locator(unmasked)).toHaveCount(0);
  await page.getByRole("button", { name: course.name, exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "강의 상세 정보" });
  await expect(dialog.locator(unmasked)).toHaveCount(3);
  await expect(dialog).not.toHaveAttribute("data-clarity-unmask");
  await page.keyboard.press("Escape");
  await page.locator("input:visible").first().fill("비공개 검색어");
  await page.getByRole("button", { name: "검색", exact: true }).click();
  const searchHeading = page.getByRole("heading", { name: '"비공개 검색어"', exact: true });
  await expect(searchHeading).toBeVisible();
  await expect(searchHeading.locator("xpath=ancestor-or-self::*[@data-clarity-unmask]")).toHaveCount(0);
  await page.getByRole("switch", { name: "찜한 강의만 보기" }).click();
  await page.getByRole("button", { name: "검색", exact: true }).click();
  await expect(card.locator(unmasked)).toHaveCount(0);
  await page.getByRole("button", { name: course.name, exact: true }).click();
  await expect(dialog.locator(unmasked)).toHaveCount(0);
  for (const key of [course.courseKey, encodeURIComponent(course.courseKey)]) {
    await page.goto(`/courses/${key}`);
    await expect(page.getByRole("heading", { name: course.name, exact: true })).toHaveAttribute("data-clarity-unmask", "true");
    await expect(page.locator(unmasked)).toHaveCount(3);
  }
});

for (const path of ["/notifications", "/timetable"]) {
  test(`${path}의 공개 강의도 개인 맥락과 상세 popup은 보호한다`, async ({ page }) => {
    await page.goto(path);
    const privateCourse = page.locator("aside").getByText(course.name, { exact: true });
    await expect(privateCourse).toBeVisible();
    await expect(page.locator("body")).toHaveAttribute("data-clarity-mask", "true");
    await expect(page.locator(unmasked)).toHaveCount(0);
    await privateCourse.click();
    const dialog = page.getByRole("dialog", { name: "강의 상세 정보" });
    await expect(dialog.getByRole("heading", { name: course.name, exact: true })).toBeVisible();
    await expect(dialog.locator(unmasked)).toHaveCount(0);
  });
}
