import api from "@/shared/api/client";
import type { CommonResponse, NotificationHistory, SliceResponse } from "@/shared/types/api";

export const getNotifications = (page = 0, size = 30) =>
  api.get<CommonResponse<SliceResponse<NotificationHistory>>>("/api/v1/notifications/history", {
    params: { page, size },
  });
