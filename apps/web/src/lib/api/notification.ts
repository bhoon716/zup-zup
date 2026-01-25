import api from "./index";
import type { CommonResponse, NotificationHistory, PageResponse } from "@/types/api";

export const getNotifications = (page = 0, size = 20) =>
  api.get<CommonResponse<PageResponse<NotificationHistory>>>("/notifications", {
    params: { page, size },
  });

export const markAsRead = (id: number) =>
  api.patch<CommonResponse<void>>(`/notifications/${id}/read`);

export const markAllAsRead = () =>
  api.patch<CommonResponse<void>>("/notifications/read-all");
