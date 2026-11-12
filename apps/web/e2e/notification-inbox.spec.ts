import { expect, test } from "@playwright/test";
import type { InboxItem } from "../src/features/notification/api/inbox.api";
import type { AnnouncementDetailResponse } from "../src/shared/types/api";

const inboxPath = "/api/v1/notifications/inbox";
const detectedAt = "2026-11-12T09:00:00";
const readAt = "2026-11-12T10:00:00";
const literalName = "확률%_실습";
const similarName = "확률AB실습";

function notification(id: number, courseName = `빈자리 확인 강의 ${id}`): Extract<InboxItem, { type: "SEAT_OPENED" }> {
  return { id, type: "SEAT_OPENED", courseName, courseKey: `2026:U211600010:INBOX${id}:01`, availableSeats: 2, occurredAt: detectedAt, readAt: null };
}

test("작은 화면에서 알림을 읽고, 검색하고, 삭제한 뒤 최신 알림을 다시 불러온다", async ({ page, baseURL }) => {
  const newest = notification(31);
  const items = [newest, ...Array.from({ length: 28 }, (_, index) => notification(30 - index)), notification(2, similarName), notification(1, literalName)];
  let failDeletion = true;
  let failNextList = false;

  await page.setViewportSize({ width: 360, height: 780 });
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const request = route.request();
    const { pathname, searchParams } = new URL(request.url());

    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: { id: 1, name: "테스트 사용자", email: "inbox@example.invalid", role: "USER", onboardingCompleted: true } });
    }
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === "/api/v1/subscriptions") {
      return route.fulfill({ json: [{ id: 1, courseKey: newest.courseKey, courseName: "구독 중인 강의", professorName: "담당 교수" }] });
    }
    if (decodeURIComponent(pathname) === `/api/v1/courses/${newest.courseKey}`) {
      return route.fulfill({ json: { courseKey: newest.courseKey, name: newest.courseName, subjectCode: "INBOX31", classNumber: "01", professor: "담당 교수" } });
    }
    if (pathname.endsWith("/reviews")) return route.fulfill({ json: { content: [], number: 0, last: true } });
    if (pathname.endsWith("/emojis")) return route.fulfill({ json: [] });
    if (pathname === "/api/v1/notifications/history") {
      return route.fulfill({ json: { content: [{ id: 1, title: "외부 채널 발송", message: "메일로 발송한 알림", channel: "EMAIL", sentAt: detectedAt }], number: 0, last: true } });
    }

    if (pathname === inboxPath && request.method() === "GET") {
      if (failNextList) {
        failNextList = false;
        return route.fulfill({ status: 503, json: {} });
      }
      const number = Number(searchParams.get("page") ?? 0);
      const read = searchParams.get("read");
      const keyword = searchParams.get("keyword") ?? "";
      const sort = searchParams.get("sort");
      const matches = items.filter((item) => item.courseName.includes(keyword) && (
        read === null || (read === "UNREAD" && item.readAt === null) || (read === "READ" && item.readAt !== null)
      )).sort((a, b) => {
        if (sort === "UNREAD_FIRST") {
          const readOrder = Number(a.readAt !== null) - Number(b.readAt !== null);
          if (readOrder) return readOrder;
        }
        return sort === "OLDEST" ? a.id - b.id : b.id - a.id;
      });
      return route.fulfill({ json: { content: matches.slice(number * 30, (number + 1) * 30), number, totalPages: Math.ceil(matches.length / 30), totalElements: matches.length } });
    }

    if (pathname.startsWith(`${inboxPath}/`)) {
      if (request.headers()["x-csrf-token"] !== "test-csrf") return route.fulfill({ status: 403, json: {} });
      if (request.method() === "PATCH" && pathname === `${inboxPath}/${newest.id}/read`) {
        newest.readAt ??= readAt;
        return route.fulfill({ status: 204 });
      }
      if (request.method() === "PATCH" && pathname === `${inboxPath}/read-all`) {
        for (const item of items) item.readAt ??= readAt;
        return route.fulfill({ status: 204 });
      }
      if (request.method() === "DELETE" && pathname === `${inboxPath}/1`) {
        if (failDeletion) {
          failDeletion = false;
          return route.fulfill({ status: 503, json: {} });
        }
        const index = items.findIndex((item) => item.id === 1);
        if (index >= 0) items.splice(index, 1);
        return route.fulfill({ status: 204 });
      }
    }

    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto("/notifications");
  await expect(page.getByRole("heading", { name: "알림함", exact: true })).toBeVisible();
  const inboxTab = page.getByRole("tab", { name: "알림함", exact: true });
  const subscriptionsTab = page.getByRole("tab", { name: "구독 강의", exact: true });
  await inboxTab.press("End");
  await expect(subscriptionsTab).toBeFocused();
  await expect(subscriptionsTab).toHaveAttribute("aria-selected", "true");
  await expect(page.getByRole("tabpanel", { name: "구독 강의" }).getByRole("button", { name: "구독 중인 강의", exact: true })).toBeVisible();
  await subscriptionsTab.press("Home");
  await expect(inboxTab).toBeFocused();
  const list = page.getByRole("list", { name: "알림 목록" });
  const newestTitle = page.getByRole("button", { name: newest.courseName, exact: true });
  const filters = page.getByRole("group", { name: "알림 읽음 상태" });
  const pagination = page.getByRole("navigation", { name: "알림 목록 위 페이지 이동" });
  const bottomPagination = page.getByRole("navigation", { name: "알림 목록 아래 페이지 이동" });
  const pageOne = pagination.getByRole("button", { name: "1 페이지", exact: true });
  const pageTwo = pagination.getByRole("button", { name: "2 페이지", exact: true });
  await expect(pageOne).toHaveAttribute("aria-current", "page");
  await expect(pagination.getByText("알림 31개")).toBeVisible();
  await expect(bottomPagination.getByText("알림 31개")).toBeVisible();
  await expect(list.getByRole("listitem")).toHaveCount(30);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(360);
  const newestCard = list.getByRole("article").filter({ has: newestTitle });
  await expect(newestCard.getByText("빈자리가 2석 생겼어요.")).toBeVisible();
  await expect(newestCard.getByText("빈자리 발생", { exact: true })).toBeVisible();
  await expect(newestCard.getByText("2026년 11월 12일 09:00", { exact: true })).toHaveAttribute("datetime", detectedAt);

  await filters.getByRole("button", { name: "읽지 않음", exact: true }).click();
  await expect(newestTitle).toBeEnabled();
  await newestTitle.press("Enter");
  const detail = page.getByRole("dialog", { name: "강의 상세 정보" });
  await expect(detail.getByRole("heading", { name: newest.courseName, exact: true })).toBeVisible();
  await expect(detail.getByText("담당 교수", { exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await expect(detail).toHaveCount(0);
  await expect(newestTitle).toHaveCount(0);

  await page.reload();
  await filters.getByRole("button", { name: "읽음", exact: true }).click();
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(list.getByRole("article").filter({ has: newestTitle }).getByText("읽음", { exact: true })).toBeVisible();
  await filters.getByRole("button", { name: "전체", exact: true }).click();
  await bottomPagination.getByRole("button", { name: "2 페이지", exact: true }).click();
  await expect(pagination).toBeFocused();
  await expect(pageTwo).toHaveAttribute("aria-current", "page");
  await expect(list.getByRole("button", { name: literalName, exact: true })).toBeVisible();

  const keywordSearch = page.getByRole("searchbox", { name: "검색어" });
  await keywordSearch.fill(" %_ ");
  await keywordSearch.press("Enter");
  await expect(pagination).toHaveCount(0);
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(list.getByRole("button", { name: literalName, exact: true })).toBeVisible();
  await keywordSearch.fill("");
  await keywordSearch.press("Enter");
  await pageTwo.click();
  const sort = page.getByRole("combobox", { name: "알림 정렬" });
  await sort.click();
  await page.getByRole("option", { name: "오래된 순" }).click();
  await expect(pageOne).toHaveAttribute("aria-current", "page");
  await expect(list.getByRole("listitem").first().getByRole("button", { name: literalName, exact: true })).toBeVisible();
  await sort.click();
  await page.getByRole("option", { name: "읽지 않은 알림 먼저" }).click();
  await expect(newestTitle).toHaveCount(0);
  await sort.click();
  await page.getByRole("option", { name: "최신순" }).click();
  await expect(newestTitle).toBeVisible();
  await pageTwo.click();
  const deleteButton = list.getByRole("button", { name: `${literalName} 알림 삭제`, exact: true });
  await deleteButton.click();
  await expect(page.getByRole("alert").filter({ hasText: "알림을 삭제하지 못했습니다." })).toBeVisible();
  await expect(deleteButton).toBeVisible();
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(newestTitle).toBeVisible();
  await expect(pagination).toHaveCount(0);

  items.unshift(notification(32));
  await page.getByRole("button", { name: "새로고침", exact: true }).click();
  await pageTwo.click();
  await expect(list.getByRole("button", { name: similarName, exact: true })).toBeVisible();
  await page.reload();
  await pageTwo.click();
  await expect(list.getByRole("button", { name: similarName, exact: true })).toBeVisible();
  await expect(list.getByRole("button", { name: literalName, exact: true })).toHaveCount(0);

  await filters.getByRole("button", { name: "읽지 않음", exact: true }).click();
  await page.getByRole("button", { name: "모두 읽음 처리", exact: true }).click();
  await expect(page.getByText("조건에 맞는 알림이 없습니다.")).toBeVisible();
  await filters.getByRole("button", { name: "읽음", exact: true }).click();
  await expect(list.getByRole("listitem")).toHaveCount(30);
  await filters.getByRole("button", { name: "전체", exact: true }).click();
  const refreshButton = page.getByRole("button", { name: "새로고침", exact: true });
  await expect(refreshButton).toBeEnabled();

  const incoming = notification(33, "새로고침으로 받은 알림");
  const incomingTitle = list.getByRole("button", { name: incoming.courseName, exact: true });
  items.unshift(incoming);
  failNextList = true;
  await refreshButton.click();
  await expect(page.getByRole("alert").filter({ hasText: "알림을 불러오지 못했습니다." })).toBeVisible();
  await expect(newestTitle).toBeVisible();
  await expect(incomingTitle).toHaveCount(0);
  await page.getByRole("button", { name: "다시 불러오기", exact: true }).click();
  await expect(incomingTitle).toBeVisible();

  await page.getByRole("tab", { name: "발송 이력", exact: true }).click();
  await expect(page.getByText("외부 채널 발송", { exact: true })).toBeVisible();
  const whileClosed = notification(34, "알림함을 열어 받은 알림");
  items.unshift(whileClosed);
  await page.getByRole("tab", { name: "알림함", exact: true }).click();
  await expect(list.getByRole("button", { name: whileClosed.courseName, exact: true })).toBeVisible();
});

test("유형별 알림을 열면 읽음으로 바뀌고 관련 화면으로 연결된다", async ({ page, baseURL }) => {
  const messages = [
    { type: "FEEDBACK_REPLY", label: "문의 답변", title: "문의에 답변이 달렸습니다", message: "문의에 새 답변이 등록됐습니다. 문의 상세에서 답변을 확인해 주세요.", targetId: 21, href: "/feedback/21", available: true },
    { type: "ANNOUNCEMENT", label: "중요 공지", title: "확인할 수 없는 공지", message: "더 이상 확인할 수 없는 공지입니다.", targetId: 31, href: "/announcements/31", available: false },
    { type: "DELIVERY_FAILED", label: "발송 실패", title: "이메일 발송 실패", message: "알림 설정에서 이메일을 확인하세요.", targetId: null, href: "/settings", available: true },
    { type: "DEVICE_REREGISTRATION", label: "기기 재등록", title: "웹 푸시 기기 재등록", message: "이 기기를 다시 등록하세요.", targetId: null, href: "/settings", available: true },
  ] satisfies Array<Pick<Exclude<InboxItem, { type: "SEAT_OPENED" }>, "type" | "title" | "message" | "targetId" | "available"> & { label: string; href: string }>;
  const items: InboxItem[] = messages.map(({ type, title, message, targetId, available }, index) => ({
    type, title, message, targetId, available, id: index + 1, occurredAt: detectedAt, readAt: null,
  }));
  let announcementUnavailable = true;

  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const request = route.request();
    const { pathname, searchParams } = new URL(request.url());

    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: { id: 1, name: "테스트 사용자", email: "inbox@example.invalid", role: "USER", onboardingCompleted: true } });
    }
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === "/api/v1/subscriptions") return route.fulfill({ json: [] });
    if (pathname === inboxPath && request.method() === "GET") {
      const type = searchParams.get("type");
      const content = type ? items.filter((item) => item.type === type) : items;
      return route.fulfill({ json: { content, number: 0, totalPages: 1, totalElements: content.length } });
    }
    const item = items.find(({ id }) => pathname === `${inboxPath}/${id}/read`);
    if (item && request.method() === "PATCH" && request.headers()["x-csrf-token"] === "test-csrf") {
      item.readAt ??= readAt;
      return route.fulfill({ status: 204 });
    }
    if (pathname === "/api/v1/announcements/31" && !announcementUnavailable) {
      return route.fulfill({ json: {
        id: 31, title: "복원된 공지", content: "복원된 본문", published: true, pinned: false,
        createdAt: detectedAt, updatedAt: readAt, firstPublishedAt: detectedAt, deletedAt: null,
      } });
    }

    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto("/notifications");
  const filter = page.getByRole("combobox", { name: "알림 유형" });
  const list = page.getByRole("list", { name: "알림 목록" });
  await expect(list.getByRole("listitem")).toHaveCount(messages.length);
  await filter.press("Space");
  await expect(page.getByRole("listbox").getByRole("option"))
    .toHaveText(["모든 알림 유형", "빈자리", ...messages.map(({ label }) => label)]);
  await page.keyboard.press("Escape");

  for (const message of messages) {
    await filter.press("Space");
    await page.getByRole("option", { name: message.label, exact: true }).click();
    await expect(filter).toBeFocused();
    await expect(list.getByRole("listitem")).toHaveCount(1);
    if (message.type === "FEEDBACK_REPLY") {
      await expect(list.getByText("문의 상세에서 답변을 확인해 주세요.", { exact: true })).toBeVisible();
    }
    const open = list.getByRole("button", { name: message.title, exact: true });
    await open.click();
    const dialog = page.getByRole("dialog", { name: message.title });
    await expect(dialog.getByText(message.message ?? "", { exact: true })).toBeVisible();
    await expect(dialog.getByRole("link")).toHaveAttribute("href", message.href);
    await page.keyboard.press("Escape");
    await expect(open).toBeFocused();
    await expect(list.getByText("읽음", { exact: true })).toBeVisible();
  }

  await filter.click();
  await page.getByRole("option", { name: "중요 공지", exact: true }).click();
  await list.getByRole("button", { name: "확인할 수 없는 공지", exact: true }).click();
  await page.getByRole("link", { name: "공지 보기" }).click();
  await expect(page.getByRole("heading", { name: "더 이상 확인할 수 없는 공지입니다" })).toBeVisible();
  announcementUnavailable = false;
  await page.reload();
  await expect(page.getByRole("heading", { name: "복원된 공지" })).toBeVisible();
});

