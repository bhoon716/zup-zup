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
    await expect(landing.getByTestId("landing-walkthrough-sticky-preview")).toBeHidden();
    await expect(landing.getByRole("heading", { name: "생생한 사용자 후기", exact: true })).toBeVisible();
    await expect(landing.getByRole("link", { name: "강의 먼저 둘러보기" })).toHaveAttribute("href", "/search");
  } finally {
    await context.close();
  }
});

const walkthroughSteps = [
  { heading: "강의 검색", marker: "COM2101-01" },
  { heading: "빈자리 알림 등록", marker: "구독이 완료되었습니다" },
  { heading: "빈자리 알림", marker: "보기" },
];

for (const viewport of [
  { width: 831, height: 923 },
  { width: 1463, height: 1313 },
  { width: 1783, height: 1327 },
  { width: 1463, height: 1700 },
]) {
  test(`${viewport.width}×${viewport.height}에서 설명과 미리보기가 같은 단계로 전환된다`, async ({ page, baseURL }) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: "no-preference" });
    await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
      ? route.continue() : route.abort());
    await page.goto("/");

    const stage = page.getByTestId("landing-walkthrough-stage");
    const preview = stage.getByTestId("landing-walkthrough-sticky-preview");
    const scenes = preview.getByTestId("landing-walkthrough-scene");
    const copies = stage.getByTestId("landing-walkthrough-copy");
    const current = stage.locator('[aria-current="step"]');
    await expect(preview).toHaveCSS("position", "sticky");
    await expect(stage.getByRole("heading", { level: 3 })).toHaveText([walkthroughSteps[0].heading]);
    for (const [index, { marker }] of walkthroughSteps.entries()) {
      await expect(scenes.nth(index).getByText(marker, { exact: true })).toHaveCount(1);
    }
    await page.evaluate(() => document.fonts.ready);

    const { start, travel, inset } = await preview.evaluate((element) => {
      const track = element.closest('[data-testid="landing-walkthrough-stage"]');
      if (!track) throw new Error("안내 영역을 찾지 못했습니다.");
      const bounds = track.getBoundingClientRect();
      const inset = parseFloat(getComputedStyle(element).top);
      const start = bounds.top + window.scrollY - inset;
      const release = bounds.bottom + window.scrollY - inset - element.getBoundingClientRect().height;
      const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
      return { start, travel: Math.min(release, maxScroll) - start, inset };
    });
    expect(travel).toBeGreaterThan(240);
    const scrollTo = (fraction: number) => page.evaluate(y => window.scrollTo(0, y), start + travel * fraction);
    const opacity = () => scenes.evaluateAll(elements => elements.map(element => Number(getComputedStyle(element).opacity)));
    const copyOffsets = () => copies.evaluateAll(elements => elements.map(element => parseFloat(getComputedStyle(element).translate)));

    await scrollTo(0);
    await expect.poll(opacity).toEqual([1, 0, 0]);
    await expect.poll(copyOffsets).toEqual([10, 0, 0]);
    await expect(current.getByRole("heading")).toHaveText("강의 검색");
    await page.mouse.wheel(0, 32);
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBeGreaterThan(start + 20);
    await expect.poll(opacity).toEqual([1, 0, 0]);

    await scrollTo(0.25);
    await expect.poll(async () => (await opacity())[1]).toBeGreaterThan(0.12);
    const [outgoing, incoming] = await opacity();
    expect(outgoing).toBeGreaterThan(0.12);
    expect(incoming).toBeLessThan(0.88);
    const [outgoingOffset, incomingOffset] = await copyOffsets();
    expect(outgoingOffset).toBeCloseTo(outgoing * 10, 3);
    expect(incomingOffset).toBeCloseTo(incoming * 10, 3);

    await page.mouse.wheel(0, 20);
    await expect.poll(async () => (await opacity())[1]).toBeGreaterThan(incoming + 0.04);
    const progressed = (await opacity())[1];
    expect(progressed).toBeLessThan(0.99);
    await expect.poll(async () => (await copyOffsets())[1]).toBeGreaterThan(incomingOffset);
    await page.mouse.wheel(0, -20);
    await expect.poll(async () => (await opacity())[1]).toBeLessThan(progressed - 0.04);

    await scrollTo(0.5);
    await expect.poll(opacity).toEqual([0, 1, 0]);
    await expect.poll(copyOffsets).toEqual([0, 10, 0]);
    await expect(current.getByRole("heading")).toHaveText("빈자리 알림 등록");
    await scrollTo(0.75);
    await expect.poll(async () => (await opacity())[2]).toBeGreaterThan(0.12);
    expect((await opacity())[1]).toBeGreaterThan(0.12);
    await scrollTo(0.9);
    await expect.poll(opacity).toEqual([0, 0, 1]);
    await expect.poll(copyOffsets).toEqual([0, 0, 10]);
    await expect(current.getByRole("heading")).toHaveText("빈자리 알림");
    await expect.poll(async () => (await preview.boundingBox())?.y).toBeCloseTo(inset, 0);
    await expect(preview).toBeInViewport({ ratio: 1 });

    const reviews = page.getByRole("heading", { name: "생생한 사용자 후기", exact: true });
    const rows = stage.getByRole("listitem");
    const lastRow = await rows.last().boundingBox();
    const reviewsBounds = await reviews.boundingBox();
    if (!lastRow || !reviewsBounds) throw new Error("마지막 안내나 후기의 위치를 찾지 못했습니다.");
    const gap = reviewsBounds.y - lastRow.y - lastRow.height;
    expect(gap).toBeGreaterThanOrEqual(0);
    expect(gap).toBeLessThanOrEqual(64);
    await reviews.scrollIntoViewIfNeeded();
    await expect(reviews).toBeInViewport();
  });
}

