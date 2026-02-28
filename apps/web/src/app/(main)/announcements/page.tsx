"use client";

import { useDeferredValue, useState } from "react";
import Link from "next/link";
import { ArrowRight, Loader2, Megaphone, Pin } from "lucide-react";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import type { AnnouncementSearchType } from "@/shared/types/api";

const formatDateTime = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

export default function AnnouncementsPage() {
  const [keyword, setKeyword] = useState("");
  const [searchType, setSearchType] = useState<AnnouncementSearchType>("TITLE_CONTENT");
  const deferredKeyword = useDeferredValue(keyword);
  const { data: announcements, isLoading } = useAnnouncements({
    keyword: deferredKeyword,
    searchType,
  });

  return (
    <main className="mx-auto min-h-[calc(100dvh-4rem)] w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <Megaphone className="h-6 w-6" />
        </div>
        <div>
          <h1 className="text-2xl font-black tracking-tight text-slate-900">공지사항</h1>
          <p className="text-sm text-slate-500">서비스 공지 및 운영 안내를 확인하세요.</p>
        </div>
      </div>

      <div className="mb-5 rounded-2xl border border-slate-200 bg-white p-4">
        <div className="grid gap-3 sm:grid-cols-[200px_1fr]">
          <select
            value={searchType}
            onChange={(event) => setSearchType(event.target.value as AnnouncementSearchType)}
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none ring-primary/20 transition focus:border-primary focus:ring-2"
          >
            <option value="TITLE">제목</option>
            <option value="CONTENT">내용</option>
            <option value="TITLE_CONTENT">제목 + 내용</option>
          </select>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="검색어를 입력하세요."
            className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none ring-primary/20 transition focus:border-primary focus:ring-2"
          />
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center rounded-2xl border border-slate-200 bg-white py-16 text-sm text-slate-500">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          공지사항을 불러오는 중...
        </div>
      ) : (
        <div className="space-y-4">
          {(announcements ?? []).map((announcement) => (
            <Link key={announcement.id} href={`/announcements/${announcement.id}`} className="block">
              <article className="rounded-2xl border border-slate-200 bg-white px-5 py-4 shadow-sm transition hover:border-primary/30 hover:shadow-md">
                <div className="mb-1.5 flex items-center gap-2">
                  {announcement.pinned ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold text-primary">
                      <Pin className="h-3.5 w-3.5" />
                      고정
                    </span>
                  ) : null}
                </div>
                <h2 className="text-base font-bold text-slate-900 sm:text-lg">{announcement.title}</h2>
                <p className="mt-2 overflow-hidden text-ellipsis whitespace-nowrap text-sm text-slate-700">
                  {announcement.previewContent || "-"}
                </p>
                <div className="mt-3 flex items-center justify-between gap-3 text-xs text-slate-500">
                  <p>
                    작성자 {announcement.authorName} · {formatDateTime(announcement.createdAt)}
                  </p>
                  <span className="inline-flex items-center gap-1 font-semibold text-primary">
                    상세보기
                    <ArrowRight className="h-3.5 w-3.5" />
                  </span>
                </div>
              </article>
            </Link>
          ))}

          {(announcements ?? []).length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 py-16 text-center text-sm text-slate-500">
              등록된 공지사항이 없습니다.
            </div>
          ) : null}
        </div>
      )}
    </main>
  );
}
