import { useQuery } from "@tanstack/react-query";
import * as announcementApi from "@/features/announcement/api/announcement.api";
import type { AnnouncementSearchType } from "@/shared/types/api";

interface UseAnnouncementsParams {
  keyword?: string;
  searchType?: AnnouncementSearchType;
}

export const useAnnouncements = (params: UseAnnouncementsParams = {}) => {
  const keyword = params.keyword?.trim() ?? "";
  const searchType = params.searchType ?? "TITLE_CONTENT";

  return useQuery({
    queryKey: ["announcements", { keyword, searchType }],
    queryFn: async () => {
      const response = await announcementApi.getAnnouncements({ keyword, searchType });
      return response.data;
    },
  });
};

export const useAnnouncement = (id: number) => {
  return useQuery({
    queryKey: ["announcements", id],
    queryFn: async () => {
      const response = await announcementApi.getAnnouncement(id);
      return response.data;
    },
    enabled: Number.isFinite(id) && id > 0,
  });
};
