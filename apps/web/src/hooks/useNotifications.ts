import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import * as notificationApi from "@/lib/api/notification";
import { toast } from "sonner";

export const useNotifications = (page = 0, size = 20) => {
  return useQuery({
    queryKey: ["notifications", page, size],
    queryFn: async () => {
      const response = await notificationApi.getNotifications(page, size);
      return response.data.data;
    },
  });
};

export const useMarkAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => notificationApi.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
    onError: () => {
      toast.error("알림 읽음 처리에 실패했습니다.");
    },
  });
};

export const useMarkAllAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationApi.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      toast.success("모든 알림을 읽음 처리했습니다.");
    },
    onError: () => {
      toast.error("알림 읽음 처리에 실패했습니다.");
    },
  });
};