for (const scenario of [
  { width: 390, height: 844, reducedMotion: "no-preference" },
  { width: 831, height: 600, reducedMotion: "no-preference" },
  { width: 1463, height: 1920, reducedMotion: "no-preference" },
  { width: 1463, height: 1313, reducedMotion: "reduce" },
] as const) {
  test(`${scenario.width}×${scenario.height} ${scenario.reducedMotion}에서 세 안내가 차례로 읽힌다`, async ({ page, baseURL }) => {
    await page.setViewportSize({ width: scenario.width, height: scenario.height });
    await page.emulateMedia({ reducedMotion: scenario.reducedMotion });
    await page.route("**/*", (route) => new URL(route.request().url()).origin === new URL(baseURL!).origin
      ? route.continue() : route.abort());
    await page.goto("/");

    const stage = page.getByTestId("landing-walkthrough-stage");
    await expect(stage.getByTestId("landing-walkthrough-sticky-preview")).toBeHidden();
    const chapters = stage.getByRole("listitem");
    let previousBottom = 0;
    for (const [index, { heading }] of walkthroughSteps.entries()) {
      const chapter = chapters.nth(index);
      await expect(chapter.getByRole("heading", { name: heading, exact: true })).toBeVisible();
      const bounds = await chapter.boundingBox();
      if (!bounds) throw new Error("안내 단계의 위치를 찾지 못했습니다.");
      expect(bounds.y).toBeGreaterThan(previousBottom);
      previousBottom = bounds.y + bounds.height;
      if (scenario.reducedMotion === "reduce") {
        await expect(chapter.getByTestId("landing-walkthrough-preview")).toHaveCSS("animation-name", "none");
      }
    }
    for (const [index, { marker }] of walkthroughSteps.entries()) {
      const sample = chapters.nth(index).getByText(marker, { exact: true });
      await sample.scrollIntoViewIfNeeded();
      await expect(sample).toBeInViewport({ ratio: 1 });
    }
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
