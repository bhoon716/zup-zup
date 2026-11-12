"use client";

import { Fragment, useRef, useState } from "react";
import Link from "next/link";
import { Bell, CheckCheck, ChevronLeft, ChevronRight, Mail, MailOpen, RefreshCw, Search, Trash2 } from "lucide-react";
import { format } from "date-fns";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import { parseCourseKey } from "@/shared/lib/course";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle } from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/ui/select";
import { inboxSortSchema, inboxTypeFilterSchema, type InboxCommand, type InboxItem, type InboxPage, type InboxReadFilter, type InboxSelection } from "../api/inbox.api";
import { useInbox, useUpdateInbox } from "../hooks/useInbox";
import { getInboxItemTitle, inboxTypeLabels } from "../lib/inbox-display";

const readFilters = [
  { value: "ALL", label: "전체" },
  { value: "UNREAD", label: "읽지 않음" },
  { value: "READ", label: "읽음" },
] satisfies Array<{ value: InboxReadFilter; label: string }>;

const sortLabels: Record<InboxSelection["sort"], string> = {
  NEWEST: "최신순",
  OLDEST: "오래된 순",
  UNREAD_FIRST: "읽지 않은 알림 먼저",
};

const inboxCountFormat = new Intl.NumberFormat("ko-KR");
const deliveryFailureAction = "알림 설정에서 수신처를 확인해 주세요.";
const deliveryFailureMessage = `알림을 보내지 못했습니다. ${deliveryFailureAction}`;
const noticePreviews = new Map<string, string | null>([
  ["문의에 새 답변이 등록됐습니다. 문의 상세에서 답변을 확인해 주세요.", "문의 상세에서 답변을 확인해 주세요."],
  ["줍줍에서 문의 답변을 확인해 주세요.", "문의 답변을 확인해 주세요."],
  ["공지 상세 화면에서 안내 내용을 확인할 수 있습니다.", null],
  ["줍줍에서 중요 공지 내용을 확인해 주세요.", null],
  ["웹 푸시를 받을 수 있는 기기가 없습니다. 알림 설정에서 기기를 다시 등록해 주세요.", "알림 설정에서 기기를 다시 등록해 주세요."],
  ["웹 푸시를 받을 수 있는 기기가 없습니다. 알림 설정에서 기기를 등록해 주세요.", "알림 설정에서 기기를 등록해 주세요."],
]);

const updateErrors: Record<InboxCommand["kind"], string> = {
  read: "알림을 읽음으로 표시하지 못했습니다.",
  readAll: "모든 알림을 읽음으로 표시하지 못했습니다.",
  delete: "알림을 삭제하지 못했습니다.",
};

type MessageItem = Exclude<InboxItem, { type: "SEAT_OPENED" }>;

function getMessagePreview(item: MessageItem) {
  const { message, courseName } = item;
  if (!message) return null;

  if (item.type === "DELIVERY_FAILED" &&
      (message === deliveryFailureMessage || (courseName && message === `${courseName} ${deliveryFailureMessage}`))) {
    return courseName ? `${courseName} · ${deliveryFailureAction}` : deliveryFailureAction;
  }
  const preview = noticePreviews.get(message);
  return preview === undefined ? message : preview;
}

function getMessageLink(item: MessageItem) {
  switch (item.type) {
    case "FEEDBACK_REPLY":
      return item.available && item.targetId ? { href: `/feedback/${item.targetId}`, label: "문의 보기" } : null;
    case "ANNOUNCEMENT":
      return item.targetId ? { href: `/announcements/${item.targetId}`, label: "공지 보기" } : null;
    case "DELIVERY_FAILED":
    case "DEVICE_REREGISTRATION":
      return { href: "/settings", label: "알림 설정 열기" };
  }
}

