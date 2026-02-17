import { useQuery } from "@tanstack/react-query";
import * as notificationApi from "@/features/notification/api/notification.api";

export const useNotifications = () => {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await notificationApi.getNotifications();
      return response.data.data;
    },
  });
};
