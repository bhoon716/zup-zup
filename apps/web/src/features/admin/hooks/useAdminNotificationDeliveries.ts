import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AxiosError } from "axios";
import { toast } from "sonner";

import * as adminApi from "@/features/admin/api/admin.api";
import type { NotificationDeliveryReplayRequest } from "@/shared/types/api";

export const adminNotificationDeliveryKeys = {
  all: ["admin", "notification-deliveries"] as const,
  dlq: (page: number, size: number) =>
    [...adminNotificationDeliveryKeys.all, "dlq", { page, size }] as const,
  detail: (id: number) => [...adminNotificationDeliveryKeys.all, "detail", id] as const,
};

/**
 * DLQ 상태 delivery만 페이지 단위로 조회합니다.
 */
export function useAdminDlqNotificationDeliveries(page: number = 0, size: number = 20) {
  return useQuery({
    queryKey: adminNotificationDeliveryKeys.dlq(page, size),
    queryFn: async () => {
      const response = await adminApi.getAdminDlqNotificationDeliveries(page, size);
      return response.data;
    },
  });
}

/**
 * 선택한 delivery 상세를 별도로 읽어 최신 상태를 표시합니다.
 */
export function useAdminNotificationDelivery(id: number | null, enabled: boolean = true) {
  return useQuery({
    queryKey: adminNotificationDeliveryKeys.detail(id ?? 0),
    queryFn: async () => {
      const response = await adminApi.getAdminNotificationDelivery(id ?? 0);
      return response.data;
    },
    enabled: enabled && id !== null,
  });
}

/**
 * 선택한 delivery만 재처리하고 목록·상세 캐시를 새로고침합니다.
 */
export function useReplayAdminNotificationDelivery() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: NotificationDeliveryReplayRequest }) =>
      adminApi.replayAdminNotificationDelivery(id, request),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: adminNotificationDeliveryKeys.all });
      toast.success(response.message || "delivery 재처리를 요청했습니다.");
    },
    onError: (error: AxiosError<{ message: string }>) => {
      toast.error(error.response?.data?.message || "delivery 재처리 요청에 실패했습니다.");
    },
  });
}
