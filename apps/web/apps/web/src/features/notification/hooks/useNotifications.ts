import { useQuery } from "@tanstack/react-query";
import * as notificationApi from "@/features/notification/api/notification.api";
import { useAuthStore } from "@/features/auth/store/useAuthStore";

/**
 * 유저의 최근 수신 알림 목록을 조회하는 훅
 */
export const useNotifications = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await notificationApi.getNotifications();
      return response.data.data;
    },
    enabled: isAuthenticated,
  });
};
