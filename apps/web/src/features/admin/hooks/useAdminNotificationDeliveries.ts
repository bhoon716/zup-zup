import { localizedErrorMessage } from "@/shared/lib/error-message";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";

import * as adminApi from "@/features/admin/api/admin.api";
import type { NotificationDeliveryReplayRequest } from "@/shared/types/api";

export const adminNotificationDeliveryKeys = {
  all: ["admin", "notification-deliveries"] as const,
  dlqPages: ["admin", "notification-deliveries", "dlq"] as const,
  dlq: (page: number) =>
    [...adminNotificationDeliveryKeys.dlqPages, { page }] as const,

  detail: (id: number) => [...adminNotificationDeliveryKeys.all, "detail", id] as const,
};

export function useAdminDlqNotificationDeliveries(page: number = 0) {
  return useQuery({
    queryKey: adminNotificationDeliveryKeys.dlq(page),
    queryFn: async () => {
      const response = await adminApi.getAdminDlqNotificationDeliveries(page);
      return response;
    },
  });
}

export function useAdminNotificationDelivery(id: number | null) {
  return useQuery({
    queryKey: adminNotificationDeliveryKeys.detail(id ?? 0),
    queryFn: () => adminApi.getAdminNotificationDelivery(id ?? 0),
    staleTime: 0,
    enabled: id !== null,
  });
}

export function useReplayAdminNotificationDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: NotificationDeliveryReplayRequest }) =>
      adminApi.replayAdminNotificationDelivery(id, request),

    onSuccess: (_delivery, { id }) => {
      void queryClient.invalidateQueries({ queryKey: adminNotificationDeliveryKeys.detail(id), exact: true });
      void queryClient.invalidateQueries({ queryKey: adminNotificationDeliveryKeys.dlqPages });
      toast.success("알림 발송 재처리를 요청했습니다.");
    },

    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(localizedErrorMessage(error.response?.data?.message, "알림 발송 재처리 요청에 실패했습니다."));
    },
  });
}
