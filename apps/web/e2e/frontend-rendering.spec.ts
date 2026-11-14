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

test("휠을 연속으로 내리고 올리면 수강신청 안내가 세 단계를 차례로 보여 준다", async ({ page, baseURL }) => {
  await page.setViewportSize({ width: 831, height: 923 });
  await page.emulateMedia({ reducedMotion: "no-preference" });
  await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
    ? route.continue() : route.abort());
  await page.goto("/");

  const pin = page.getByTestId("landing-walkthrough-pin");
  const header = page.getByRole("banner");
  const currentChapter = pin.locator('[aria-current="step"] h3');
  await expect(pin).toHaveCSS("position", "sticky");
  await page.evaluate(() => document.fonts.ready);
  const headerBounds = await header.boundingBox();
  if (!headerBounds) throw new Error("헤더를 찾지 못했습니다.");
  const headerBottom = headerBounds.y + headerBounds.height;
  await pin.evaluate((element, bottom) => {
    window.scrollBy(0, element.getBoundingClientRect().top - bottom - 8);
  }, headerBottom);

  for (const [deltaY, chapter] of [
    [120, "강의 검색"],
    [120, "빈자리 알림 등록"],
    [120, "빈자리 알림"],
    [-120, "빈자리 알림 등록"],
    [-120, "강의 검색"],
  ] as const) {
    const before = await page.evaluate(() => window.scrollY);
    await page.mouse.wheel(0, deltaY);
    await expect.poll(() => page.evaluate(() => window.scrollY)).not.toBe(before);
    await expect(currentChapter).toHaveText(chapter);
    await expect.poll(async () => (await pin.boundingBox())?.y).toBeCloseTo(headerBottom + 3, 0);
  }

  await page.mouse.wheel(0, 600);
  await expect(page.getByRole("heading", { name: "생생한 사용자 후기" })).toBeInViewport();
  await expect.poll(async () => (await pin.boundingBox())?.y).toBeLessThan(headerBottom);
});

for (const viewport of [
  { width: 390, height: 844 },
  { width: 831, height: 923 },
  { width: 831, height: 1320 },
  { width: 1440, height: 880 },
  { width: 1440, height: 1600 },
]) {
  test(`${viewport.width}×${viewport.height}에서 제목과 예시가 한 자리에 고정되고 단계가 바뀐다`, async ({ page, baseURL }) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: "no-preference" });
    await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
      ? route.continue() : route.abort());
    await page.goto("/");

    const pin = page.getByTestId("landing-walkthrough-pin");
    const stage = page.getByTestId("landing-walkthrough-stage");
    const title = pin.getByRole("heading", { level: 2 });
    const search = { heading: "강의 검색", marker: "여석 2" };
    const registration = { heading: "빈자리 알림 등록", marker: "등록 완료" };
    const notification = { heading: "빈자리 알림", marker: "수강신청 빈자리가 생겼어요!" };

    async function readChapter(progress: number, chapter: typeof search, previous: typeof search) {
      await pin.evaluate((element, fraction) => {
        const track = element.parentElement;
        if (!track) throw new Error("소개 영역을 찾지 못했습니다.");
        const { top, height } = track.getBoundingClientRect();
        const pinnedHeight = element.getBoundingClientRect().height;
        const stickyInset = Number.parseFloat(getComputedStyle(element).top);
        window.scrollBy(0, top - stickyInset + (height - pinnedHeight) * fraction);
      }, progress);

      const copy = stage.getByRole("listitem").filter({
        has: page.getByRole("heading", { name: chapter.heading, exact: true }),
      });
      await expect(copy).toHaveCSS("opacity", "1");
      await expect(copy.getByRole("heading")).toBeInViewport();
      const marker = copy.getByText(chapter.marker, { exact: true });
      expect(await marker.evaluate((element) => element.checkVisibility({ opacityProperty: true, visibilityProperty: true }))).toBe(true);
      await expect(marker).toBeInViewport();
      await expect(stage.getByText(previous.marker, { exact: true })).toBeHidden();
    }

    await expect(pin).toHaveCSS("position", "sticky");
    await expect(stage.getByRole("heading", { level: 3 })).toHaveText([
      search.heading,
      registration.heading,
      notification.heading,
    ]);
    await readChapter(0.18, search, registration);
    await expect(pin).toBeInViewport({ ratio: 1 });
    const pinnedStage = await stage.boundingBox();
    const pinnedTitle = await title.boundingBox();
    const header = await page.getByRole("banner").boundingBox();
    if (!pinnedStage || !pinnedTitle || !header) throw new Error("고정된 소개 화면이 보이지 않습니다.");
    expect(pinnedTitle.y).toBeGreaterThan(header.y + header.height);
    if (viewport.width === 831 && viewport.height === 923) {
      expect(pinnedTitle.y).toBeCloseTo(101, 0);
    }
    const expectPinnedStage = async () => {
      await expect(pin).toBeInViewport({ ratio: 1 });
      const currentStage = await stage.boundingBox();
      expect(currentStage?.y).toBeCloseTo(pinnedStage.y, 0);
      expect(currentStage?.height).toBeCloseTo(pinnedStage.height, 0);
      expect((await title.boundingBox())?.y).toBeCloseTo(pinnedTitle.y, 0);
    };

    await readChapter(0.5, registration, search);
    await expectPinnedStage();
    await readChapter(0.82, notification, registration);
    await expectPinnedStage();
    await readChapter(0.5, registration, notification);
    await expectPinnedStage();
    await readChapter(0.18, search, registration);
    await expectPinnedStage();
  });
}

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
