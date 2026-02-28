import api from "@/shared/api/client";
import type {
  AnnouncementDetailResponse,
  AnnouncementListItemResponse,
  AnnouncementSearchType,
  CommonResponse,
} from "@/shared/types/api";

export interface AnnouncementSearchParams {
  keyword?: string;
  searchType?: AnnouncementSearchType;
}

export const getAnnouncements = async (
  params: AnnouncementSearchParams = {}
): Promise<CommonResponse<AnnouncementListItemResponse[]>> => {
  const keyword = params.keyword?.trim();
  const { data } = await api.get("/api/v1/announcements", {
    params: {
      keyword: keyword ? keyword : undefined,
      searchType: params.searchType ?? "TITLE_CONTENT",
    },
  });
  return data;
};

export const getAnnouncement = async (id: number): Promise<CommonResponse<AnnouncementDetailResponse>> => {
  const { data } = await api.get(`/api/v1/announcements/${id}`);
  return data;
};
