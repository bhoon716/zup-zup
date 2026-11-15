"use client";

import { useEffect, useRef, useState } from "react";
import { ArrowLeft, Download, Loader2, MessageSquare, X } from "lucide-react";
import { format } from "date-fns";
import { toast } from "sonner";

import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/shared/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { FeedbackPagination } from "@/features/feedback/components/feedback-pagination";
import {
  useFeedbacksForAdmin,
  useAdminFeedbackDetail,
  useUpdateFeedbackStatus,
  useCreateFeedbackReply,
  useUpdateFeedbackReply,
  useDeleteFeedbackReply,
  useAdminFeedbackAttachmentDownload,
} from "@/features/feedback/hooks/useFeedback";
import type {
  AdminFeedbackDeletionFilter,
  AdminFeedbackOrder,
  AdminFeedbackSearch,
  AdminFeedbackSearchType,
  FeedbackStatus,
  FeedbackType,
} from "@/shared/types/api";
import { isCsrfFailure } from "@/shared/api/auth-error";

const PREVIEW_FILE_EXTENSIONS: Record<string, string> = {
  "image/jpeg": "jpg",
  "image/png": "png",
};

const DELETION_OPTIONS = [
  { value: "ACTIVE", label: "삭제 제외" },
  { value: "ALL", label: "전체" },
  { value: "DELETED", label: "삭제된 문의" },
] satisfies { value: AdminFeedbackDeletionFilter; label: string }[];

const ORDER_OPTIONS = [
  { value: "PENDING_FIRST", label: "처리 대기 우선" },
  { value: "NEWEST", label: "최신순" },
  { value: "OLDEST", label: "오래된순" },
] satisfies { value: AdminFeedbackOrder; label: string }[];

const SEARCH_OPTIONS = [
  { value: "TITLE", label: "제목" },
  { value: "TITLE_CONTENT", label: "제목 + 본문" },
  { value: "TITLE_CONTENT_REPLY", label: "제목 + 본문 + 답변" },
] satisfies { value: AdminFeedbackSearchType; label: string }[];

const STATUS_OPTIONS = [
  { value: "PENDING", label: "처리 대기", textColor: "text-amber-800" },
  { value: "IN_PROGRESS", label: "처리 중", textColor: "text-slate-600" },
  { value: "COMPLETED", label: "처리 완료", textColor: "text-slate-600" },
  { value: "REJECTED", label: "반려", textColor: "text-slate-600" },
] satisfies { value: FeedbackStatus; label: string; textColor: string }[];

const TYPE_LABELS: Record<FeedbackType, string> = {
  BUG: "버그",
  SUGGESTION: "건의",
  OTHER: "기타",
};

type AttachmentPreview = {
  label: number;
  objectUrl: string;
  filename: string;
};