test("관리자는 처음 공개할 때 알림을 선택하고 삭제한 공지를 복원할 수 있다", async ({ page, baseURL }) => {
  const adminPath = "/api/v1/admin/announcements";
  let announcement: AnnouncementDetailResponse = {
    id: 1, title: "작성 중인 공지", content: "공지 본문", published: false, pinned: false,
    createdAt: detectedAt, updatedAt: detectedAt, firstPublishedAt: null, deletedAt: null,
  };

  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const request = route.request();
    const { pathname, searchParams } = new URL(request.url());
    const method = request.method();

    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: { id: 1, name: "테스트 관리자", email: "admin@example.invalid", role: "ADMIN", onboardingCompleted: true } });
    }
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === adminPath && method === "GET") {
      const page = Number(searchParams.get("page") ?? 0);
      const content = page === 0 ? [{ ...announcement, id: 2, title: "최근 공지" }] : [announcement];
      return route.fulfill({ json: { content, number: page, totalPages: 2 } });
    }
    if (request.headers()["x-csrf-token"] !== "test-csrf") return route.fulfill({ status: 403, json: {} });
    if (pathname === `${adminPath}/1` && method === "PUT") {
      announcement = { ...announcement, published: true, firstPublishedAt: readAt };
      return route.fulfill({ json: announcement });
    }
    if (pathname === `${adminPath}/1` && method === "DELETE") {
      announcement = { ...announcement, deletedAt: readAt };
      return route.fulfill({ status: 204 });
    }
    if (pathname === `${adminPath}/1/restore` && method === "PATCH") {
      announcement = { ...announcement, title: "복원된 최신 공지", deletedAt: null };
      return route.fulfill({ json: announcement });
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.goto("/admin/announcements");
  const notify = page.getByRole("switch", { name: "중요 공지 알림 보내기" });
  await expect(notify).not.toBeChecked();
  const draft = page.getByRole("button", { name: /작성 중인 공지/ });
  const pagination = page.getByRole("navigation", { name: "관리 공지 페이지" });
  await expect(draft).toHaveCount(0);
  await pagination.getByRole("button", { name: "다음 페이지" }).click();
  await draft.click();
  await expect(notify).toHaveCount(0);
  await page.getByRole("switch", { name: "공개", exact: true }).click();
  await expect(notify).not.toBeChecked();
  await notify.click();
  const publication = page.waitForRequest((request) => new URL(request.url()).pathname === `${adminPath}/1` && request.method() === "PUT");
  await page.getByRole("button", { name: "공지 수정" }).click();
  expect((await publication).postDataJSON()).toMatchObject({ published: true, notify: true });
  await expect(page.getByText("공지사항을 수정했습니다.")).toBeVisible();
  await draft.click();
  await expect(notify).toHaveCount(0);

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(draft.getByText("삭제됨", { exact: true })).toBeVisible();
  await draft.click();
  await expect(page.getByPlaceholder("공지 제목")).toBeDisabled();
  await page.getByRole("button", { name: "공지 복원" }).click();
  await expect(page.getByPlaceholder("공지 제목")).toHaveValue("복원된 최신 공지");
  await expect(page.getByRole("switch", { name: "공개", exact: true })).toBeChecked();
  await expect(notify).toHaveCount(0);
  await expect(page.getByText("삭제됨", { exact: true })).toHaveCount(0);
  await pagination.getByRole("button", { name: "이전 페이지" }).click();
  await expect(page.getByRole("button", { name: /최근 공지/ })).toBeEnabled();
});
