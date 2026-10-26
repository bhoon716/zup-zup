import api from "@/shared/api/client";
import type { NotificationHistory, SliceResponse } from "@/shared/types/api";

export const getNotifications = (page = 0) =>
  api.get<SliceResponse<NotificationHistory>>("/api/v1/notifications/history", {
    params: { page },
  });
