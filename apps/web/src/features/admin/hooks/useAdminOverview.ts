import { useQuery } from "@tanstack/react-query";
import * as adminApi from "@/features/admin/api/admin.api";

export const useAdminOverview = () => {
  return useQuery({
    queryKey: ["admin", "overview"],
    queryFn: async () => {
      const response = await adminApi.getDashboardOverview();
      return response.data;
    },
    refetchInterval: 30000,
  });
};
