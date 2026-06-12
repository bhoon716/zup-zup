import { renderHook, waitFor } from "@testing-library/react";
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { createQueryWrapper, createTestQueryClient } from "@/test/query-client";
import * as dashboardApi from "@/widgets/home/api/dashboard.api";
import { useDashboardSnapshot } from "./useDashboard";

const mockSnapshot = {
  user: {
    id: 1,
    email: "user@example.com",
    name: "홍길동",
    role: "USER",
    emailEnabled: true,
    webPushEnabled: true,
    fcmEnabled: true,
    discordEnabled: false,
    onboardingCompleted: true,
  },
  notifications: [
    {
      id: 1,
      courseKey: "TEST-101",
      title: "공석 발생",
      message: "테스트 메시지",
      channel: "FCM",
      sentAt: "2024-01-01T12:00:00",
    },
  ],
  primaryTimetable: {
    id: 10,
    name: "대표 시간표",
    primary: true,
    courses: [],
    customSchedules: [],
    totalCredits: "0",
  },
  upcomingSchedules: [
    {
      id: 1,
      scheduleType: "종강",
      startDate: "2026-06-19",
      endDate: "2026-06-19",
      startTime: "18:00",
      endTime: "18:00",
      dDay: "D-1",
    },
  ],
  announcements: [
    {
      id: 1,
      title: "테스트 공지",
      previewContent: "미리보기",
      pinned: true,
      createdAt: "2024-01-01T12:00:00",
    },
  ],
};

const server = setupServer(
  http.get("*/api/v1/dashboard", () => {
    return HttpResponse.json({
      code: "SUCCESS",
      message: "ok",
      data: mockSnapshot,
    });
  }),
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe("useDashboardSnapshot hook", () => {
  it("대시보드 스냅샷을 한 번에 조회한다", async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useDashboardSnapshot(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.user.name).toBe("홍길동");
    expect(result.current.data?.notifications).toHaveLength(1);
    expect(result.current.data?.primaryTimetable?.name).toBe("대표 시간표");
    expect(result.current.data?.upcomingSchedules).toHaveLength(1);
    expect(result.current.data?.announcements).toHaveLength(1);
  });

  it("401 응답은 게스트 상태로 처리한다", async () => {
    const spy = vi.spyOn(dashboardApi, "getDashboardSnapshot").mockRejectedValueOnce({
      response: { status: 401 },
    } as never);

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useDashboardSnapshot(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
    spy.mockRestore();
  });

  it("게스트 스냅샷은 null로 처리한다", async () => {
    server.use(
      http.get("*/api/v1/dashboard", () => {
        return HttpResponse.json({
          code: "SUCCESS",
          message: "ok",
          data: {
            ...mockSnapshot,
            user: null,
          },
        });
      }),
    );

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    const { result } = renderHook(() => useDashboardSnapshot(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeNull();
  });

  it("비활성화되면 스냅샷을 조회하지 않는다", () => {
    const spy = vi.spyOn(dashboardApi, "getDashboardSnapshot");
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    renderHook(() => useDashboardSnapshot({ enabled: false }), { wrapper });

    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
  });
});
