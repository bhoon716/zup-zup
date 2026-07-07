"use client";

import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { NotificationCard } from "./notification-card";
import { Button } from "@/shared/ui/button";
import { Loader2, Bell, ChevronDown } from "lucide-react";

export function NotificationList() {
  const { data, isLoading, error } = useNotifications();

  if (isLoading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center py-20">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
        <p className="mt-4 text-sm text-slate-400 font-medium">알림 내역을 불러오는 중입니다...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center py-20 text-center">
        <div className="w-16 h-16 rounded-full bg-red-50 flex items-center justify-center mb-4">
          <Bell className="w-8 h-8 text-red-500" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 mb-2">불러오기 실패</h3>
        <p className="text-slate-500 text-sm mb-4">알림 목록을 불러오는데 실패했습니다.</p>
        <Button onClick={() => window.location.reload()} variant="outline" className="rounded-xl">다시 시도</Button>
      </div>
    );
  }

  if (!data || data.length === 0) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center py-20 text-center">
        <div className="w-20 h-20 rounded-full bg-slate-50 flex items-center justify-center mb-6">
          <Bell className="w-10 h-10 text-slate-200" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 mb-2">알림 내역이 없습니다</h3>
        <p className="text-slate-500 text-sm">
          강의를 구독하면 빈자리 알림을 이곳에서 확인할 수 있습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="space-y-3 flex-1">
        {data.map((notification) => (
          <NotificationCard key={notification.id} notification={notification} />
        ))}
      </div>
      
      {data.length >= 10 && (
        <div className="mt-8 pt-4 border-t border-slate-50 text-center">
          <button className="inline-flex items-center gap-1 text-sm font-bold text-slate-400 hover:text-primary transition-colors py-2 px-4 rounded-xl hover:bg-slate-50">
            <span>더 많은 내역 보기</span>
            <ChevronDown className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}
