import api from "@/shared/api/client";
import type { DashboardSnapshotResponse } from "@/shared/types/api";

export const getDashboardSnapshot = async (): Promise<DashboardSnapshotResponse> => {
  const { data } = await api.get("/api/v1/dashboard");
  return data;
};
