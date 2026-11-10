"use client";

import { ChevronRight, Loader2, Megaphone, Pin } from "lucide-react";
import Link from "next/link";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import type { PublicPageData } from "@/shared/lib/public-metadata";
import { useClientReady } from "@/shared/lib/use-client-ready";
import type { AnnouncementListItemResponse } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

function formatDate(value: string) {
  const date = new Date(`${value.slice(0, 10)}T00:00:00Z`);
  if (Number.isNaN(date.getTime())) return "-";

  return new Intl.DateTimeFormat("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    timeZone: "UTC",
  }).format(date);
}

export function HomeAnnouncements({ initialData }: { initialData?: PublicPageData<AnnouncementListItemResponse[]> | null }) {
  const isClientReady = useClientReady();
  const { data: announcements, isPending, isError } = useAnnouncements({ enabled: isClientReady, initialData });
  const isAnnouncementLoading = !announcements && (!isClientReady || isPending);
  const latestAnnouncements = (announcements ?? []).slice(0, 4);

  return (
    <section className="pb-32">
      <div className="max-w-[1200px] mx-auto px-6">
        <div className="flex items-center justify-between mb-10">
          <div>
            <h2 className="text-2xl md:text-3xl font-bold text-[#161118] flex items-center gap-3 mb-2">
              <Megaphone className="text-primary w-8 h-8 fill-primary/10" />
              공지사항
            </h2>
            <p className="text-[#161118]/60 text-sm md:text-base font-medium">서비스 및 학사의 주요 공지사항을 확인하세요.</p>
          </div>
          <Button asChild variant="link" className="text-primary font-bold flex items-center gap-1 hover:no-underline group p-0">
            <Link href="/announcements">
              전체보기 <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
          </Button>
        </div>

        {isAnnouncementLoading ? (
          <div role="status" className="flex h-36 items-center justify-center text-sm text-slate-500">
            <Loader2 aria-hidden="true" className="mr-2 h-6 w-6 motion-safe:animate-spin" />
            <span className="sr-only">공지사항을 불러오는 중입니다.</span>
          </div>
        ) : isError && !announcements ? (
          <p role="status" className="text-sm text-slate-500">공지사항을 불러오지 못했습니다.</p>
        ) : latestAnnouncements.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8 text-center text-sm text-slate-500">
            등록된 공지사항이 없습니다.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {latestAnnouncements.map((item) => (
              <Link key={item.id} href={`/announcements/${item.id}`}>
                <div
                  className="bg-white dark:bg-primary/5 p-6 rounded-2xl border border-primary/5 shadow-sm flex flex-col gap-3 hover:bg-primary/5 transition-all cursor-pointer group hover:border-primary/20 h-full"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex min-w-0 items-center gap-2.5">
                      {item.pinned ? (
                        <span className="inline-flex shrink-0 items-center gap-1 rounded-md border border-indigo-100 bg-indigo-50 px-2 py-0.5 text-[10px] font-black text-indigo-600 dark:border-indigo-900/50 dark:bg-indigo-950/50">
                          <Pin className="h-3 w-3" />
                          고정
                        </span>
                      ) : null}
                      <h3 className="truncate text-base md:text-lg font-bold text-gray-800 transition-colors group-hover:text-primary dark:text-gray-200">
                        {item.title}
                      </h3>
                    </div>
                    <span className="shrink-0 text-xs font-medium text-gray-400">{formatDate(item.createdAt)}</span>
                  </div>
                  <p className="line-clamp-2 text-sm text-slate-500">{item.previewContent || "-"}</p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
