"use client";

import Link from "next/link";
import { ChevronRight, Loader2, Megaphone, Pin } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import { cn } from "@/shared/lib/utils";
import type { AnnouncementListItemResponse } from "@/shared/types/api";
import { dashboardListStyles as styles } from "./dashboard-list-styles";

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

export function DashboardAnnouncements({ announcements }: { announcements?: AnnouncementListItemResponse[] } = {}) {
  const { data: fetchedAnnouncements, isLoading } = useAnnouncements({ enabled: announcements === undefined });
  const latestAnnouncements = (announcements ?? fetchedAnnouncements ?? []).slice(0, 4);
  const loading = announcements ? false : isLoading;

  return (
    <div className={styles.card}>
      <div className="flex items-start justify-between">
        <h3 className={styles.header}>
          <span className={styles.icon}>
            <Megaphone aria-hidden="true" className="size-5" />
          </span>
          공지사항
        </h3>
        <Button asChild variant="ghost" size="sm" className="self-start text-xs font-bold text-muted-foreground hover:text-primary">
          <Link href="/announcements">전체보기 <ChevronRight aria-hidden="true" className="size-4" /></Link>
        </Button>
      </div>

      {loading ? (
        <div className="flex h-36 items-center justify-center text-sm text-slate-500">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          공지사항 불러오는 중...
        </div>
      ) : latestAnnouncements.length === 0 ? (
        <div className={styles.empty}>
          등록된 공지사항이 없습니다.
        </div>
      ) : (
        <div className="space-y-2">
          {latestAnnouncements.map((item) => (
            <Link
              key={item.id}
              href={`/announcements/${item.id}`}
              className={cn(styles.row, "group transition-colors hover:border-indigo-100 hover:bg-indigo-50/30 dark:hover:border-indigo-900/40 dark:hover:bg-indigo-900/10")}
            >
              <div className="flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-2.5">
                  {item.pinned ? (
                    <span className="inline-flex size-5 shrink-0 items-center justify-center rounded-md bg-indigo-50 text-indigo-600 dark:bg-indigo-950/50">
                      <Pin aria-hidden="true" className="size-3" />
                      <span className="sr-only">고정 공지</span>
                    </span>
                  ) : null}
                  <h4 className={cn(styles.title, "truncate transition-colors group-hover:text-indigo-600")}>
                    {item.title}
                  </h4>
                </div>
                <span className={styles.metadata}>{formatDate(item.createdAt)}</span>
              </div>
              <p className={cn(styles.summary, "line-clamp-2")}>{item.previewContent || "-"}</p>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
