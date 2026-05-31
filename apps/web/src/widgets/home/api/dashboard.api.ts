import api from "@/shared/api/client";
import type { CommonResponse, DashboardSnapshotResponse } from "@/shared/types/api";

export const getDashboardSnapshot = async (): Promise<CommonResponse<DashboardSnapshotResponse>> => {
  const { data } = await api.get("/api/v1/dashboard");
  return data;
};
