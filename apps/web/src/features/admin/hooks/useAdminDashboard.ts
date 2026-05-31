import { useQuery } from "@tanstack/react-query";
import * as adminApi from "@/features/admin/api/admin.api";

export const useAdminDashboardSnapshot = () => {
  return useQuery({
    queryKey: ["admin", "dashboard-snapshot"],
    queryFn: async () => {
      const response = await adminApi.getDashboardSnapshot();
      return response.data;
    },
  });
};
