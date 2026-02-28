import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";
import * as adminApi from "@/features/admin/api/admin.api";
import type { AnnouncementRequest } from "@/shared/types/api";

export const useAdminAnnouncements = () => {
  return useQuery({
    queryKey: ["admin", "announcements"],
    queryFn: async () => {
      const response = await adminApi.getAdminAnnouncements();
      return response.data;
    },
  });
};

export const useCreateAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: AnnouncementRequest) => adminApi.createAdminAnnouncement(request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success(response.message || "공지사항을 등록했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message || "공지사항 등록 중 오류가 발생했습니다.");
    },
  });
};

export const useUpdateAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: AnnouncementRequest }) =>
      adminApi.updateAdminAnnouncement(id, request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success(response.message || "공지사항을 수정했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message || "공지사항 수정 중 오류가 발생했습니다.");
    },
  });
};

export const useDeleteAdminAnnouncement = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => adminApi.deleteAdminAnnouncement(id),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "announcements"] });
      queryClient.invalidateQueries({ queryKey: ["announcements"] });
      toast.success(response.message || "공지사항을 삭제했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message || "공지사항 삭제 중 오류가 발생했습니다.");
    },
  });
};
