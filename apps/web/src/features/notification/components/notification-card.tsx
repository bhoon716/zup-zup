"use client";

import { format } from "date-fns";
import type { NotificationHistory } from "@/shared/types/api";

interface NotificationCardProps {
  notification: NotificationHistory;
}

/**
 * 알림 히스토리의 한 행을 렌더링합니다.
 * 날짜, 강의명, 강의 코드, 교수명 및 여석 발생 상태를 표시합니다.
 */
export function NotificationCard({ notification }: NotificationCardProps) {
  // 메시지에서 "X자리 발생" 부분 추출 시도 (서버 메시지 형식에 의존)
  const seatMatch = notification.message.match(/(\d+)자리/);
  const seatCount = seatMatch ? seatMatch[1] : null;

  return (
    <div className="bg-white p-4 rounded-xl border border-gray-200 hover:shadow-md transition-all cursor-pointer group flex flex-col sm:flex-row gap-3 items-start sm:items-center">
      <div className="flex-1 w-full flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-6">
        <span className="text-sm text-slate-500 font-medium whitespace-nowrap min-w-[120px]">
          {format(new Date(notification.createdAt), "yyyy.MM.dd HH:mm")}
        </span>
        <div className="flex items-center gap-3">
          <h4 className="font-bold text-base text-slate-900 truncate max-w-[180px]">
            {notification.title}
          </h4>
          <span className="text-xs text-gray-300">|</span>
          <p className="text-sm text-slate-500 truncate">
            {notification.courseKey.split(':').slice(2).join(' • ')}
          </p>
        </div>
      </div>
      
      <div className="w-full sm:w-auto flex justify-between sm:justify-end items-center">
        <div className="inline-flex items-center gap-1.5 bg-primary/5 px-3 py-1.5 rounded-lg text-primary border border-primary/10 shadow-sm">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary/40 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-primary"></span>
          </span>
          <span className="text-xs font-bold">
            {seatCount ? `${seatCount}자리 발생` : "여석 발생"}
          </span>
        </div>
      </div>
    </div>
  );
}
