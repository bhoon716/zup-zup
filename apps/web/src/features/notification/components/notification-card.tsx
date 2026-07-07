"use client";

import { format } from "date-fns";
import { BellRing } from "lucide-react";
import type { NotificationHistory } from "@/shared/types/api";

interface NotificationCardProps {
  notification: NotificationHistory;
}

/**
 * 알림 히스토리의 한 행을 렌더링합니다.
 * 서버가 저장한 제목과 본문을 그대로 보여줍니다.
 */
export function NotificationCard({ notification }: NotificationCardProps) {
  const sentAt = notification.sentAt || notification.createdAt;

  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-gray-200 bg-white p-4 transition-all hover:shadow-md">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <BellRing className="h-4 w-4 shrink-0 text-primary" />
            <h4 className="truncate text-base font-bold text-slate-900">
              {notification.title}
            </h4>
          </div>
          <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-600">
            {notification.message}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-400">
        <span>{notification.channel}</span>
        <span>·</span>
        <span>{sentAt ? format(new Date(sentAt), "yyyy.MM.dd HH:mm") : "-"}</span>
      </div>
    </div>
  );
}
