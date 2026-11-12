import { expect, test } from "@playwright/test";

const courseKey = "2026:U211600010:QUERY:01";
const coursePath = `/api/v1/courses/${courseKey}`;
const course = {
  courseKey, subjectCode: "QUERY", classNumber: "01", name: "조회 횟수 강의",
  academicYear: "2026", semester: "U211600010", professor: "김교수", capacity: 30, current: 20,
};
const user = { id: 1, name: "수강생", email: "query@example.invalid", role: "USER", onboardingCompleted: true };
const timetable = { id: 1, name: "개인 시간표", primary: true, courses: [], customSchedules: [], totalCredits: "0" };

test.beforeEach(async ({ context, baseURL }) => {
  await context.route("**/*", route => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
});

test("게스트 강의 상세는 내 별점 목록 없이 공개 이모지를 표시한다", async ({ page }) => {
  let reviewReads = 0;
  await page.route("**/api/**", route => {
    const path = decodeURIComponent(new URL(route.request().url()).pathname);
    if (path === "/api/v1/users/me") return route.fulfill({ status: 401, json: { code: "A001" } });
    if (path === coursePath) return route.fulfill({ json: course });
    if (path === `${coursePath}/emojis`) return route.fulfill({ json: [{ emoji: "👍", count: 1, isMine: false }] });
    if (path === `${coursePath}/reviews`) {
      reviewReads += 1;
      return route.fulfill({ json: { content: [], last: true, number: 0 } });
    }
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });

  await page.goto(`/courses/${encodeURIComponent(courseKey)}`);
  await expect(page.getByText("리뷰를 작성하려면 로그인이 필요합니다.")).toBeVisible();
  await expect(page.getByRole("button", { name: "👍 1개" })).toBeVisible();
  expect(reviewReads).toBe(0);
});

test("검색에서는 시간표 메뉴를 열 때만 조회하고 다시 열면 같은 목록을 사용한다", async ({ page }) => {
  let timetableReads = 0;
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") return route.fulfill({ json: user });
    if (path === "/api/v1/courses/search-default-semester") return route.fulfill({ json: { semester: course.semester } });
    if (path === "/api/v1/courses/search") return route.fulfill({ json: { content: [course], last: true, number: 0 } });
    if (path === "/api/v1/subscriptions" || path === "/api/v1/wishlist") return route.fulfill({ json: [] });
    if (path === "/api/v1/timetables") {
      timetableReads += 1;
      return route.fulfill({ json: [timetable] });
    }
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });
  await page.addInitScript(() => localStorage.setItem("course-guide-modal-seen-v1", "seen"));

  await page.goto("/search");
  await expect(page.getByRole("group", { name: "사용자 메뉴", exact: true }).getByText("수강생 님")).toBeVisible();
  await expect(page.getByRole("button", { name: course.name, exact: true })).toBeVisible();
  const initialReads = timetableReads;

  const addMenu = page.getByRole("button", { name: `${course.name}를 시간표에 추가` });
  const importMenu = page.getByRole("button", { name: "내 시간표에서 공강 불러오기" });
  for (const menu of [addMenu, importMenu, importMenu]) {
    await menu.click();
    await expect(page.getByRole("menuitem", { name: timetable.name })).toBeVisible();
    await page.keyboard.press("Escape");
  }

  expect({ initialReads, totalReads: timetableReads }).toEqual({ initialReads: 0, totalReads: 1 });
});

test("설정 첫 진입은 인증 확인과 프로필 초기화에 같은 요청을 사용한다", async ({ page }) => {
  let profileReads = 0;
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") {
      profileReads += 1;
      return route.fulfill({ json: { ...user, notificationEmail: user.email, emailEnabled: true } });
    }
    if (path === "/api/v1/subscriptions") return route.fulfill({ json: [] });
    if (path === "/api/v1/users/devices") return route.fulfill({ json: [] });
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });

  await page.goto("/settings");
  await expect(page.locator('input[name="notificationEmail"]')).toHaveValue(user.email);
  expect(profileReads).toBe(1);
});

test("관리자 DLQ 항목 선택 시 현재 상세를 한 번 조회한다", async ({ page }) => {
  const delivery = {
    id: 41, outboxId: 11, courseKey, courseName: "요청 수 발송", channel: "EMAIL", status: "DLQ",
    attempts: 5, lastError: "N001", lastErrorReason: "수신 실패", deadLetteredAt: "2026-11-11T00:00:00",
    idempotencyKeyRetained: true,
  };
  let detailReads = 0;
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") return route.fulfill({ json: { ...user, role: "ADMIN" } });
    if (path === "/api/v1/admin/notification-deliveries/dlq") {
      return route.fulfill({ json: { content: [delivery], totalElements: 1, totalPages: 1 } });
    }
    if (path === "/api/v1/admin/notification-deliveries/41") {
      detailReads += 1;
      return route.fulfill({ json: delivery });
    }
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });

  await page.goto("/admin/notification-deliveries");
  await page.getByRole("button", { name: /요청 수 발송/ }).click();
  await expect(page.getByRole("button", { name: /재처리/ })).toBeEnabled();
  expect(detailReads).toBe(1);
});

test("시간표 첫 화면은 찜 탭을 열기 전까지 찜 목록을 조회하지 않는다", async ({ page }) => {
  let wishlistReads = 0;
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/users/me") return route.fulfill({ json: user });
    if (path === "/api/v1/timetables") return route.fulfill({ json: [timetable] });
    if (path === "/api/v1/timetables/1") return route.fulfill({ json: timetable });
    if (path === "/api/v1/wishlist") {
      wishlistReads += 1;
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API" } });
  });

  const timetableReady = page.waitForResponse(response =>
    new URL(response.url()).pathname === "/api/v1/timetables/1" && response.ok());
  await page.goto("/timetable");
  await timetableReady;
  await expect(page.getByText("시간표 강의 0건")).toBeVisible();
  const initialReads = wishlistReads;
  await page.getByRole("button", { name: /찜/ }).click();
  await expect(page.getByText("찜한 강의 0건")).toBeVisible();
  await expect.poll(() => wishlistReads).toBe(1);
  expect(initialReads).toBe(0);
});
