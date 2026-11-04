import { useQuery } from "@tanstack/react-query";
import * as announcementApi from "@/features/announcement/api/announcement.api";
import type { AnnouncementSearchType } from "@/shared/types/api";

interface UseAnnouncementsParams {
  keyword?: string;
  searchType?: AnnouncementSearchType;
  page?: number;
  enabled?: boolean;
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
    enabled: params.enabled ?? true,
  });
};

export const useAnnouncement = (id: number) => {
  return useQuery({
    queryKey: ["announcements", id],
    queryFn: async () => {
      const response = await announcementApi.getAnnouncement(id);
      return response;
    },
    enabled: Number.isFinite(id) && id > 0,
  });
};
