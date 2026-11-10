import { useQuery } from "@tanstack/react-query";
import * as announcementApi from "@/features/announcement/api/announcement.api";
import type { AnnouncementDetailResponse, AnnouncementListItemResponse, AnnouncementSearchType } from "@/shared/types/api";
import type { PublicPageData } from "@/shared/lib/public-metadata";

interface UseAnnouncementsParams {
  keyword?: string;
  searchType?: AnnouncementSearchType;
  page?: number;
  enabled?: boolean;
  initialData?: PublicPageData<AnnouncementListItemResponse[]> | null;
}

export const useAnnouncements = (params: UseAnnouncementsParams = {}) => {
  const keyword = params.keyword?.trim() ?? "";
  const searchType = params.searchType ?? "TITLE_CONTENT";
  const page = params.page ?? 0;

  return useQuery({
    queryKey: ["announcements", { keyword, searchType, page }],
    queryFn: async () => {
      const response = await announcementApi.getAnnouncements({ keyword, searchType, page });
      return response.content;
    },
    initialData: params.initialData?.data,
    initialDataUpdatedAt: params.initialData?.updatedAt,
    enabled: params.enabled ?? true,
  });
};

export const useAnnouncement = (id: number, initialAnnouncement?: PublicPageData<AnnouncementDetailResponse> | null) => {
  const seed = initialAnnouncement?.data.id === id ? initialAnnouncement : undefined;
  return useQuery({
    queryKey: ["announcements", id],
    queryFn: async () => {
      const response = await announcementApi.getAnnouncement(id);
      return response;
    },
    initialData: seed?.data,
    initialDataUpdatedAt: seed?.updatedAt,
    enabled: Number.isFinite(id) && id > 0,
  });
};
