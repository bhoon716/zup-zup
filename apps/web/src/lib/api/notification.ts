import api from "./index";
import type { CommonResponse, NotificationHistory } from "@/types/api";

export const getNotifications = () =>
  api.get<CommonResponse<NotificationHistory[]>>("/api/v1/notifications/history");
