"use client";

import { Fragment, useDeferredValue, useRef, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, Pin, Search } from "lucide-react";
import { useAnnouncementPage } from "@/features/announcement/hooks/useAnnouncements";
import type { AnnouncementSearchType } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/ui/select";

const searchOptions = [
  { value: "TITLE_CONTENT", label: "제목 + 내용" },
  { value: "TITLE", label: "제목" },
  { value: "CONTENT", label: "내용" },
] satisfies { value: AnnouncementSearchType; label: string }[];

const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  timeZone: "UTC",
});

function formatDate(value: string) {
  const date = new Date(`${value.slice(0, 10)}T00:00:00Z`);
  return Number.isNaN(date.getTime()) ? "-" : dateFormatter.format(date);
}

export default function AnnouncementsPage() {
  const [search, setSearch] = useState<{ keyword: string; searchType: AnnouncementSearchType; page: number }>({
    keyword: "",
    searchType: "TITLE_CONTENT",
    page: 0,
  });
  const listTop = useRef<HTMLFormElement>(null);
  const deferredSearch = useDeferredValue(search);
  const { data, isPending, isError, refetch } = useAnnouncementPage(deferredSearch);
  const lastPage = Math.max(0, (data?.totalPages ?? 1) - 1);
  if (data && search === deferredSearch && search.page > lastPage) {
    setSearch({ ...search, page: lastPage });
  }

  const changePage = (page: number) => {
    if (page === search.page) return;
    setSearch({ ...search, page });
    listTop.current?.focus({ preventScroll: true });
    listTop.current?.scrollIntoView({ block: "start" });
  };

  return (
    <main className="font-schedule mx-auto w-full max-w-5xl px-4 pt-7 pb-12 sm:px-6 sm:pt-10">
      <header className="mb-4 flex flex-col gap-3 sm:mb-5 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold text-foreground sm:text-3xl">공지사항</h1>

        <form
          ref={listTop}
          role="search"
          aria-label="공지사항 검색"
          tabIndex={-1}
          className="flex w-full scroll-mt-20 items-center rounded-lg border bg-card px-1 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary sm:w-80"
          onSubmit={(event) => event.preventDefault()}
        >
          <div className="relative min-w-0 flex-1">
            <Search aria-hidden="true" className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              aria-label="공지 검색어"
              value={search.keyword}
              maxLength={100}
              onChange={(event) => setSearch({ ...search, keyword: event.target.value, page: 0 })}
              placeholder="공지 검색"
              className="h-10 rounded-md border-0 pr-2 pl-9 text-sm shadow-none"
            />
          </div>
          <Select
            value={search.searchType}
            onValueChange={(value) => {
              const option = searchOptions.find((candidate) => candidate.value === value);
              if (option) setSearch({ ...search, searchType: option.value, page: 0 });
            }}
          >
            <SelectTrigger aria-label="검색 기준" className="shrink-0 rounded-md border-0 px-2 shadow-none before:mr-1 before:h-4 before:w-px before:bg-border hover:bg-muted">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="start" className="font-schedule">
              {searchOptions.map(({ value, label }) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
        </form>
      </header>

      <section aria-label="공지 목록" className="overflow-hidden rounded-xl border bg-card">
        {isPending ? (
          <p role="status" className="px-4 py-12 text-center text-sm text-muted-foreground">공지사항을 불러오는 중입니다.</p>
        ) : isError ? (
          <div role="alert" className="flex flex-wrap items-center justify-center gap-3 px-4 py-12 text-sm text-muted-foreground">
            공지사항을 불러오지 못했습니다.
            <Button variant="outline" size="sm" onClick={() => { void refetch(); }}>다시 시도</Button>
          </div>
        ) : data.content.length === 0 ? (
          <p className="px-4 py-12 text-center text-sm text-muted-foreground">
            {search.keyword.trim() ? "검색 결과가 없습니다." : "등록된 공지사항이 없습니다."}
          </p>
        ) : (
          <ul className="divide-y">
            {data.content.map((announcement) => (
              <li key={announcement.id}>
                <Link
                  href={`/announcements/${announcement.id}`}
                  data-pinned={announcement.pinned || undefined}
                  className="group grid grid-cols-[minmax(0,1fr)_auto] items-center gap-x-3 gap-y-1.5 border-l-2 border-transparent px-3.5 py-3 transition-colors hover:bg-muted/70 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary data-pinned:border-l-primary/45 data-pinned:bg-primary/[0.045] data-pinned:hover:bg-primary/[0.08] sm:grid-cols-[minmax(0,1fr)_auto_auto] sm:gap-x-5 sm:px-5"
                >
                  <div className="col-start-1 row-start-1 min-w-0">
                    <h2 className="flex items-start gap-2 text-[0.9375rem] leading-snug font-medium text-foreground group-hover:text-primary sm:text-base">
                      {announcement.pinned && <span className="sr-only">상단 고정 </span>}
                      {announcement.pinned && (
                        <Pin aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-primary" />
                      )}
                      <span className="min-w-0 wrap-anywhere break-keep">{announcement.title}</span>
                    </h2>
                    {announcement.previewContent && <p className="mt-1 truncate text-sm leading-normal text-muted-foreground">{announcement.previewContent}</p>}
                  </div>
                  <time dateTime={announcement.createdAt} className="col-start-1 row-start-2 text-xs whitespace-nowrap tabular-nums text-muted-foreground sm:col-start-2 sm:row-start-1">
                    {formatDate(announcement.createdAt)}
                  </time>
                  <ChevronRight aria-hidden="true" className="col-start-2 row-span-2 row-start-1 size-4 text-muted-foreground/65 group-hover:text-primary sm:col-start-3 sm:row-span-1" />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      {data && search === deferredSearch && data.totalPages > 1 && (
        <AnnouncementPagination page={data.number} totalPages={data.totalPages} onChange={changePage} />
      )}
    </main>
  );
}

function AnnouncementPagination({ page, totalPages, onChange }: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  const pages = [...new Set([0, page - 1, page, page + 1, totalPages - 1])]
    .filter((number) => number >= 0 && number < totalPages)
    .sort((first, second) => first - second);
  return (
    <nav aria-label="공지 페이지 이동" className="mx-auto mt-4 flex w-fit max-w-full flex-wrap items-center justify-center gap-1 rounded-lg border bg-card p-1">
      <Button type="button" variant="ghost" size="icon-sm" aria-label="이전 페이지" disabled={page === 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft aria-hidden="true" />
      </Button>
      {pages.map((number, index) => (
        <Fragment key={number}>
          {index > 0 && number - pages[index - 1] > 1 && <span aria-hidden="true" className="text-xs text-muted-foreground">…</span>}
          <Button
            type="button"
            variant={number === page ? "default" : "ghost"}
            size="icon-sm"
            aria-label={`${number + 1} 페이지`}
            aria-current={number === page ? "page" : undefined}
            onClick={() => onChange(number)}
          >
            {number + 1}
          </Button>
        </Fragment>
      ))}
      <Button type="button" variant="ghost" size="icon-sm" aria-label="다음 페이지" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        <ChevronRight aria-hidden="true" />
      </Button>
    </nav>
  );
}
