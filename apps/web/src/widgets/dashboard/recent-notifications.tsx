"use client";

import { Bell } from "lucide-react";
import Link from "next/link";
import { format } from "date-fns";

import { NotificationHistory } from "@/shared/types/api";

/**
 * 사용자의 최근 알림 내역 중 최상위 3개를 요약하여 보여줍니다.
 * 전체 알림 페이지로 이동할 수 있는 링크를 제공합니다.
 */
export function RecentNotifications({ notifications }: { notifications: NotificationHistory[] }) {
  const limitedNotifications = notifications?.slice(0, 3) || [];

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-card border border-gray-100 dark:border-gray-800 transition-all hover:shadow-xl group flex flex-col h-full min-h-[300px]">
      <div className="flex justify-between items-center mb-5 shrink-0">
        <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
          <span className="w-8 h-8 rounded-xl bg-red-50 dark:bg-red-950/30 flex items-center justify-center">
            <Bell className="w-4 h-4 text-red-500" />
          </span>
          최근 알림
        </h3>
        <Link href="/notifications" className="text-xs font-bold text-gray-400 hover:text-primary transition-colors">
          더보기
        </Link>
      </div>
      
      <div className="space-y-4">
        {limitedNotifications.length > 0 ? (
          limitedNotifications.map((notif) => (
            <div key={notif.id} className="flex gap-4 items-start p-3.5 rounded-2xl hover:bg-gray-50 dark:hover:bg-gray-800 transition-all cursor-pointer border border-transparent hover:border-gray-100 dark:hover:border-gray-700 group/item">
              <div className="w-2 h-2 rounded-full bg-red-500 shrink-0 mt-2 shadow-[0_0_8px_rgba(239,68,68,0.5)] group-hover/item:scale-125 transition-transform" />
              <div className="flex flex-col gap-1 overflow-hidden">
                <p className="text-sm font-bold text-gray-800 dark:text-gray-200 truncate group-hover/item:text-primary transition-colors">
                  {notif.title}
                </p>
                <p className="text-[10px] text-gray-400 font-medium">
                  {format(new Date(notif.createdAt), "yy. MM. dd. HH:mm")}
                </p>
              </div>
            </div>
          ))
        ) : (
          <p className="text-xs text-center py-6 text-gray-400 font-medium">최근 알림이 없습니다.</p>
        )}
      </div>
    </div>
  );
}