export default function AdminFeedbackPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [deletionFilter, setDeletionFilter] = useState<AdminFeedbackDeletionFilter>("ACTIVE");
  const [order, setOrder] = useState<AdminFeedbackOrder>("PENDING_FIRST");
  const [page, setPage] = useState(0);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [search, setSearch] = useState<AdminFeedbackSearch>({ keyword: "", searchType: "TITLE_CONTENT" });
  const [replyContent, setReplyContent] = useState("");
  const [isReplySubmitting, setIsReplySubmitting] = useState(false);
  const [editingReplyId, setEditingReplyId] = useState<number | null>(null);
  const [attachmentPreview, setAttachmentPreview] = useState<AttachmentPreview | null>(null);
  const selectedFeedbackIdRef = useRef<number | null>(null);
  const isMountedRef = useRef(true);
  const listHeadingRef = useRef<HTMLHeadingElement>(null);
  const detailPaneRef = useRef<HTMLElement>(null);
  const selectionTriggerRef = useRef<HTMLButtonElement>(null);
  const keywordInputRef = useRef<HTMLInputElement>(null);

  const listQuery = useFeedbacksForAdmin(page, deletionFilter, order, search.keyword ? search : undefined);
  const detailQuery = useAdminFeedbackDetail(selectedId ?? 0, selectedId !== null);
  const listData = listQuery.data;
  const detailData = detailQuery.data;
  const totalPages = listData?.totalPages ?? 0;

  if (listData && !listQuery.isFetching && !listQuery.isError) {
    const lastPage = Math.max(0, totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }

  const updateStatusMutation = useUpdateFeedbackStatus();
  const createReplyMutation = useCreateFeedbackReply();
  const updateReplyMutation = useUpdateFeedbackReply();
  const deleteReplyMutation = useDeleteFeedbackReply();
  const downloadAttachmentMutation = useAdminFeedbackAttachmentDownload();

  useEffect(() => {
    return () => {
      if (attachmentPreview) {
        URL.revokeObjectURL(attachmentPreview.objectUrl);
      }
    };
  }, [attachmentPreview]);

  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!window.matchMedia?.("(width < 768px)").matches) return;
    if (selectedId !== null) {
      detailPaneRef.current?.focus();
    } else {
      selectionTriggerRef.current?.focus();
    }
  }, [selectedId]);

  const resetReplyDraft = () => {
    setEditingReplyId(null);
    setReplyContent("");
  };

  const handleFeedbackSelection = (feedbackId: number | null) => {
    selectedFeedbackIdRef.current = feedbackId;
    setSelectedId(feedbackId);
    setAttachmentPreview(null);
    resetReplyDraft();
  };

  const handleDeletionFilterChange = (value: string) => {
    const option = DELETION_OPTIONS.find(({ value: optionValue }) => optionValue === value);
    if (!option) return;
    setDeletionFilter(option.value);
    setPage(0);
    handleFeedbackSelection(null);
  };

  const handleOrderChange = (value: string) => {
    const option = ORDER_OPTIONS.find(({ value: optionValue }) => optionValue === value);
    if (!option) return;
    setOrder(option.value);
    setPage(0);
    handleFeedbackSelection(null);
  };

  const handlePageChange = (nextPage: number) => {
    if (nextPage === page) return;
    setPage(nextPage);
    handleFeedbackSelection(null);
    listHeadingRef.current?.focus();
  };

  const applySearch = (nextSearch: AdminFeedbackSearch) => {
    if (nextSearch.keyword === search.keyword && nextSearch.searchType === search.searchType) return;
    setSearch(nextSearch);
    setPage(0);
    handleFeedbackSelection(null);
  };

  const handleStatusChange = async (value: string) => {
    const option = STATUS_OPTIONS.find(({ value: status }) => status === value);
    if (!selectedId || !option) return;
    try {
      await updateStatusMutation.mutateAsync({ id: selectedId, request: { status: option.value } });
      toast.success("상태가 변경되었습니다.");
    } catch {
      toast.error("변경에 실패했습니다.");
    }
  };

  const handleReplySubmit = async () => {
    if (!selectedId || !replyContent.trim()) return;
    setIsReplySubmitting(true);
    try {
      if (editingReplyId) {
        await updateReplyMutation.mutateAsync({ 
          replyId: editingReplyId, 
          feedbackId: selectedId, 
          request: { content: replyContent } 
        });
        toast.success("답변 수정 완료");
      } else {
        await createReplyMutation.mutateAsync({ 
          id: selectedId, 
          request: { content: replyContent } 
        });
        toast.success("답변 등록 완료");
      }
      if (selectedFeedbackIdRef.current === selectedId) {
        resetReplyDraft();
      }
    } catch {
      toast.error("답변 처리 실패");
    } finally {
      setIsReplySubmitting(false);
    }
  };

  const handleReplyDelete = async (replyId: number) => {
    if (!selectedId || !window.confirm("정말 이 답변을 삭제하시겠습니까?")) return;
    try {
      await deleteReplyMutation.mutateAsync({ replyId, feedbackId: selectedId });
      toast.success("답변이 삭제되었습니다.");
    } catch {
      toast.error("답변 삭제 실패");
    }
  };

  const startEditReply = (id: number, content: string) => {
    setEditingReplyId(id);
    setReplyContent(content);
  };

  const handleAttachmentDownload = async (attachmentId: number, label: number) => {
    const feedbackId = selectedId;
    if (!feedbackId || !window.confirm("보존된 첨부파일을 열람하시겠습니까? 접근 기록이 남습니다.")) return;
    try {
      const blob = await downloadAttachmentMutation.mutateAsync({ feedbackId, attachmentId });
      if (!isMountedRef.current || selectedFeedbackIdRef.current !== feedbackId) {
        return;
      }
      const objectUrl = URL.createObjectURL(blob);
      const contentType = blob.type.split(";", 1)[0].toLowerCase();
      const extension = PREVIEW_FILE_EXTENSIONS[contentType];
      const filename = `feedback-attachment-${attachmentId}${extension ? `.${extension}` : ""}`;
      if (extension) {
        setAttachmentPreview({ label, objectUrl, filename });
        return;
      }

      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1_000);
    } catch (error) {
      toast.error(isCsrfFailure(error)
        ? "로그인 상태가 변경되었습니다. 다시 시도해 주세요."
        : "첨부파일을 열람할 수 없습니다.");
    }
  };

  const canResetSearch = Boolean(keywordDraft || search.keyword);

  return (
    <main className="mx-auto flex max-w-7xl flex-col gap-3 px-3 py-4 sm:px-5 md:h-[calc(100dvh-4rem)] md:min-h-[32rem]">
      <Dialog open={attachmentPreview !== null} onOpenChange={(open) => !open && setAttachmentPreview(null)}>
        <DialogContent className="max-h-[90vh] max-w-4xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>첨부파일 미리보기</DialogTitle>
            <DialogDescription>인증된 요청으로 받은 정규화 첨부파일입니다.</DialogDescription>
          </DialogHeader>
          {attachmentPreview && (
            <div className="space-y-4">
              {/* eslint-disable-next-line @next/next/no-img-element -- authenticated Blob URLs are not available to Next Image. */}
              <img
                src={attachmentPreview.objectUrl}
                alt={`첨부파일 ${attachmentPreview.label} 미리보기`}
                className="max-h-[65vh] w-full rounded-lg border border-gray-200 object-contain dark:border-gray-800"
              />
              <Button asChild className="w-full sm:w-auto">
                <a href={attachmentPreview.objectUrl} download={attachmentPreview.filename}>다운로드</a>
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
      <h1 className="text-xl font-semibold tracking-tight text-slate-900">문의 관리</h1>

      <div className="grid min-h-0 flex-1 md:grid-cols-[minmax(17rem,1fr)_minmax(0,2fr)] md:gap-3">
        <section
          aria-label="문의 목록"
          className={`min-h-0 min-w-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white ${selectedId === null ? "flex" : "hidden md:flex"}`}
        >
          <div className="border-b border-slate-200 px-3 py-2.5">
            <h2 ref={listHeadingRef} tabIndex={-1} className="mb-2 text-sm font-semibold text-slate-900">
              문의 목록 {listData && <span className="ml-1 font-normal text-slate-500">{listData.totalElements}건</span>}
            </h2>
            <form
              role="search"
              aria-label="문의 검색"
              className="mb-2 flex items-center gap-2"
              onSubmit={(event) => {
                event.preventDefault();
                applySearch({ ...search, keyword: keywordDraft.trim() });
              }}
            >
              <Select
                value={search.searchType}
                onValueChange={(value) => {
                  const option = SEARCH_OPTIONS.find((candidate) => candidate.value === value);
                  if (option) applySearch({ ...search, searchType: option.value });
                }}
              >
                <SelectTrigger aria-label="검색 범위" size="sm" className="shrink-0 gap-1 px-2 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent position="popper" align="start">
                  {SEARCH_OPTIONS.map(({ value, label }) => (
                    <SelectItem key={value} value={value}>{label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <div className="relative min-w-0 flex-1">
                <Input
                  ref={keywordInputRef}
                  type="search"
                  aria-label="문의 검색어"
                  placeholder="검색어 입력"
                  maxLength={100}
                  value={keywordDraft}
                  onChange={(event) => setKeywordDraft(event.target.value)}
                  className={`h-8 px-2 text-xs md:text-xs [&::-webkit-search-cancel-button]:hidden ${canResetSearch ? "pr-7" : ""}`}
                />
                {canResetSearch && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon-xs"
                    aria-label="초기화"
                    title="검색어 초기화"
                    className="absolute inset-y-1 right-1 text-slate-600"
                    onClick={() => {
                      setKeywordDraft("");
                      applySearch({ ...search, keyword: "" });
                      keywordInputRef.current?.focus();
                    }}
                  >
                    <X aria-hidden="true" />
                  </Button>
                )}
              </div>
              <Button type="submit" size="sm" className="px-2.5 text-xs">검색</Button>
            </form>
            <div className="grid grid-cols-2 gap-2">
              <div className="min-w-0">
                <label htmlFor="feedback-deletion" className="mb-1 block text-xs text-slate-600">표시</label>
                <Select value={deletionFilter} onValueChange={handleDeletionFilterChange}>
                  <SelectTrigger id="feedback-deletion" size="sm" className="w-full text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {DELETION_OPTIONS.map(({ value, label }) => (
                      <SelectItem key={value} value={value}>{label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="min-w-0">
                <label htmlFor="feedback-order" className="mb-1 block text-xs text-slate-600">정렬</label>
                <Select value={order} onValueChange={handleOrderChange}>
                  <SelectTrigger id="feedback-order" size="sm" className="w-full text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ORDER_OPTIONS.map(({ value, label }) => (
                      <SelectItem key={value} value={value}>{label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
          <div key={`${deletionFilter}-${order}-${page}-${search.searchType}-${search.keyword}`} className="min-h-0 flex-1 md:overflow-y-auto">
            {listQuery.isLoading ? (
              <div role="status" className="flex items-center justify-center gap-2 p-5 text-sm text-slate-600">
                <Loader2 className="size-4 animate-spin" /> 문의를 불러오는 중
              </div>
            ) : listQuery.isError ? (
              <div className="p-4 text-center text-sm text-slate-600">
                <p>문의 목록을 불러오지 못했습니다.</p>
                <Button variant="outline" size="sm" className="mt-2" onClick={() => void listQuery.refetch()}>다시 시도</Button>
              </div>
            ) : listData?.content.length ? (
              <ul className="divide-y divide-slate-100">
                {listData.content.map((item) => {
                  const status = STATUS_OPTIONS.find(({ value }) => value === item.status);
                  const isSelected = selectedId === item.id;
                  return (
                    <li key={item.id}>
                      <button
                        type="button"
                        aria-current={isSelected ? "true" : undefined}
                        onClick={(event) => {
                          selectionTriggerRef.current = event.currentTarget;
                          handleFeedbackSelection(item.id);
                        }}
                        className={`flex w-full cursor-pointer flex-col gap-1 px-3 py-2.5 text-left transition-colors focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary ${isSelected ? "bg-slate-100" : "hover:bg-slate-50"}`}
                      >
                        <span className="flex w-full items-baseline justify-between gap-2">
                          <span className="truncate text-sm font-semibold text-slate-900">{item.title}</span>
                          {status && <span className={`shrink-0 text-xs ${status.textColor}`}>{status.label}</span>}
                        </span>
                        <span className="flex w-full items-baseline gap-x-1.5 text-xs text-slate-600">
                          <span className="shrink-0">{TYPE_LABELS[item.type]}</span>
                          <span aria-hidden="true" className="text-slate-400">·</span>
                          <span className="truncate">{item.authorLabel}</span>
                          {item.deleted && <span className="shrink-0">삭제됨</span>}
                          <time dateTime={item.createdAt} className="ml-auto shrink-0 tabular-nums text-slate-500">
                            {format(new Date(item.createdAt), "MM.dd HH:mm")}
                          </time>
                        </span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className="p-5 text-center text-sm text-slate-600">
                {search.keyword ? "검색 결과가 없습니다." : "표시할 문의가 없습니다."}
              </p>
            )}
          </div>
          {(totalPages > 1 || page > 0) && (
            <FeedbackPagination
              page={page}
              totalPages={totalPages}
              onChange={handlePageChange}
              className="gap-0.5 border-t border-slate-200 px-1 py-1.5"
            />
          )}
        </section>

        <section
          ref={detailPaneRef}
          aria-label="문의 상세"
          tabIndex={-1}
          className={`min-h-0 min-w-0 flex-col overflow-hidden rounded-xl border border-slate-200 bg-white ${selectedId === null ? "hidden md:flex" : "flex"}`}
        >
          {selectedId !== null && (
            <div className="border-b border-slate-100 px-2 py-1 md:hidden">
              <Button variant="ghost" size="sm" onClick={() => handleFeedbackSelection(null)}>
                <ArrowLeft className="size-4" /> 문의 목록
              </Button>
            </div>
          )}
          {selectedId === null ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 p-4 text-center text-sm text-slate-500">
              <MessageSquare className="size-6" />
              <p>목록에서 문의를 선택해 주세요.</p>
            </div>
          ) : detailQuery.isLoading ? (
            <div role="status" className="flex flex-1 items-center justify-center gap-2 p-5 text-sm text-slate-600">
              <Loader2 className="size-4 animate-spin" /> 문의를 불러오는 중
            </div>
          ) : detailQuery.isError ? (
            <div className="p-4 text-center text-sm text-slate-600">
              <p>문의 상세를 불러오지 못했습니다.</p>
              <Button variant="outline" size="sm" className="mt-2" onClick={() => void detailQuery.refetch()}>다시 시도</Button>
            </div>
          ) : detailData && (
            <>
              <header className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 border-b border-slate-200 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-600">
                    <span>{TYPE_LABELS[detailData.type]}</span>
                    <span>{detailData.authorLabel}</span>
                    <time dateTime={detailData.createdAt}>{format(new Date(detailData.createdAt), "yyyy.MM.dd HH:mm")}</time>
                    {detailData.deleted && <Badge variant="secondary">삭제됨</Badge>}
                  </div>
                  <h2 className="text-lg leading-snug font-semibold break-words text-slate-900">{detailData.title}</h2>
                </div>
                <Select value={detailData.status} onValueChange={handleStatusChange} disabled={detailData.deleted || updateStatusMutation.isPending}>
                  <SelectTrigger aria-label="처리 상태" size="sm" className="shrink-0 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {STATUS_OPTIONS.map(({ value, label }) => (
                      <SelectItem key={value} value={value}>{label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </header>

              <div key={selectedId} className="min-h-0 flex-1 md:overflow-y-auto">
                <div className="px-4 py-3">
                  <p className="text-sm leading-relaxed break-words whitespace-pre-wrap text-slate-700">
                    {detailData.content}
                  </p>
                  {detailData.attachments.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-2">
                      {detailData.attachments.map((attachment, index) => (
                        <Button key={attachment.id} variant="outline" size="sm" className="text-xs" disabled={downloadAttachmentMutation.isPending} onClick={() => handleAttachmentDownload(attachment.id, index + 1)}>
                          <Download className="size-3.5" /> 첨부파일 {index + 1}
                        </Button>
                      ))}
                    </div>
                  )}
                </div>

                <section aria-label="답변 기록" className="border-t border-slate-100 px-4 py-3">
                  <h3 className="mb-2 text-sm font-semibold text-slate-900">답변 기록</h3>
                  {detailData.replies.length === 0 ? (
                    <p className="text-sm text-slate-500">등록된 답변이 없습니다.</p>
                  ) : (
                    <ul className="space-y-2">
                      {detailData.replies.map((reply) => (
                        <li key={reply.id} className="rounded-lg bg-slate-50 px-3 py-2">
                          <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-600">
                            <span className="font-semibold text-slate-900">{reply.authorLabel}</span>
                            {reply.deleted && <Badge variant="secondary">삭제됨</Badge>}
                            <time dateTime={reply.createdAt}>{format(new Date(reply.createdAt), "yyyy.MM.dd HH:mm")}</time>
                            {!detailData.deleted && !reply.deleted && (
                              <div className="ml-auto flex items-center">
                                <Button variant="ghost" size="sm" className="h-7 px-2 text-xs" onClick={() => startEditReply(reply.id, reply.content)}>수정</Button>
                                <Button variant="ghost" size="sm" className="h-7 px-2 text-xs text-red-700" onClick={() => handleReplyDelete(reply.id)}>삭제</Button>
                              </div>
                            )}
                          </div>
                          <p className="text-sm leading-relaxed break-words whitespace-pre-wrap text-slate-700">{reply.content}</p>
                        </li>
                      ))}
                    </ul>
                  )}
                </section>
              </div>

              {!detailData.deleted && (
                <form
                  className="space-y-2 border-t border-slate-200 px-4 py-3"
                  onSubmit={(event) => {
                    event.preventDefault();
                    void handleReplySubmit();
                  }}
                >
                  <label htmlFor="feedback-reply" className="sr-only">답변</label>
                  <Textarea
                    id="feedback-reply"
                    placeholder="답변을 입력하세요..."
                    value={replyContent}
                    onChange={(event) => setReplyContent(event.target.value)}
                    className="min-h-16 resize-none text-base md:text-[13px]"
                  />
                  <div className="flex justify-end gap-2">
                    {editingReplyId !== null && (
                      <Button type="button" size="sm" variant="ghost" onClick={resetReplyDraft}>수정 취소</Button>
                    )}
                    <Button type="submit" size="sm" disabled={isReplySubmitting || !replyContent.trim()}>
                      {isReplySubmitting && <Loader2 className="size-4 animate-spin" />}
                      {editingReplyId !== null ? "수정 저장" : "답변 등록"}
                    </Button>
                  </div>
                </form>
              )}
            </>
          )}
        </section>
      </div>
    </main>
  );
}
