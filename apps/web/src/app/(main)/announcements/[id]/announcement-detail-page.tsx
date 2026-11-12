"use client";

import Link from "next/link";
import { isAxiosError } from "axios";
import { useParams } from "next/navigation";
import {
  ChevronLeft,
  Loader2,
  Pin,
  ArrowRight,
  Share2
} from "lucide-react";
import { useAnnouncement } from "@/features/announcement/hooks/useAnnouncements";
import { MarkdownViewer } from "@/shared/ui/markdown-viewer";
import { Button } from "@/shared/ui/button";
import type { PublicPageData } from "@/shared/lib/public-metadata";
import type { AnnouncementDetailResponse } from "@/shared/types/api";

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

export default function AnnouncementDetailPage({
  initialAnnouncement,
}: { initialAnnouncement?: PublicPageData<AnnouncementDetailResponse> | null }) {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id ?? 0);
  const { data: announcement, error, isLoading, isFetching } = useAnnouncement(id, initialAnnouncement);
  const isUnavailable = isAxiosError(error) && error.response?.status === 404;

  const handleShare = async () => {
    if (!announcement) {
      return;
    }

    const shareUrl = window.location.href;
    const shareData = {
      title: announcement.title,
      url: shareUrl,
    };

    if (navigator.share) {
      await navigator.share(shareData);
      return;
    }

    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(shareUrl);
    }
  };

  if (isLoading || (initialAnnouncement === null && isFetching)) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center bg-slate-50">
        <Loader2 className="h-10 w-10 animate-spin text-primary/40" />
        <p className="mt-4 text-sm font-bold text-slate-400">공지사항을 불러오는 중입니다...</p>
      </div>
    );
  }

  if (isUnavailable || !announcement) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center bg-slate-50">
        <h1 className="text-xl font-black text-slate-900">
          {isUnavailable ? "더 이상 확인할 수 없는 공지입니다" : "공지사항을 불러오지 못했습니다"}
        </h1>
        <Button asChild className="mt-6 rounded-2xl">
          <Link href="/announcements">목록으로 돌아가기</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="min-h-[calc(100dvh-4rem)] bg-slate-50">
      <main className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
        <div className="mb-4 flex items-center justify-between">
          <Button asChild variant="ghost" className="rounded-lg text-slate-600">
            <Link href="/announcements">
              <ChevronLeft aria-hidden="true" />
              공지사항
            </Link>
          </Button>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="rounded-lg"
            aria-label="공지 공유"
            title="공지 공유"
            onClick={() => { void handleShare(); }}
          >
            <Share2 aria-hidden="true" />
          </Button>
        </div>

        <article className="rounded-2xl border border-slate-200 bg-white px-4 py-5 shadow-sm sm:px-7 sm:py-7">
          <header className="mb-5 border-b border-slate-200 pb-5">
            {announcement.pinned && (
              <span className="mb-2 inline-flex items-center gap-1 rounded-md bg-primary/10 px-1.5 py-0.5 text-xs font-medium text-primary">
                <Pin aria-hidden="true" className="size-3" />
                중요 공지
              </span>
            )}
            <h1 className="break-words text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
              {announcement.title}
            </h1>
            <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600 sm:text-sm">
              <span>등록 <time dateTime={announcement.createdAt}>{formatDateTime(announcement.createdAt)}</time></span>
              {announcement.updatedAt !== announcement.createdAt && (
                <span>수정 <time dateTime={announcement.updatedAt}>{formatDateTime(announcement.updatedAt)}</time></span>
              )}
            </div>
          </header>

          <MarkdownViewer content={announcement.content} className="text-base leading-relaxed text-slate-700" />

          <footer className="mt-8 border-t border-slate-200 pt-4">
            <Button asChild variant="outline" className="rounded-lg">
              <Link href="/feedback">
                문의 및 건의하기
                <ArrowRight aria-hidden="true" />
              </Link>
            </Button>
          </footer>
        </article>
      </main>
    </div>
  );
}
