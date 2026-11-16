import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CanceledError } from "axios";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { getInbox, updateInbox, type InboxSelection } from "../api/inbox.api";

const inboxKey = (userId: number) => ["notification-inbox", userId] as const;
const unreadSelection = { view: "INBOX", page: 0, read: "UNREAD", type: "ALL", keyword: "", sort: "NEWEST" } satisfies InboxSelection;
const unreadRefreshIntervalMs = 60_000;

export function useHasUnreadInbox(userId: number) {
  return useQuery({
    queryKey: [...inboxKey(userId), "has-unread"],
    queryFn: async ({ signal }) => (await getInbox(unreadSelection, signal)).totalElements > 0,
    staleTime: 0,
    refetchInterval: unreadRefreshIntervalMs,
    refetchOnWindowFocus: true,
    refetchOnReconnect: true,
    retry: false,
  });
}

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
