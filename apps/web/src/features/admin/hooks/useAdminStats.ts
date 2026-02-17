import { useQuery } from "@tanstack/react-query";
import * as adminApi from "@/features/admin/api/admin.api";

export const useAdminStats = () => {
  return useQuery({
    queryKey: ["admin", "stats"],
    queryFn: async () => {
      const response = await adminApi.getDashboardStats();
      return response.data;
    },
  });
};
