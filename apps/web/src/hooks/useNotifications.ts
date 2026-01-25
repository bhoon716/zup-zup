import { useQuery } from "@tanstack/react-query";
import * as notificationApi from "@/lib/api/notification";

export const useNotifications = () => {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await notificationApi.getNotifications();
      return response.data.data;
    },
  });
};
