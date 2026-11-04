import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "@/shared/api/client";
import { searchCourses, getCourseHistory } from "@/features/course/api/course.api";
import { getReviews } from "@/features/review/api/review.api";
import { getAnnouncements } from "@/features/announcement/api/announcement.api";
import { getNotifications } from "@/features/notification/api/notification.api";
import { getMyFeedbacks, getFeedbacksForAdmin } from "@/features/feedback/api/feedback.api";
import { getAdminAnnouncements, getAdminDlqNotificationDeliveries } from "@/features/admin/api/admin.api";

vi.mock("@/shared/api/client", () => ({ default: { get: vi.fn(), post: vi.fn() } }));

const response = { content: [], number: 2, size: 30, totalElements: 91, totalPages: 4, last: false };

beforeEach(() => {
  vi.clearAllMocks();
  vi.mocked(api.get).mockResolvedValue({ data: response });
  vi.mocked(api.post).mockResolvedValue({ data: response });
});

describe("서버 고정 페이지 크기를 사용하는 API", () => {
  it.each([
    { name: "강의 검색", read: () => searchCourses({ name: "자료" }, 2), method: "post", path: "/api/v1/courses/search", params: { page: 2 }, body: { name: "자료" } },
    { name: "강의 이력", read: () => getCourseHistory("course:1", 2), method: "get", path: "/api/v1/courses/course%3A1/history", params: { page: 2 } },
    { name: "리뷰 정렬", read: () => getReviews("course:1", 2, "rating,desc"), method: "get", path: "/api/v1/courses/course%3A1/reviews", params: { page: 2, sort: "rating,desc" } },
    { name: "공지 검색", read: () => getAnnouncements({ keyword: " 점검 ", searchType: "TITLE", page: 2 }), method: "get", path: "/api/v1/announcements", params: { keyword: "점검", searchType: "TITLE", page: 2 } },
    { name: "알림 이력", read: async () => (await getNotifications(2)).data, method: "get", path: "/api/v1/notifications/history", params: { page: 2 } },
    { name: "내 문의", read: () => getMyFeedbacks(2), method: "get", path: "/api/v1/feedbacks/me", params: { page: 2 } },
    { name: "관리자 문의 필터", read: () => getFeedbacksForAdmin(2, "DELETED"), method: "get", path: "/api/v1/admin/feedbacks", params: { page: 2, deletion: "DELETED" } },
    { name: "관리자 공지", read: () => getAdminAnnouncements(2), method: "get", path: "/api/v1/admin/announcements", params: { page: 2 } },
    { name: "관리자 DLQ", read: () => getAdminDlqNotificationDeliveries(2), method: "get", path: "/api/v1/admin/notification-deliveries/dlq", params: { page: 2 } },
  ])("$name: 크기를 전달하지 않고 page·정렬·필터와 응답 메타데이터를 유지한다", async ({ read, method, path, params, ...entry }) => {
    expect(await read()).toEqual(response);
    if (method === "post") {
      expect(api.post).toHaveBeenCalledExactlyOnceWith(path, 'body' in entry ? entry.body : undefined, { params });
    } else {
      expect(api.get).toHaveBeenCalledExactlyOnceWith(path, { params });
    }
  });

  it.each([1, 100, -1])("공지 검색 객체에 남은 size=%s도 전달하지 않는다", async size => {
    const params = { page: 1, size };
    await getAnnouncements(params);
    expect(api.get).toHaveBeenCalledExactlyOnceWith("/api/v1/announcements", {
      params: { keyword: undefined, searchType: "TITLE_CONTENT", page: 1 },
    });
  });
});
