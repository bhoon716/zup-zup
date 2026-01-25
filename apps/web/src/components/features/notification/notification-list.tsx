"use client";

import { useNotifications, useMarkAllAsRead } from "@/hooks/useNotifications";
import { NotificationCard } from "./notification-card";
import { Button } from "@/components/ui/button";
import { Loader2, Bell, CheckCheck } from "lucide-react";
import { useState } from "react";

export function NotificationList() {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useNotifications(page, 20);
  const { mutate: markAllAsRead, isPending } = useMarkAllAsRead();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 mb-4">알림 목록을 불러오는데 실패했습니다.</p>
        <Button onClick={() => window.location.reload()}>다시 시도</Button>
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="mb-4">
          <Bell className="w-16 h-16 mx-auto text-gray-300" />
        </div>
        <h3 className="text-lg font-semibold mb-2">알림이 없습니다</h3>
        <p className="text-muted-foreground">
          강좌를 구독하면 빈자리 알림을 받을 수 있습니다.
        </p>
      </div>
    );
  }

  const unreadCount = data.content.filter((n) => !n.isRead).length;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-2xl font-bold">알림 내역</h2>
          <p className="text-sm text-muted-foreground mt-1">
            총 {data.totalElements}개 알림 {unreadCount > 0 && `(읽지 않음 ${unreadCount}개)`}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => markAllAsRead()}
            disabled={isPending}
            className="gap-2"
          >
            <CheckCheck className="w-4 h-4" />
            모두 읽음
          </Button>
        )}
      </div>

      <div className="space-y-3">
        {data.content.map((notification) => (
          <NotificationCard key={notification.id} notification={notification} />
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-6">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
          >
            이전
          </Button>
          <span className="text-sm text-muted-foreground">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page === data.totalPages - 1}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  );
}
