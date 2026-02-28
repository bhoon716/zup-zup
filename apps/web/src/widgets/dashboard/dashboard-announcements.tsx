"use client";

import Link from "next/link";
import { ChevronRight, Loader2, Megaphone, Pin } from "lucide-react";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import { Button } from "@/shared/ui/button";

const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

/**
 * 서비스 및 학사의 주요 공지사항을 보여주는 컴포넌트입니다.
 */
export function DashboardAnnouncements() {
  const { data: announcements, isLoading } = useAnnouncements();
  const latestAnnouncements = (announcements ?? []).slice(0, 4);

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full">
      <div className="flex justify-between items-center mb-5">
        <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center">
            <Megaphone className="w-5 h-5 text-indigo-600" />
          </div>
          공지사항
        </h3>
        <Link href="/announcements">
          <Button variant="ghost" size="sm" className="text-xs font-bold text-gray-400 hover:text-primary transition-colors">
            전체보기 <ChevronRight className="w-4 h-4" />
          </Button>
        </Link>
      </div>

      {isLoading ? (
        <div className="flex h-36 items-center justify-center text-sm text-slate-500">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          공지사항 불러오는 중...
        </div>
      ) : latestAnnouncements.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
          등록된 공지사항이 없습니다.
        </div>
      ) : (
        <div className="space-y-3">
          {latestAnnouncements.map((item) => (
            <Link
              key={item.id}
              href={`/announcements/${item.id}`}
              className="group block rounded-2xl border border-gray-50 p-3.5 transition-all hover:border-indigo-100 hover:bg-indigo-50/30 dark:border-gray-800/50 dark:hover:border-indigo-900/40 dark:hover:bg-indigo-900/10"
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-2.5">
                  {item.pinned ? (
                    <span className="inline-flex shrink-0 items-center gap-1 rounded-md border border-indigo-100 bg-indigo-50 px-2 py-0.5 text-[10px] font-black text-indigo-600 dark:border-indigo-900/50 dark:bg-indigo-950/50">
                      <Pin className="h-3 w-3" />
                      고정
                    </span>
                  ) : null}
                  <h4 className="truncate text-sm font-bold text-gray-800 transition-colors group-hover:text-indigo-600 dark:text-gray-200">
                    {item.title}
                  </h4>
                </div>
                <span className="shrink-0 text-[10px] font-medium text-gray-400">{formatDate(item.createdAt)}</span>
              </div>
              <p className="mt-2 line-clamp-2 text-xs text-slate-500">{item.previewContent || "-"}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
