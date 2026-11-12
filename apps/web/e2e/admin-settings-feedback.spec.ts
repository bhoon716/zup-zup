import { expect, test } from "@playwright/test";

test("관리자 설정에서 행 아래에 일정을 편집하고 전체 관리에서 삭제된 일정을 복원할 수 있다", async ({ page, baseURL }) => {
  const schedules = [
    { id: 1, scheduleType: "지난 일정", startDate: "2026-11-12", endDate: "2026-11-13", endTime: "08:00", dDay: "D-Day", ddayEnabled: false, deletedAt: null },
    { id: 2, scheduleType: "진행 중인 일정", startDate: "2026-11-13", endDate: "2026-11-13", dDay: "D-Day", ddayEnabled: false, deletedAt: null },
    { id: 3, scheduleType: "삭제한 일정", startDate: "2026-11-14", endDate: "2026-11-14", dDay: "D-1", ddayEnabled: true, deletedAt: "2026-11-12T08:00:00" },
    { id: 4, scheduleType: "예정 일정", startDate: "2026-11-15", endDate: "2026-11-15", startTime: "09:00", endTime: "17:30", dDay: "D-2", ddayEnabled: false, deletedAt: null },
  ];
  let rejectedOnce = false;
  await page.clock.setFixedTime(new Date("2026-11-13T00:00:00Z"));
  await page.route("**/*", (route) => new URL(route.request().url()).origin === baseURL ? route.continue() : route.abort());
  await page.route("**/api/**", (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/users/me") {
      return route.fulfill({ json: {
        id: 1, name: "관리자", email: "admin@example.invalid", role: "ADMIN", onboardingCompleted: true,
      } });
    }
    if (pathname === "/api/v1/admin/dashboard") {
      return route.fulfill({ json: { crawlTarget: { year: "2026", semester: "U211600020" } } });
    }
    if (pathname === "/api/v1/admin/schedules") return route.fulfill({ json: schedules });
    if (pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "test-csrf" } });
    if (pathname === "/api/v1/admin/schedules/4" && route.request().method() === "PUT") {
      if (!rejectedOnce) {
        rejectedOnce = true;
        return route.fulfill({ status: 409, json: { message: "일정이 이미 변경되었습니다." } });
      }
      return route.fulfill({ json: schedules[3] });
    }
    return route.fulfill({ status: 404, json: {} });
  });

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/admin/settings");
  const crawlTarget = page.getByRole("region", { name: "크롤링 대상 제어" });
  await expect(crawlTarget.getByRole("button", { name: "이 설정으로 실행" })).toBeVisible();
  await expect(crawlTarget.getByRole("button", { name: "지정 값으로 실행" })).toBeVisible();
  await expect(page.getByText("학기 선택:", { exact: false })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "강의 데이터 재수집" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "대시보드 새로고침" })).toHaveCount(0);
  await expect(page.getByText("진행 중인 일정", { exact: true })).toBeVisible();
  await expect(page.getByText("지난 일정", { exact: true })).toHaveCount(0);
  await expect(page.getByText("삭제한 일정", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "자가 알림 테스트" })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);

  const rows = page.getByRole("region", { name: "수강 신청 일정 관리" }).getByRole("listitem");
  const firstEdit = page.getByRole("button", { name: "진행 중인 일정 수정" });
  const nextEdit = page.getByRole("button", { name: "예정 일정 수정" });
  await firstEdit.press("Enter");
  await expect(rows.first().getByRole("form")).toBeVisible();

  await nextEdit.press("Enter");
  const editForm = rows.last().getByRole("form", { name: "예정 일정 수정" });
  await expect(editForm).toBeVisible();
  await expect(rows.first().getByRole("form")).toHaveCount(0);
  await expect(editForm.getByLabel("일정 구분")).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);

  await editForm.getByLabel("종료 시간", { exact: true }).fill("18:00");
  await editForm.getByRole("button", { name: "저장" }).click();
  await expect(editForm.getByRole("alert")).toHaveText("일정이 이미 변경되었습니다.");
  await expect(editForm.getByLabel("종료 시간", { exact: true })).toHaveValue("18:00");

  await editForm.getByRole("button", { name: "취소", exact: true }).click();
  await expect(editForm).toHaveCount(0);
  await expect(nextEdit).toBeFocused();

  await nextEdit.press("Enter");
  await editForm.getByRole("button", { name: "저장" }).click();
  await expect(editForm).toHaveCount(0);
  await expect(nextEdit).toBeFocused();

  await page.getByRole("button", { name: "일정 추가" }).press("Enter");
  const createForm = page.getByRole("form", { name: "새 일정 추가" });
  await expect(createForm.getByRole("combobox", { name: "일정 구분" })).toBeFocused();
  expect((await createForm.boundingBox())?.y).toBeLessThan((await rows.first().boundingBox())?.y ?? 0);

  await page.getByRole("link", { name: "전체 일정 관리" }).click();
  await expect(page.getByText("지난 일정", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "삭제한 일정 복원" })).toBeEnabled();
});
