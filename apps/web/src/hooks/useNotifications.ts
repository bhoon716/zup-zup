import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import * as notificationApi from "@/lib/api/notification";
import { toast } from "sonner";

export const useNotifications = () => {
  return useQuery({
    queryKey: ["notifications"],
    queryFn: async () => {
      const response = await notificationApi.getNotifications();
      return response.data.data;
    },
  });
};
