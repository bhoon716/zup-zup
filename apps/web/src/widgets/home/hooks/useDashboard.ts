import { useQuery } from "@tanstack/react-query";
import * as dashboardApi from "@/widgets/home/api/dashboard.api";

export const useDashboardSnapshot = () => {
  return useQuery({
    queryKey: ["dashboard", "snapshot"],
    queryFn: async () => {
      const response = await dashboardApi.getDashboardSnapshot();
      return response.data;
    },
  });
};
