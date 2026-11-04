import { useInfiniteQuery } from "@tanstack/react-query";
import * as notificationApi from "@/features/notification/api/notification.api";
import { useAuthStore } from "@/features/auth/store/useAuthStore";




export const useNotifications = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  const query = useInfiniteQuery({
    queryKey: ["notifications"],
    queryFn: async ({ pageParam }) => {
      const response = await notificationApi.getNotifications(pageParam);
      return response.data;
    },
    enabled: isAuthenticated,
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.last ? undefined : lastPage.number + 1,
  });

  return {
    ...query,
    data: query.data?.pages.flatMap((page) => page?.content ?? []),
  };
};
