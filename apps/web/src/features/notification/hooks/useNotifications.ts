import { useInfiniteQuery } from "@tanstack/react-query";
import * as notificationApi from "@/features/notification/api/notification.api";
import { useAuthStore } from "@/features/auth/store/useAuthStore";

export const useNotifications = () => {
  const userId = useAuthStore((state) => state.isAuthenticated ? state.user?.id : undefined);

  const query = useInfiniteQuery({
    queryKey: ["notifications", userId],
    queryFn: async ({ pageParam, signal }) => {
      const response = await notificationApi.getNotifications(pageParam, signal);
      return response.data;
    },
    enabled: userId !== undefined,
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.last ? undefined : lastPage.number + 1,
  });

  return {
    ...query,
    data: query.data?.pages.flatMap((page) => page?.content ?? []),
  };
};
