"use client";

import Link from "next/link";
import { isAxiosError } from "axios";
import { useParams } from "next/navigation";
import { ArrowRight, ChevronLeft, Loader2 } from "lucide-react";
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

  if (isLoading || (initialAnnouncement === null && isFetching)) {
    return (
      <main className="grid min-h-[calc(100dvh-4rem)] place-items-center px-4">
        <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 aria-hidden="true" className="size-4 motion-safe:animate-spin" />
          공지사항을 불러오는 중입니다.
        </p>
      </main>
    );
  }

  if (isUnavailable || !announcement) {
    return (
      <main className="flex min-h-[calc(100dvh-4rem)] flex-col items-center justify-center px-4 text-center">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {isUnavailable ? "더 이상 확인할 수 없는 공지입니다" : "공지사항을 불러오지 못했습니다"}
        </h1>
        <Button asChild className="mt-6 rounded-lg">
          <Link href="/announcements">목록으로 돌아가기</Link>
        </Button>
      </main>
    );
  }

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
      <Button asChild variant="ghost" className="-ml-2 mb-4 rounded-lg text-muted-foreground">
        <Link href="/announcements">
          <ChevronLeft aria-hidden="true" />
          공지사항
        </Link>
      </Button>

      <article className="rounded-xl border bg-card px-4 py-5 sm:px-7 sm:py-7">
        <header className="mb-5 border-b pb-5">
          <h1 className="text-2xl font-bold tracking-tight wrap-anywhere break-keep text-foreground sm:text-3xl">
            {announcement.title}
          </h1>
          <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-xs text-muted-foreground">
            <span>등록 <time dateTime={announcement.createdAt}>{formatDateTime(announcement.createdAt)}</time></span>
            {announcement.updatedAt !== announcement.createdAt && (
              <span>수정 <time dateTime={announcement.updatedAt}>{formatDateTime(announcement.updatedAt)}</time></span>
            )}
          </div>
        </header>

        <MarkdownViewer
          content={announcement.content}
          className="text-sm text-muted-foreground [&_:is(h1,h2,h3,h4)]:text-base [&_:is(h1,h2,h3,h4)]:font-semibold [&_:is(h1,h2,h3,h4,p,ul,ol,table,th)]:text-foreground"
        />

        <footer className="mt-8 border-t pt-4">
          <Button asChild variant="outline" className="rounded-lg">
            <Link href="/feedback">
              문의 및 건의하기
              <ArrowRight aria-hidden="true" />
            </Link>
          </Button>
        </footer>
      </article>
    </main>
  );
}
