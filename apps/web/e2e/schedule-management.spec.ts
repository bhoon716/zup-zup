import { expect, test } from "@playwright/test";
import type { ScheduleRequest, ScheduleResponse } from "../src/shared/types/api";

test("일정 하나로 대표 지정·수정·삭제·복원을 관리한다", async ({ page, context }) => {
  const schedules: ScheduleResponse[] = [];
  const writes: Array<{ method: string; path: string; body?: ScheduleRequest }> = [];
  const unexpected: string[] = [];
  const admin = { id: 1, email: "admin@example.com", name: "관리자", role: "ADMIN", emailEnabled: false,
    webPushEnabled: false, fcmEnabled: false, discordEnabled: false, onboardingCompleted: true };
  await context.route("**/*", route => new URL(route.request().url()).origin === "http://127.0.0.1:3100"
    ? route.continue() : route.abort());
  await page.route("**/api/**", route => {
    const path = new URL(route.request().url()).pathname;
    const method = route.request().method();
    if (path === "/api/v1/users/me") return route.fulfill({ json: admin });
    if (path === "/api/v1/admin/schedules" && method === "GET") return route.fulfill({ json: schedules });
    if (path === "/api/v1/admin/schedules" && method === "POST") {
      const body = route.request().postDataJSON() as ScheduleRequest;
      writes.push({ method, path, body });
      const created = { ...body, id: 1, ddayEnabled: body.ddayEnabled ?? false, deletedAt: null, dDay: "D-2" };
      schedules.push(created);
      return route.fulfill({ json: created });
    }
    if (path === "/api/v1/admin/schedules/1" && method === "PUT") {
      const body = route.request().postDataJSON() as ScheduleRequest;
      writes.push({ method, path, body });
      Object.assign(schedules[0], body);
      return route.fulfill({ json: schedules[0] });
    }
    if (path === "/api/v1/admin/schedules/1" && method === "DELETE") {
      writes.push({ method, path });
      schedules[0].deletedAt = "2026-10-26T10:00:00";
      return route.fulfill({ status: 204 });
    }
    if (path === "/api/v1/admin/schedules/1/restore" && method === "PATCH") {
      writes.push({ method, path });
      schedules[0].deletedAt = null;
      return route.fulfill({ json: schedules[0] });
    }
    unexpected.push(`${method} ${path}`);
    return route.fulfill({ status: 500, json: { code: "UNEXPECTED_TEST_API", message: path } });
  });
  await context.addCookies([{ name: "is_logged_in", value: "true", url: "http://127.0.0.1:3100" }]);
  await page.goto("/admin/schedules");
  await page.getByRole("button", { name: "일정 추가" }).click();
  await page.getByRole("combobox", { name: "일정 구분" }).click();
  await page.getByRole("option", { name: "기타", exact: true }).click();
  await page.getByLabel("일정 이름").fill("대표 학사 일정");
  await page.getByLabel("시작일").fill("2026-10-28");
  await page.getByLabel("종료일").fill("2026-10-29");
  await page.getByRole("checkbox", { name: "대표 D-day로 표시" }).check();
  await page.getByRole("button", { name: "저장", exact: true }).click();
  await expect(page.getByText("대표 D-day", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "대표 학사 일정 수정" }).click();
  await expect(page.getByRole("checkbox", { name: "대표 D-day로 표시" })).toBeChecked();
  await page.getByLabel("시작일").fill("2026-10-27");
  await page.getByRole("button", { name: "저장", exact: true }).click();
  await expect(page.getByRole("checkbox", { name: "대표 D-day로 표시" })).toHaveCount(0);
  await expect(page.getByText(/2026-10-27/)).toBeVisible();

  page.once("dialog", dialog => dialog.accept());
  await page.getByRole("button", { name: "대표 학사 일정 삭제" }).click();
  await expect(page.getByText("삭제됨", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "대표 학사 일정 수정" })).toHaveCount(0);
  await page.getByRole("button", { name: "대표 학사 일정 복원" }).click();
  await expect(page.getByText("삭제됨", { exact: true })).toHaveCount(0);
  await expect(page.getByText("대표 D-day", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "대표 학사 일정 수정" })).toBeVisible();
  expect(writes).toEqual([
    { method: "POST", path: "/api/v1/admin/schedules", body: { scheduleType: "대표 학사 일정", startDate: "2026-10-28", endDate: "2026-10-29", ddayEnabled: true } },
    { method: "PUT", path: "/api/v1/admin/schedules/1", body: { scheduleType: "대표 학사 일정", startDate: "2026-10-27", endDate: "2026-10-29", ddayEnabled: true } },
    { method: "DELETE", path: "/api/v1/admin/schedules/1" },
    { method: "PATCH", path: "/api/v1/admin/schedules/1/restore" },
  ]);
  expect(unexpected).toEqual([]);
});
