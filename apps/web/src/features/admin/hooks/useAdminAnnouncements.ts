import { localizedErrorMessage } from "@/shared/lib/error-message";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import * as adminApi from "@/features/admin/api/admin.api";
import type { AnnouncementDetailResponse, AnnouncementRequest, PageResponse } from "@/shared/types/api";

const adminAnnouncementsKey = ["admin", "announcements"];

export const useAdminAnnouncements = (page: number) => {
  return useQuery({
    queryKey: [...adminAnnouncementsKey, page],
    queryFn: () => adminApi.getAdminAnnouncements(page),
    placeholderData: keepPreviousData,
  });
};

export const useCreateAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AnnouncementRequest) => adminApi.createAdminAnnouncement(request),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminAnnouncementsKey });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success("공지사항을 등록했습니다.");
    },

    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(localizedErrorMessage(error.response?.data?.message, "공지사항 등록 중 오류가 발생했습니다."));
    },
  });
};

export const useUpdateAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: AnnouncementRequest }) =>
      adminApi.updateAdminAnnouncement(id, request),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminAnnouncementsKey });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success("공지사항을 수정했습니다.");
    },

    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(localizedErrorMessage(error.response?.data?.message, "공지사항 수정 중 오류가 발생했습니다."));
    },
  });
};

export const useDeleteAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.deleteAdminAnnouncement(id),

    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminAnnouncementsKey });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success("공지사항을 삭제했습니다.");
    },

    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(localizedErrorMessage(error.response?.data?.message, "공지사항 삭제 중 오류가 발생했습니다."));
    },
  });
};

export const useRestoreAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: adminApi.restoreAdminAnnouncement,
    onSuccess: (restored) => {
      queryClient.setQueriesData<PageResponse<AnnouncementDetailResponse>>({ queryKey: adminAnnouncementsKey }, (page) =>
        page && { ...page, content: page.content.map((announcement) => announcement.id === restored.id ? restored : announcement) }
      );
      queryClient.invalidateQueries({ queryKey: adminAnnouncementsKey });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success("공지사항을 복원했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(localizedErrorMessage(error.response?.data?.message, "공지사항 복원 중 오류가 발생했습니다."));
    },
  });
};
