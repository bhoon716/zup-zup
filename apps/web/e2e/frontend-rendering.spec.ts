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

test("홈 소개는 클라이언트 자바스크립트 없이 읽을 수 있다", async ({ browser, baseURL }) => {
  const context = await browser.newContext({ javaScriptEnabled: false });
  try {
    await context.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
      ? route.continue() : route.abort());
    const page = await context.newPage();

    await page.goto(baseURL!);

    const landing = page.getByRole("main");
    await expect(landing.getByRole("heading", { level: 1, name: "수강신청 빈자리, 이제 알림으로 잡으세요." })).toBeVisible();
    await expect(landing.getByRole("heading", { level: 3 })).toHaveText([
      "강의 검색",
      "빈자리 알림 등록",
      "빈자리 알림",
    ]);
    await expect(landing.getByRole("heading", { name: "생생한 사용자 후기", exact: true })).toBeVisible();
    await expect(landing.getByRole("link", { name: "강의 먼저 둘러보기" })).toHaveAttribute("href", "/search");
  } finally {
    await context.close();
  }
});

const walkthroughSteps = [
  { heading: "강의 검색", marker: "여석 2" },
  { heading: "빈자리 알림 등록", marker: "등록 완료" },
  { heading: "빈자리 알림", marker: "수강신청 빈자리가 생겼어요!" },
];

test("1463×1313에서 스크롤 안내가 한 미리보기 안에서 전환된다", async ({ page, baseURL }) => {
  await page.setViewportSize({ width: 1463, height: 1313 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
  await page.goto("/");

  const preview = page.getByTestId("landing-walkthrough-sticky-preview");
  await expect(preview).toHaveCSS("position", "sticky");
});

for (const viewport of [
  { width: 390, height: 600 },
  { width: 390, height: 844 },
  { width: 831, height: 923 },
  { width: 1783, height: 1327 },
]) {
  test(`${viewport.width}×${viewport.height}에서 안내가 문서 순서대로 배치되고 후기로 이어진다`, async ({ page, baseURL }) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: "no-preference" });
    await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
      ? route.continue() : route.abort());
    await page.goto("/");
    await page.evaluate(() => document.fonts.ready);

    const stage = page.getByTestId("landing-walkthrough-stage");
    const chapters = stage.getByRole("listitem");
    const reviews = page.getByRole("heading", { name: "생생한 사용자 후기", exact: true });
    await expect(stage.getByRole("heading", { level: 3 })).toHaveText(walkthroughSteps.map(({ heading }) => heading));
    const reviewBounds = await reviews.boundingBox();
    if (!reviewBounds) throw new Error("후기의 위치를 찾지 못했습니다.");

    let previousBottom: number | undefined;
    for (const index of walkthroughSteps.keys()) {
      const bounds = await chapters.nth(index).boundingBox();
      if (!bounds) throw new Error("안내 단계의 위치를 찾지 못했습니다.");
      if (previousBottom !== undefined) {
        expect(bounds.y - previousBottom).toBeGreaterThanOrEqual(0);
        expect(bounds.y - previousBottom).toBeLessThanOrEqual(52);
      }
      previousBottom = bounds.y + bounds.height;
    }
    if (previousBottom === undefined) throw new Error("마지막 안내의 위치를 찾지 못했습니다.");
    const gapBeforeReviews = reviewBounds.y - previousBottom;
    expect(gapBeforeReviews).toBeGreaterThanOrEqual(0);
    expect(gapBeforeReviews).toBeLessThanOrEqual(64);

    for (const index of [0, 1, 2, 1, 0]) {
      const preview = chapters.nth(index).getByText(walkthroughSteps[index].marker, { exact: true });
      await preview.scrollIntoViewIfNeeded();
      await expect(preview).toBeInViewport({ ratio: 1 });
    }
  });
}

test("작은 스크롤에 미리보기가 서서히 나타나고 동작 줄이기에서는 효과를 끈다", async ({ page, baseURL }) => {
  await page.setViewportSize({ width: 831, height: 923 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
  await page.goto("/");
  await page.evaluate(() => document.fonts.ready);

  const preview = page.getByTestId("landing-walkthrough-preview").nth(1);
  await preview.evaluate((element) => {
    const bounds = element.getBoundingClientRect();
    window.scrollBy(0, bounds.top - window.innerHeight + bounds.height * 0.12);
  });
  const opacity = () => preview.evaluate((element) => Number(getComputedStyle(element).opacity));
  await expect.poll(opacity).toBeLessThan(0.96);
  const enteringOpacity = await opacity();
  expect(enteringOpacity).toBeGreaterThan(0.6);

  await page.mouse.wheel(0, 36);
  await expect.poll(opacity).toBeGreaterThan(enteringOpacity);
  const progressedOpacity = await opacity();
  expect(progressedOpacity).toBeLessThan(0.99);
  await expect(preview).toBeInViewport();

  await page.mouse.wheel(0, -36);
  await expect.poll(opacity).toBeLessThan(progressedOpacity);
  await page.mouse.wheel(0, 240);
  await expect.poll(opacity).toBe(1);

  await page.emulateMedia({ reducedMotion: "reduce" });
  await expect(preview).toHaveCSS("animation-name", "none");
  await expect(preview).toHaveCSS("opacity", "1");
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
