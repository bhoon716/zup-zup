import api from "@/shared/api/client";
import type { AnnouncementDetailResponse, AnnouncementListItemResponse, AnnouncementSearchType, PageResponse } from "@/shared/types/api";

export interface AnnouncementSearchParams {
  keyword?: string;
  searchType?: AnnouncementSearchType;
  page?: number;
}

export const getAnnouncements = async (
  params: AnnouncementSearchParams = {}
): Promise<PageResponse<AnnouncementListItemResponse>> => {
  const keyword = params.keyword?.trim();
  const { data } = await api.get("/api/v1/announcements", {
    params: {
      keyword: keyword ? keyword : undefined,
      searchType: params.searchType ?? "TITLE_CONTENT",
      page: params.page ?? 0,
    },
  });
  return data;
};

export const getAnnouncement = async (id: number): Promise<AnnouncementDetailResponse> => {
  const { data } = await api.get(`/api/v1/announcements/${id}`);
  return data;
};
