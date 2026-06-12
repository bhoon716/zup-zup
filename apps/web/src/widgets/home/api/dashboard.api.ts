import api from "@/shared/api/client";
import type { AxiosRequestConfig } from "axios";
import type { CommonResponse, DashboardSnapshotResponse } from "@/shared/types/api";

export const getDashboardSnapshot = async (): Promise<CommonResponse<DashboardSnapshotResponse>> => {
  const config = { skipAuthRefresh: true } as AxiosRequestConfig & { skipAuthRefresh: boolean };
  const { data } = await api.get("/api/v1/dashboard", config);
  return data;
};
