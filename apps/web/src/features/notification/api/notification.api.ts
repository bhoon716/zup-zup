import api from "@/shared/api/client";
import type { CommonResponse, NotificationHistory } from "@/shared/types/api";

export const getNotifications = () =>
  api.get<CommonResponse<NotificationHistory[]>>("/api/v1/notifications/history");
