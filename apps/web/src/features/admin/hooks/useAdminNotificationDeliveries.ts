import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";

import * as adminApi from "@/features/admin/api/admin.api";
import type { NotificationDeliveryReplayRequest } from "@/shared/types/api";

export const adminNotificationDeliveryKeys = {
  all: ["admin", "notification-deliveries"] as const,
  dlq: (page: number) =>
    [...adminNotificationDeliveryKeys.all, "dlq", { page }] as const,
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




export function useAdminNotificationDelivery(id: number | null, enabled: boolean = true) {
  return useQuery({
    queryKey: adminNotificationDeliveryKeys.detail(id ?? 0),
    queryFn: async () => {
      const response = await adminApi.getAdminNotificationDelivery(id ?? 0);
      return response;
    },
    enabled: enabled && id !== null,
  });
}




export function useReplayAdminNotificationDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: NotificationDeliveryReplayRequest }) =>
      adminApi.replayAdminNotificationDelivery(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminNotificationDeliveryKeys.all });
      toast.success("delivery 재처리를 요청했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message || "delivery 재처리 요청에 실패했습니다.");
    },
  });
}
