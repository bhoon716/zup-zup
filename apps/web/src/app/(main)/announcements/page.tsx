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
    <main className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
      <header className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">공지사항</h1>

        <form
          ref={listTop}
          role="search"
          aria-label="공지사항 검색"
          tabIndex={-1}
          className="flex w-full scroll-mt-20 gap-2 rounded-lg focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-primary sm:w-auto"
          onSubmit={(event) => event.preventDefault()}
        >
          <Select
            value={search.searchType}
            onValueChange={(value) => {
              const option = searchOptions.find((candidate) => candidate.value === value);
              if (option) setSearch({ ...search, searchType: option.value, page: 0 });
            }}
          >
            <SelectTrigger aria-label="검색 기준" className="rounded-lg bg-card">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="start">
              {searchOptions.map(({ value, label }) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <div className="relative min-w-0 flex-1 sm:w-56">
            <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              type="search"
              aria-label="공지 검색어"
              value={search.keyword}
              maxLength={100}
              onChange={(event) => setSearch({ ...search, keyword: event.target.value, page: 0 })}
              placeholder="공지 검색"
              className="rounded-lg bg-card pl-9 text-sm"
            />
          </div>
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
                  className="group grid grid-cols-[auto_minmax(0,1fr)] items-start gap-x-2.5 gap-y-1 px-3 py-3.5 hover:bg-muted/70 focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-primary sm:grid-cols-[auto_minmax(0,1fr)_auto] sm:px-6"
                >
                  <span className="w-3.5 pt-1">
                    {announcement.pinned && (
                      <>
                        <Pin aria-hidden="true" className="size-3.5 text-muted-foreground" />
                        <span className="sr-only">상단 고정</span>
                      </>
                    )}
                  </span>
                  <div>
                    <h2 className="text-sm font-semibold wrap-anywhere break-keep text-foreground group-hover:text-primary sm:text-base">{announcement.title}</h2>
                    {announcement.previewContent && <p className="mt-0.5 truncate text-sm leading-relaxed text-muted-foreground">{announcement.previewContent}</p>}
                  </div>
                  <time dateTime={announcement.createdAt} className="col-start-2 whitespace-nowrap text-xs tabular-nums text-muted-foreground sm:col-start-3 sm:mt-1">
                    {formatDate(announcement.createdAt)}
                  </time>
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
    <nav aria-label="공지 페이지 이동" className="mt-5 flex flex-wrap items-center justify-center gap-1">
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
