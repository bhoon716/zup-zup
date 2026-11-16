import { useQuery } from "@tanstack/react-query";
import type { AdminTrafficRange } from "@/shared/types/api";
import * as adminApi from "@/features/admin/api/admin.api";

export const useAdminDashboardSnapshot = () => {
  return useQuery({
    queryKey: ["admin", "dashboard-snapshot"],
    queryFn: adminApi.getDashboardSnapshot,
  });
};


export const useAdminNotificationTraffic = (range: AdminTrafficRange) => {
  return useQuery({
    queryKey: ["admin", "notification-traffic", range],
    queryFn: () => adminApi.getNotificationTraffic(range),
  });
};
