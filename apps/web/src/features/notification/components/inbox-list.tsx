"use client";

import { Fragment, useRef, useState, type Ref } from "react";
import Link from "next/link";
import { Bell, CheckCheck, ChevronLeft, ChevronRight, RefreshCw, Search, Trash2 } from "lucide-react";
import { format } from "date-fns";
import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import { parseCourseKey } from "@/shared/lib/course";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogTitle } from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/shared/ui/select";
import { inboxSortSchema, inboxTypeFilterSchema, type InboxCommand, type InboxItem, type InboxPage, type InboxReadFilter, type InboxSelection } from "../api/inbox.api";
import { useInbox, useUpdateInbox } from "../hooks/useInbox";

const readFilters = [
  { value: "ALL", label: "전체" },
  { value: "UNREAD", label: "읽지 않음" },
  { value: "READ", label: "읽음" },
] satisfies Array<{ value: InboxReadFilter; label: string }>;

const typeLabels: Record<InboxItem["type"], string> = {
  SEAT_OPENED: "빈자리",
  FEEDBACK_REPLY: "문의 답변",
  ANNOUNCEMENT: "중요 공지",
  DELIVERY_FAILED: "발송 실패",
  DEVICE_REREGISTRATION: "기기 재등록",
};

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
  const topPagination = useRef<HTMLElement>(null);
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
    <div className="flex flex-wrap items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-800">
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
    if (page !== selection.page) changeSelection({ ...selection, page });
  };

  return (
    <div className="space-y-5">
      <div className="grid gap-2 md:grid-cols-[minmax(0,1fr)_auto]">
        <form
          role="search"
          aria-label="알림함 검색"
          className="flex gap-2"
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
            <Input type="search" aria-label="검색어" placeholder="제목, 내용, 강의명 검색" value={searchText} onChange={(event) => setSearchText(event.target.value)} className="h-10 min-w-0 rounded-xl pl-9" />
          </div>
          <Button type="submit" variant="outline" className="h-10 rounded-xl">검색</Button>
        </form>
        <div className="grid grid-cols-2 gap-2 md:flex">
          <Select
            value={selection.type}
            onValueChange={(value) => changeSelection({ ...selection, type: inboxTypeFilterSchema.parse(value), page: 0 })}
          >
            <SelectTrigger aria-label="알림 유형" className="data-[size=default]:h-10 w-full rounded-xl bg-white md:w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="start" className="rounded-xl border-slate-200 shadow-lg">
              <SelectItem value="ALL">모든 알림 유형</SelectItem>
              {Object.entries(typeLabels).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
          <Select
            value={selection.sort}
            onValueChange={(value) => changeSelection({ ...selection, sort: inboxSortSchema.parse(value), page: 0 })}
          >
            <SelectTrigger aria-label="알림 정렬" className="data-[size=default]:h-10 w-full rounded-xl bg-white md:w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent position="popper" align="start" className="rounded-xl border-slate-200 shadow-lg">
              {Object.entries(sortLabels).map(([value, label]) => <SelectItem key={value} value={value}>{label}</SelectItem>)}
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2">
        <div role="group" aria-label="알림 읽음 상태" className="flex gap-1">
          {readFilters.map(({ value, label }) => (
            <Button
              key={value}
              type="button"
              size="sm"
              variant={selection.read === value ? "secondary" : "ghost"}
              aria-pressed={selection.read === value}
              className="h-9 rounded-lg"
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
        <div className="flex items-center gap-1">
          <Button ref={refreshButton} type="button" variant="ghost" size="icon" aria-label="새로고침" title="새로고침" aria-disabled={inbox.isFetching} onClick={() => { if (!inbox.isFetching) refresh(); }}>
            <RefreshCw aria-hidden="true" className={inbox.isFetching ? "motion-safe:animate-spin" : undefined} />
          </Button>
          <Button type="button" variant="outline" size="sm" className="h-9 rounded-lg" disabled={update.isPending} onClick={() => update.mutate({ kind: "readAll" })}>
            <CheckCheck aria-hidden="true" />
            모두 읽음 처리
          </Button>
        </div>
      </div>

      {inbox.isError && (
        <div className="flex flex-wrap items-center gap-2 rounded-xl bg-red-50 px-3 py-2 text-sm text-red-800">
          <p role="alert">
            알림을 불러오지 못했습니다. {results && "이전 조회 결과를 표시하고 있습니다."}
          </p>
          <Button type="button" variant="outline" size="sm" disabled={inbox.isFetching} onClick={refresh}>다시 불러오기</Button>
        </div>
      )}
      {!failedDetailRead && commandError}

      {inbox.isPending && <p role="status" className="text-sm text-slate-500">알림을 불러오는 중입니다.</p>}

      {results && (
        <InboxPagination
          position="top"
          navRef={topPagination}
          results={results}
          disabled={isShowingPreviousResults}
          onPageChange={changePage}
        />
      )}

      {results && results.content.length > 0 && (
        <ul aria-label="알림 목록" className="space-y-2">
          {results.content.map((item) => {
            const isUnread = item.readAt === null;
            const isOpenDisabled = isShowingPreviousResults || (isUnread && update.isPending);
            const title = item.type === "SEAT_OPENED" ? item.courseName : item.title ?? typeLabels[item.type];
            const isSeatOpening = item.type === "SEAT_OPENED";
            const message = isSeatOpening ? `빈자리가 ${item.availableSeats}석 생겼어요.` : getMessagePreview(item);
            return (
              <li key={item.id}>
                <article className={`relative flex items-start gap-2 rounded-xl border p-3 sm:p-4 ${isUnread ? "border-primary/20 bg-primary/5" : "border-slate-200 bg-white"}`}>
                  <div className="min-w-0 flex-1">
                    <p className="mb-1 text-xs font-medium text-slate-600">{typeLabels[item.type]}</p>
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                      <h2 className="min-w-0 font-bold text-slate-900">
                        <button
                          type="button"
                          aria-disabled={isOpenDisabled}
                          className="cursor-pointer rounded-sm text-left [overflow-wrap:anywhere] after:absolute after:inset-0 after:rounded-xl after:content-[''] hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary aria-disabled:cursor-default"
                          onClick={(event) => {
                            if (isOpenDisabled) return;
                            openItem(item, event.currentTarget);
                          }}
                        >
                          {title}
                        </button>
                      </h2>
                      <Badge variant={isUnread ? "default" : "secondary"}>{isUnread ? "읽지 않음" : "읽음"}</Badge>
                    </div>
                    {message && <p className="mt-1 text-sm text-slate-700 [overflow-wrap:anywhere]">{message}</p>}
                    <p className="mt-2 text-xs text-slate-600">
                      {isSeatOpening && <span className="mr-2 font-medium">빈자리 발생</span>}
                      <time dateTime={item.occurredAt}>{format(new Date(item.occurredAt), "yyyy년 M월 d일 HH:mm")}</time>
                    </p>
                  </div>
                  <Button
                    type="button"
                    aria-label={`${title} 알림 삭제`}
                    title="알림 삭제"
                    variant="ghost"
                    size="icon"
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
        <div className="rounded-xl bg-slate-50 px-4 py-12 text-center">
          <Bell aria-hidden="true" className="mx-auto mb-3 size-6 text-slate-400" />
          <p className="text-sm font-medium text-slate-700">
            {selection.keyword || selection.read !== "ALL" || selection.type !== "ALL" ? "조건에 맞는 알림이 없습니다." : "받은 알림이 없습니다."}
          </p>
        </div>
      )}

      {results && results.totalPages > 1 && (
        <InboxPagination
          position="bottom"
          results={results}
          disabled={isShowingPreviousResults}
          onPageChange={(page) => {
            if (page === selection.page) return;
            changePage(page);
            topPagination.current?.focus({ preventScroll: true });
            topPagination.current?.scrollIntoView({ block: "start" });
          }}
        />
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
            <DialogTitle className="pr-6 leading-snug [overflow-wrap:anywhere]">{messageItem.title ?? typeLabels[messageItem.type]}</DialogTitle>
            <DialogDescription className="whitespace-pre-wrap [overflow-wrap:anywhere]">{messageItem.message ?? typeLabels[messageItem.type]}</DialogDescription>
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

function InboxPagination({ position, results, disabled, onPageChange, navRef }: {
  position: "top" | "bottom";
  results: InboxPage;
  disabled: boolean;
  onPageChange: (page: number) => void;
  navRef?: Ref<HTMLElement>;
}) {
  const { number: page, totalPages, totalElements } = results;
  const total = <p className="text-sm font-medium text-slate-600">알림 {inboxCountFormat.format(totalElements)}개</p>;
  if (totalPages <= 1) return total;

  const visiblePages = Math.min(3, totalPages);
  const firstPage = Math.max(0, Math.min(page - 1, totalPages - visiblePages));
  const nearbyPages = Array.from({ length: visiblePages }, (_, index) => firstPage + index);
  const pageNumbers = [...new Set([0, ...nearbyPages, totalPages - 1])];

  return (
    <nav
      ref={navRef}
      aria-label={position === "top" ? "알림 목록 위 페이지 이동" : "알림 목록 아래 페이지 이동"}
      tabIndex={position === "top" ? -1 : undefined}
      className="flex scroll-mt-24 flex-wrap items-center justify-between gap-x-3 gap-y-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
    >
      {total}
      <div className="ml-auto flex items-center gap-0.5">
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
      </div>
    </nav>
  );
}
