import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CanceledError } from "axios";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { getInbox, updateInbox, type InboxSelection } from "../api/inbox.api";

const inboxKey = (userId: number) => ["notification-inbox", userId] as const;

export function useInbox(userId: number, selection: InboxSelection) {
  return useQuery({
    queryKey: [...inboxKey(userId), selection],
    queryFn: ({ signal }) => getInbox(selection, signal),
    staleTime: 0,
    refetchOnMount: "always",
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    retry: false,
  });
}

export function useUpdateInbox(userId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (command: Parameters<typeof updateInbox>[0]) => {
      const { isAuthenticated, user } = useAuthStore.getState();
      if (!isAuthenticated || user?.id !== userId) {
        throw new CanceledError("로그인 세션이 변경되었습니다.");
      }
      return updateInbox(command);
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: inboxKey(userId) }),
  });
}
