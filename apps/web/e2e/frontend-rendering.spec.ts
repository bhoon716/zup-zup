import { expect, test } from "@playwright/test";

const semester = "U211600010";
const pageSize = 20;
const courseCount = 120;

function makeCourse(number: number) {
  return {
    courseKey: `2026:${semester}:ROW${number}:01`,
    name: `목록 검증 강의 ${number}`,
    subjectCode: `ROW${number}`,
    classNumber: "01",
    academicYear: "2026",
    semester,
    professor: "목록 검증 교수",
    capacity: 30,
    current: 20,
  };
}

test("비로그인 홈에 서비스 소개, 일정 카운트다운, 누적 통계를 표시한다", async ({ page, baseURL }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  const removedSectionRequests: string[] = [];
  const schedule = {
    id: 1,
    scheduleType: "겨울 계절학기 수강신청",
    startDate: "2026-11-16",
    endDate: "2026-11-19",
    dDay: "D-2",
  };
  await page.route("**/*", (route) => {
    const url = new URL(route.request().url());
    if (url.origin !== new URL(baseURL!).origin) return route.abort();
    if (url.pathname === "/api/v1/users/me") return route.fulfill({ status: 401, json: { code: "A001" } });
    if (url.pathname === "/api/v1/schedules/ddays") return route.fulfill({ json: [schedule] });
    if (url.pathname === "/api/v1/schedules" || url.pathname === "/api/v1/announcements") {
      removedSectionRequests.push(url.pathname);
      return route.abort();
    }
    return route.continue();
  });

  await page.goto("/");

  await expect(page.getByRole("heading", { level: 1 })).toHaveText(/^수강신청 빈자리,\s*이제 알림으로 잡으세요\.$/);
  await expect(page.getByRole("link", { name: "지금 시작하기" })).toHaveAttribute("href", "/login");
  await expect(page.getByRole("link", { name: "강의 먼저 둘러보기" })).toHaveAttribute("href", "/search");
  await expect(page.getByRole("heading", { name: "실시간 여석 알림" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "스마트 시간표 관리" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "정밀 강의 검색" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "생생한 사용자 후기" })).toBeVisible();
  await expect(page.getByText(`앞으로 ${schedule.scheduleType}까지`, { exact: true })).toBeVisible();
  await expect(page.getByText(schedule.dDay, { exact: true })).toBeVisible();

  const statistics = page.getByRole("region", { name: "서비스 이용 통계" });
  await expect(statistics.getByText("누적 사용자", { exact: true })).toBeVisible();
  await expect(statistics.getByText("700+명", { exact: true })).toBeVisible();
  await expect(statistics.getByText("보낸 빈자리 알림", { exact: true })).toBeVisible();
  await expect(statistics.getByText("7,000+개", { exact: true })).toBeVisible();
  const statisticsDate = statistics.getByText("2026년 11월 16일", { exact: true });
  await expect(statisticsDate).toBeVisible();
  await expect(statisticsDate).toHaveAttribute("datetime", "2026-11-16");
  await expect(page.getByRole("heading", { name: "주요 일정", exact: true })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "공지사항", exact: true })).toHaveCount(0);
  expect(removedSectionRequests).toEqual([]);
});

test("긴 검색 목록은 페이지를 추가해도 카드 수를 제한하고 상세를 닫으면 초점을 돌려준다", async ({ page, baseURL }) => {
  const requestedPages = new Set<number>();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.route("**/*", (route) => {
    const url = new URL(route.request().url());
    if (url.origin !== new URL(baseURL!).origin) return route.abort();
    if (!url.pathname.startsWith("/api/")) return route.continue();
    if (url.pathname === "/api/v1/users/me") return route.fulfill({ status: 401, json: { code: "A001" } });
    if (url.pathname === "/api/v1/courses/search-default-semester") return route.fulfill({ json: { semester } });
    if (url.pathname === "/api/v1/courses/search") {
      const number = Number(url.searchParams.get("page"));
      requestedPages.add(number);
      const content = Array.from({ length: pageSize }, (_, index) => makeCourse(number * pageSize + index + 1));
      return route.fulfill({ json: { content, last: (number + 1) * pageSize >= courseCount, number } });
    }
    const lastCourse = makeCourse(courseCount);
    const detailPath = `/api/v1/courses/${lastCourse.courseKey}`;
    const path = decodeURIComponent(url.pathname);
    if (path === detailPath) return route.fulfill({ json: lastCourse });
    if (path === `${detailPath}/emojis`) return route.fulfill({ json: [] });
    return route.fulfill({ status: 404, json: {} });
  });
  await page.addInitScript(() => localStorage.setItem("course-guide-modal-seen-v1", "seen"));

  await page.goto("/search");
  const firstTitle = page.getByRole("button", { name: makeCourse(1).name, exact: true });
  await expect(firstTitle).toBeVisible();

  const lastTitle = page.getByRole("button", { name: makeCourse(courseCount).name, exact: true });
  await expect(async () => {
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    expect(await lastTitle.isVisible()).toBe(true);
  }).toPass({ timeout: 10_000, intervals: [100, 200] });

  expect([...requestedPages]).toEqual([0, 1, 2, 3, 4, 5]);
  expect(await page.getByRole("article").count()).toBeLessThan(40);
  await expect(firstTitle).toHaveCount(0);

  await lastTitle.press("Space");
  const detail = page.getByRole("dialog", { name: "강의 상세 정보" });
  await expect(detail).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(detail).toHaveCount(0);
  await expect(lastTitle).toBeFocused();
});
