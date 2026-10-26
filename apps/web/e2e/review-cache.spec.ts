import { expect, test } from "@playwright/test";

import type { Course, ReviewResponse } from "../src/shared/types/api";

const response = (data: unknown) => (data);
const course: Course = {
  courseKey: "2026:U211600010:CSE101:01", subjectCode: "CSE101", name: "자료구조",
  professor: "김교수", stdtrNo: "학수-01", classNumber: "01", academicYear: "2026",
  semester: "U211600010", capacity: 30, current: 20, available: 10, classTime: "월 1,2",
  averageRating: 4, reviewCount: 10, isReviewed: false, isSubscribable: true,
};
const otherSection: Course = {
  ...course, courseKey: "2026:U211600010:CSE101:02", stdtrNo: "학수-02", classNumber: "02",
  capacity: 50, current: 35, available: 15, classTime: "화 3,4",
};

for (const action of ["create", "update"] as const) {
  test(`review ${action} preserves another section and its wishlist target`, async ({ page, context }) => {
    let review: ReviewResponse | null = action === "update" ? {
      id: 11, courseKey: course.courseKey, rating: 3, likeCount: 0, dislikeCount: 0,
      isMine: true, createdAt: "2026-10-26T10:00:00", updatedAt: "2026-10-26T10:00:00",
    } : null;
    const wishlistTargets: string[] = [];
    const reviewWrites: Array<{ method: string; path: string; rating: number }> = [];
    const unexpectedApiRequests: string[] = [];
    const writeCsrfTokens: string[] = [];
    const searchCsrfTokens: Array<string | undefined> = [];
    const user = { id: 1, email: "reviewer@example.com", name: "수강생", role: "USER",
      emailEnabled: false, webPushEnabled: false, fcmEnabled: false, discordEnabled: false,
      onboardingCompleted: true };


    await context.route("**/*", (route) => new URL(route.request().url()).origin === "http://127.0.0.1:3100"
      ? route.continue() : route.abort());
    await page.route("**/api/**", (route) => {
      const path = decodeURIComponent(new URL(route.request().url()).pathname);
      const method = route.request().method();
      const fulfill = (data: unknown) => route.fulfill({ json: response(data) });
      if (path === "/api/auth/csrf") return fulfill({ token: "review-csrf" });
      if (["POST", "PUT", "PATCH", "DELETE"].includes(method) && path !== "/api/v1/courses/search") {
        writeCsrfTokens.push(route.request().headers()["x-csrf-token"]);
      }
      if (path === "/api/v1/users/me") return fulfill(user);
      if (path === "/api/v1/courses/search-default-semester") return fulfill({ semester: course.semester });
      if (path === "/api/v1/courses/search") {
        searchCsrfTokens.push(route.request().headers()["x-csrf-token"]);
        return fulfill({ content: [course, otherSection], last: true, number: 0 });
      }
      if (path === "/api/v1/subscriptions" || path === "/api/v1/timetables") return fulfill([]);
      if (path === "/api/v1/wishlist") {
        return fulfill(wishlistTargets.map((courseKey) => ({ id: 1, courseKey })));
      }
      if (path.startsWith("/api/v1/wishlist/") && method === "POST") {
        wishlistTargets.push(path.slice("/api/v1/wishlist/".length));
        return fulfill({ isWished: true });
      }
      if (path.endsWith("/emojis")) return fulfill([]);
      if (path.endsWith("/reviews") && method === "GET") {
        return fulfill({ content: review ? [review] : [], last: true, number: 0 });
      }
      if ((path === `/api/v1/courses/${course.courseKey}/reviews` && method === "POST")
          || (path === "/api/v1/courses/reviews/11" && method === "PUT")) {
        const { rating } = route.request().postDataJSON() as { rating: number };
        reviewWrites.push({ method, path, rating });
        review = { id: 11, courseKey: course.courseKey, rating, likeCount: 0, dislikeCount: 0,
          isMine: true, createdAt: "2026-10-26T10:00:00", updatedAt: "2026-10-26T10:00:00" };
        return fulfill(review);
      }
      const detail = [course, otherSection].find((item) => path === `/api/v1/courses/${item.courseKey}`);
      if (detail) return fulfill(detail);
      unexpectedApiRequests.push(`${method} ${path}`);
      return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API", message: path } });
    });
    await page.addInitScript(() => localStorage.setItem("course-guide-modal-seen-v1", "seen"));
    await page.goto("/search");
    await expect(page.getByRole("group", { name: "사용자 메뉴", exact: true }).getByText("수강생 님", { exact: true })).toBeVisible();

    const otherCard = page.locator("article").filter({ hasText: "학수-02" });
    const dialog = page.getByRole("dialog", { name: "강의 상세 정보" });
    const submitLabel = action === "create" ? "등록" : "수정";

    await otherCard.getByRole("button", { name: "자료구조", exact: true }).click();
    await expect(dialog.getByRole("button", { name: submitLabel, exact: true })).toBeEnabled();
    await dialog.getByRole("button", { name: "닫기", exact: true }).click();
    await page.locator("article").filter({ hasText: "학수-01" })
      .getByRole("button", { name: "자료구조", exact: true }).click();
    await expect(dialog.getByRole("button", { name: submitLabel, exact: true })).toBeEnabled();
    await dialog.getByRole("button", { name: "5점", exact: true }).click();
    await dialog.getByRole("button", { name: submitLabel, exact: true }).click();
    await expect(dialog.getByText(action === "create" ? "4.1점" : "4.2점", { exact: true })).toBeVisible();
    await dialog.getByRole("button", { name: "닫기", exact: true }).click();

    await expect(otherCard).toContainText("화 3,4");
    await expect(otherCard.getByText("35 / 50", { exact: true }).filter({ visible: true })).toBeVisible();
    await expect(otherCard).toContainText(action === "create" ? "4.1" : "4.2");
    await otherCard.getByRole("button", { name: "자료구조 관심 강의 추가", exact: true }).click();
    await expect.poll(() => wishlistTargets).toEqual([otherSection.courseKey]);
    expect(reviewWrites).toEqual([{
      method: action === "create" ? "POST" : "PUT",
      path: action === "create" ? `/api/v1/courses/${course.courseKey}/reviews` : "/api/v1/courses/reviews/11",
      rating: 5,
    }]);
    expect(unexpectedApiRequests).toEqual([]);
    expect(writeCsrfTokens).toEqual(Array(2).fill("review-csrf"));
    expect(searchCsrfTokens.length).toBeGreaterThan(0);
    expect(searchCsrfTokens.every(token => token === undefined)).toBe(true);
  });
}
