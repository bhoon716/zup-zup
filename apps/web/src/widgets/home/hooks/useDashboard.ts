import { useQuery } from "@tanstack/react-query";
import * as dashboardApi from "@/widgets/home/api/dashboard.api";
import type { DashboardSnapshotResponse } from "@/shared/types/api";

export const useDashboardSnapshot = (options?: { enabled?: boolean }) => {
  return useQuery<DashboardSnapshotResponse | null>({
    queryKey: ["dashboard", "snapshot"],
    enabled: options?.enabled ?? true,
    queryFn: async () => {
      try {
        const response = await dashboardApi.getDashboardSnapshot();
        if (!response.data.user) {
          return null;
        }

        return response.data;
      } catch (error) {
        if (error && typeof error === "object" && "response" in error) {
          const responseError = error as { response?: { status?: number } };
          if (responseError.response?.status === 401) {
            return null;
          }
        }
        throw error;
      }
    },
  });
};
