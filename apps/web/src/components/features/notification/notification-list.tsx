"use client";

import { useNotifications } from "@/hooks/useNotifications";
import { NotificationCard } from "./notification-card";
import { Button } from "@/components/ui/button";
import { Loader2, Bell } from "lucide-react";

export function NotificationList() {
  const { data, isLoading, error } = useNotifications();

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

  if (!data || data.length === 0) {
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

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-2xl font-bold">알림 내역</h2>
          <p className="text-sm text-muted-foreground mt-1">
            총 {data.length}개의 알림 수신 내역이 있습니다.
          </p>
        </div>
      </div>

      <div className="space-y-3">
        {data.map((notification) => (
          <NotificationCard key={notification.id} notification={notification} />
        ))}
      </div>
    </div>
  );
}