export function InboxList({ userId }: { userId: number }) {
  const [selection, setSelection] = useState<InboxSelection>({ page: 0, read: "ALL", type: "ALL", keyword: "", sort: "NEWEST" });
  const [previousResults, setPreviousResults] = useState<InboxPage>();
  const [searchText, setSearchText] = useState("");
  const [detail, setDetail] = useState<{ item: InboxItem; open: boolean }>();
  const detailTrigger = useRef<HTMLButtonElement>(null);
  const refreshButton = useRef<HTMLButtonElement>(null);
  const toolbar = useRef<HTMLDivElement>(null);
  const inbox = useInbox(userId, selection);
  const update = useUpdateInbox(userId);
  const results = inbox.data ?? previousResults;
  const isShowingPreviousResults = !inbox.data && !!previousResults;
  const failedCommand = update.isError ? update.variables : undefined;
  const detailItem = detail?.item;
  const isDetailOpen = detail?.open ?? false;
  const messageItem = detailItem && detailItem.type !== "SEAT_OPENED" ? detailItem : null;
  const messageLink = messageItem ? getMessageLink(messageItem) : null;
  const failedDetailRead = isDetailOpen && messageItem && failedCommand?.kind === "read" && failedCommand.id === messageItem.id;
  const commandError = failedCommand && (
    <div className="flex flex-wrap items-center gap-2 bg-red-50 px-3 py-2 text-sm text-red-800 sm:px-5">
      <p role="alert">{updateErrors[failedCommand.kind]}</p>
      <Button type="button" variant="outline" size="sm" onClick={() => update.mutate(failedCommand)}>다시 시도</Button>
    </div>
  );

  if (inbox.data && !inbox.isFetching && !inbox.isError) {
    const lastPage = Math.max(0, inbox.data.totalPages - 1);
    if (selection.page > lastPage) setSelection({ ...selection, page: lastPage });
  }

  const changeSelection = (next: InboxSelection) => {
    setPreviousResults(results);
    setSelection(next);
  };

  const openItem = (item: InboxItem, trigger: HTMLButtonElement) => {
    detailTrigger.current = trigger;
    setDetail({ item, open: true });
    if (item.readAt === null) update.mutate({ kind: "read", id: item.id });
  };

  const changeDetailOpen = (open: boolean) => {
    if (!open && !detailTrigger.current?.isConnected) detailTrigger.current = refreshButton.current;
    setDetail((current) => current && { ...current, open });
  };

  const refresh = () => { void inbox.refetch({ cancelRefetch: false }); };
  const changePage = (page: number) => {
    if (page === selection.page) return;
    changeSelection({ ...selection, page });
    toolbar.current?.focus({ preventScroll: true });
    toolbar.current?.scrollIntoView({ block: "start" });
  };

  return (
    <div>
      <div
        ref={toolbar}
        role="group"
        aria-label="알림 목록 도구"
        tabIndex={-1}
        className="space-y-2 scroll-mt-20 border-b border-slate-200 px-3 py-3 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-primary sm:px-5"
      >
        <div className="grid grid-cols-[auto_minmax(0,1fr)] gap-2 sm:grid-cols-[auto_minmax(0,1fr)_auto]">
          <Select
            value={selection.type}
            onValueChange={(value) => changeSelection({ ...selection, type: inboxTypeFilterSchema.parse(value), page: 0 })}
          >
            <SelectTrigger aria-label="알림 유형" className="rounded-lg bg-white px-2.5 sm:w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="start" className="rounded-xl border-slate-200 shadow-lg">
              <SelectItem value="ALL">모든 알림 유형</SelectItem>
              {Object.entries(inboxTypeLabels).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>

          <form
            role="search"
            aria-label="알림함 검색"
            className="flex min-w-0 gap-1.5"
            onSubmit={(event) => {
              event.preventDefault();
              const keyword = searchText.trim();
              if (selection.keyword !== keyword || selection.page !== 0) {
                changeSelection({ ...selection, keyword, page: 0 });
              } else {
                refresh();
              }
            }}
          >
            <div className="relative min-w-0 flex-1">
              <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-slate-500" />
              <Input type="search" aria-label="검색어" placeholder="제목, 내용, 강의명 검색" value={searchText} onChange={(event) => setSearchText(event.target.value)} className="h-9 min-w-0 rounded-lg pl-9" />
            </div>
            <Button type="submit" variant="outline" className="rounded-lg px-2.5 sm:px-4">검색</Button>
          </form>

          <Select
            value={selection.sort}
            onValueChange={(value) => changeSelection({ ...selection, sort: inboxSortSchema.parse(value), page: 0 })}
          >
            <SelectTrigger aria-label="알림 정렬" className="col-start-2 justify-self-end rounded-lg bg-white px-2.5 sm:col-start-auto sm:w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="end" className="rounded-xl border-slate-200 shadow-lg">
              {Object.entries(sortLabels).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-1">
          <div role="group" aria-label="알림 읽음 상태" className="flex gap-1">
            {readFilters.map(({ value, label }) => (
              <Button
                key={value}
                type="button"
                size="sm"
                variant={selection.read === value ? "secondary" : "ghost"}
                aria-pressed={selection.read === value}
                className="rounded-lg px-2 sm:px-3"
                onClick={() => {
                  if (value !== selection.read) {
                    changeSelection({ ...selection, read: value, page: 0 });
                  }
                }}
              >
                {label}
              </Button>
            ))}
          </div>
          <div className="ml-auto flex items-center gap-1">
            <Button ref={refreshButton} type="button" variant="ghost" size="icon-sm" aria-label="새로고침" title="새로고침" aria-disabled={inbox.isFetching} onClick={() => { if (!inbox.isFetching) refresh(); }}>
              <RefreshCw aria-hidden="true" className={inbox.isFetching ? "motion-safe:animate-spin" : undefined} />
            </Button>
            <Button type="button" variant="outline" size="sm" className="rounded-lg" disabled={update.isPending} onClick={() => update.mutate({ kind: "readAll" })}>
              <CheckCheck aria-hidden="true" />
              모두 읽음 처리
            </Button>
          </div>
        </div>
      </div>

      {inbox.isError && (
        <div className="flex flex-wrap items-center gap-2 bg-red-50 px-3 py-2 text-sm text-red-800 sm:px-5">
          <p role="alert">
            알림을 불러오지 못했습니다. {results && "이전 조회 결과를 표시하고 있습니다."}
          </p>
          <Button type="button" variant="outline" size="sm" disabled={inbox.isFetching} onClick={refresh}>다시 불러오기</Button>
        </div>
      )}
      {!failedDetailRead && commandError}

      {inbox.isPending && <p role="status" className="px-3 py-4 text-sm text-slate-500 sm:px-5">알림을 불러오는 중입니다.</p>}

      {results && results.content.length > 0 && (
        <ul aria-label="알림 목록" className="divide-y divide-slate-200">
          {results.content.map((item) => {
            const isUnread = item.readAt === null;
            const MailIcon = isUnread ? Mail : MailOpen;
            const isOpenDisabled = isShowingPreviousResults || (isUnread && update.isPending);
            const isSeatOpening = item.type === "SEAT_OPENED";
            const title = getInboxItemTitle(item);
            const message = isSeatOpening ? `빈자리가 ${item.availableSeats}석 생겼어요.` : getMessagePreview(item);
            return (
              <li key={item.id}>
                <article className={cn(
                  "relative flex items-center gap-2 px-3 py-2.5 hover:bg-slate-50 sm:gap-3 sm:px-5",
                  isUnread && "bg-primary/5 hover:bg-primary/10",
                )}>
                  <MailIcon
                    role="img"
                    aria-label={isUnread ? "읽지 않음" : "읽음"}
                    className={cn("size-[18px] shrink-0 self-start md:self-auto", isUnread ? "text-primary" : "text-slate-500")}
                  />
                  <div className="min-w-0 flex-1 md:flex md:items-center md:gap-4">
                    <div className="min-w-0 flex-1 md:flex md:items-baseline md:gap-2">
                      <h2 className={cn(
                        "min-w-0 text-sm text-slate-900",
                        isUnread ? "font-bold" : "font-medium",
                        message && "md:max-w-3/5",
                      )}>
                        <button
                          type="button"
                          aria-disabled={isOpenDisabled}
                          className="block w-full cursor-pointer truncate rounded-sm text-left after:absolute after:inset-0 after:content-[''] hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary aria-disabled:cursor-default"
                          onClick={(event) => {
                            if (isOpenDisabled) return;
                            openItem(item, event.currentTarget);
                          }}
                        >
                          {title}
                        </button>
                      </h2>
                      {message && <p className="mt-0.5 min-w-0 truncate text-sm text-slate-600 md:mt-0">{message}</p>}
                    </div>
                    <p className="mt-1 flex flex-wrap items-baseline gap-x-2 text-xs leading-tight text-slate-600 md:mt-0 md:shrink-0">
                      <span className="font-medium">{isSeatOpening ? "빈자리 감지" : inboxTypeLabels[item.type]}</span>
                      <time dateTime={item.occurredAt} className="whitespace-nowrap tabular-nums">
                        {format(new Date(item.occurredAt), "yyyy년 M월 d일 HH:mm")}
                      </time>
                    </p>
                  </div>
                  <Button
                    type="button"
                    aria-label={`${title} 알림 삭제`}
                    title="알림 삭제"
                    variant="ghost"
                    size="icon-sm"
                    disabled={isShowingPreviousResults || update.isPending}
                    className="relative z-10 text-slate-500 hover:bg-red-50 hover:text-red-700"
                    onClick={() => update.mutate({ kind: "delete", id: item.id })}
                  >
                    <Trash2 aria-hidden="true" />
                  </Button>
                </article>
              </li>
            );
          })}
        </ul>
      )}

      {inbox.data?.content.length === 0 && (
        <div className="bg-slate-50 px-4 py-12 text-center">
          <Bell aria-hidden="true" className="mx-auto mb-3 size-6 text-slate-400" />
          <p className="text-sm font-medium text-slate-700">
            {selection.keyword || selection.read !== "ALL" || selection.type !== "ALL" ? "조건에 맞는 알림이 없습니다." : "받은 알림이 없습니다."}
          </p>
        </div>
      )}

      {results && (
        <footer className="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 rounded-b-2xl border-t border-slate-200 bg-slate-50 px-3 py-2 sm:px-5">
          <p className="text-sm font-medium text-slate-600">알림 {inboxCountFormat.format(results.totalElements)}개</p>
          {results.totalPages > 1 && (
            <InboxPagination results={results} disabled={isShowingPreviousResults} onPageChange={changePage} />
          )}
        </footer>
      )}

      {detailItem?.type === "SEAT_OPENED" && (
        <CourseDetailDialog
          course={{ courseKey: detailItem.courseKey, name: detailItem.courseName, ...parseCourseKey(detailItem.courseKey) }}
          open={isDetailOpen}
          returnFocusRef={detailTrigger}
          onOpenChange={changeDetailOpen}
        />
      )}
      {messageItem && (
        <Dialog open={isDetailOpen} onOpenChange={changeDetailOpen}>
          <DialogContent
            className="max-h-[90dvh] overflow-y-auto"
            onCloseAutoFocus={(event) => {
              event.preventDefault();
              const trigger = detailTrigger.current?.isConnected ? detailTrigger.current : refreshButton.current;
              trigger?.focus();
            }}
          >
            <DialogTitle className="pr-6 leading-snug [overflow-wrap:anywhere]">{getInboxItemTitle(messageItem)}</DialogTitle>
            <DialogDescription className="whitespace-pre-wrap [overflow-wrap:anywhere]">{messageItem.message ?? inboxTypeLabels[messageItem.type]}</DialogDescription>
            {failedDetailRead && commandError}
            {messageLink && (
              <DialogFooter>
                <Button asChild><Link href={messageLink.href} prefetch={false}>{messageLink.label}</Link></Button>
              </DialogFooter>
            )}
          </DialogContent>
        </Dialog>
      )}
    </div>
  );
}

function InboxPagination({ results, disabled, onPageChange }: {
  results: InboxPage;
  disabled: boolean;
  onPageChange: (page: number) => void;
}) {
  const { number: page, totalPages } = results;

  const visiblePages = Math.min(3, totalPages);
  const firstPage = Math.max(0, Math.min(page - 1, totalPages - visiblePages));
  const nearbyPages = Array.from({ length: visiblePages }, (_, index) => firstPage + index);
  const pageNumbers = [...new Set([0, ...nearbyPages, totalPages - 1])];

  return (
    <nav aria-label="알림 목록 페이지 이동" className="ml-auto flex items-center gap-0.5">
      <Button type="button" variant="ghost" size="icon-sm" aria-label="이전 페이지" disabled={disabled || page === 0} onClick={() => onPageChange(page - 1)}>
        <ChevronLeft aria-hidden="true" />
      </Button>
      {pageNumbers.map((number, index) => (
        <Fragment key={number}>
          {index > 0 && number - pageNumbers[index - 1] > 1 && <span aria-hidden="true" className="text-xs text-slate-500">…</span>}
          <Button
            type="button"
            variant={number === page ? "default" : "ghost"}
            size="icon-sm"
            aria-label={`${number + 1} 페이지`}
            aria-current={number === page ? "page" : undefined}
            disabled={disabled}
            onClick={() => onPageChange(number)}
          >
            {number + 1}
          </Button>
        </Fragment>
      ))}
      <Button type="button" variant="ghost" size="icon-sm" aria-label="다음 페이지" disabled={disabled || page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
        <ChevronRight aria-hidden="true" />
      </Button>
    </nav>
  );
}
