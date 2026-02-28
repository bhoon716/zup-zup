"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ChevronLeft, Loader2, Megaphone, Pin } from "lucide-react";
import { useAnnouncement } from "@/features/announcement/hooks/useAnnouncements";
import { MarkdownViewer } from "@/shared/ui/markdown-viewer";

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

export default function AnnouncementDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id ?? 0);
  const { data: announcement, isLoading } = useAnnouncement(id);

  return (
    <main className="mx-auto min-h-[calc(100dvh-4rem)] w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Megaphone className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-black tracking-tight text-slate-900">공지사항 상세</h1>
            <p className="text-sm text-slate-500">공지 본문과 작성 정보를 확인하세요.</p>
          </div>
        </div>
        <Link
          href="/announcements"
          className="inline-flex items-center gap-1 rounded-xl border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 transition hover:bg-slate-50"
        >
          <ChevronLeft className="h-3.5 w-3.5" />
          목록으로
        </Link>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center rounded-2xl border border-slate-200 bg-white py-16 text-sm text-slate-500">
          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          공지사항을 불러오는 중...
        </div>
      ) : !announcement ? (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 py-16 text-center text-sm text-slate-500">
          공지사항을 찾을 수 없습니다.
        </div>
      ) : (
        <article className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-7">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            {announcement.pinned ? (
              <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold text-primary">
                <Pin className="h-3.5 w-3.5" />
                고정
              </span>
            ) : null}
          </div>
          <h2 className="text-2xl font-black tracking-tight text-slate-900">{announcement.title}</h2>
          <MarkdownViewer content={announcement.content} className="mt-3" />
          <div className="mt-6 border-t border-slate-100 pt-4 text-xs text-slate-500">
            <p>작성자 {announcement.authorName}</p>
            <p className="mt-1">등록 {formatDateTime(announcement.createdAt)}</p>
            <p className="mt-1">수정 {formatDateTime(announcement.updatedAt)}</p>
          </div>
        </article>
      )}
    </main>
  );
}
