"use client";

import { Fragment, useMemo, useRef, useState } from "react";
import { ChevronLeft, ChevronRight, EyeOff, Loader2, Megaphone, Pencil, Pin, Plus, RotateCcw, Trash2 } from "lucide-react";
import {
  useAdminAnnouncements,
  useCreateAdminAnnouncement,
  useDeleteAdminAnnouncement,
  useRestoreAdminAnnouncement,
  useUpdateAdminAnnouncement,
} from "@/features/admin/hooks/useAdminAnnouncements";
import type { AnnouncementDetailResponse, AnnouncementSearchType } from "@/shared/types/api";
import { MarkdownViewer } from "@/shared/ui/markdown-viewer";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Switch } from "@/shared/ui/switch";

import { cn } from "@/shared/lib/utils";

const formatDateTime = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "-";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

const extractPreview = (content: string) => {
  const plainText = content
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/\*([^*]+)\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\[([^\]]+)]\(([^)]+)\)/g, "$1")
    .replace(/#{1,6}\s*/g, "")
    .replace(/>\s*/g, "")
    .replace(/\r\n/g, " ")
    .replace(/\n/g, " ")
    .trim();

  return plainText.length <= 90 ? plainText : `${plainText.substring(0, 90)}...`;
};

const matchesSearch = (
  announcement: AnnouncementDetailResponse,
  keyword: string,
  searchType: AnnouncementSearchType
) => {
  if (!keyword) return true;

  const normalizedKeyword = keyword.toLowerCase();
  const title = announcement.title.toLowerCase();
  const content = announcement.content.toLowerCase();

  switch (searchType) {
    case "TITLE": return title.includes(normalizedKeyword);
    case "CONTENT": return content.includes(normalizedKeyword);
    case "TITLE_CONTENT":
    default: return title.includes(normalizedKeyword) || content.includes(normalizedKeyword);
  }
};

function AnnouncementItem({ announcement, isActive, disabled, onClick }: {
  announcement: AnnouncementDetailResponse;
  isActive: boolean;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "w-full rounded-xl border px-3 py-2.5 text-left transition",
        isActive 
          ? "border-primary/40 bg-primary/5" 
          : "border-slate-200 bg-white hover:border-primary/30"
      )}
    >
      <div className="mb-1 flex items-center gap-1.5">
        {announcement.pinned && (
          <span className="inline-flex items-center rounded-md bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
            <Pin className="mr-1 h-3 w-3" />
            고정
          </span>
        )}
        {announcement.deletedAt ? (
          <span className="inline-flex items-center rounded-md bg-red-50 px-1.5 py-0.5 text-[10px] font-bold text-red-700">
            <Trash2 className="mr-1 h-3 w-3" />
            삭제됨
          </span>
        ) : !announcement.published && (
          <span className="inline-flex items-center rounded-md bg-slate-200 px-1.5 py-0.5 text-[10px] font-bold text-slate-600">
            <EyeOff className="mr-1 h-3 w-3" />
            비공개
          </span>
        )}
      </div>
      <p className="truncate text-sm font-bold text-slate-900">{announcement.title}</p>
      <p className="mt-1 overflow-hidden text-ellipsis whitespace-nowrap text-xs text-slate-500">
        {extractPreview(announcement.content) || "-"}
      </p>
      <p className="mt-1.5 text-[11px] text-slate-400">{formatDateTime(announcement.createdAt)}</p>
    </button>
  );
}

export function AdminAnnouncementPanel() {
  const [page, setPage] = useState(0);
  const { data: announcements, isLoading, isError, isPlaceholderData, refetch } = useAdminAnnouncements(page);
  const listRef = useRef<HTMLDivElement>(null);
  const { mutate: createAnnouncement, isPending: isCreating } = useCreateAdminAnnouncement();
  const { mutate: updateAnnouncement, isPending: isUpdating } = useUpdateAdminAnnouncement();
  const { mutate: deleteAnnouncement, isPending: isDeleting } = useDeleteAdminAnnouncement();
  const { mutate: restoreAnnouncement, isPending: isRestoring } = useRestoreAdminAnnouncement();

  const [editingAnnouncement, setEditingAnnouncement] = useState<AnnouncementDetailResponse | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pinned, setPinned] = useState(false);
  const [published, setPublished] = useState(true);
  const [notify, setNotify] = useState(false);
  const [searchType, setSearchType] = useState<AnnouncementSearchType>("TITLE_CONTENT");
  const [keyword, setKeyword] = useState("");

  const isSaving = isCreating || isUpdating;
  const isBusy = isSaving || isDeleting || isRestoring;
  const editingId = editingAnnouncement?.id;
  const isDeleted = !!editingAnnouncement?.deletedAt;
  const canNotify = published && !isDeleted && (editingAnnouncement === null || editingAnnouncement.firstPublishedAt === null);
  const canSave = title.trim().length > 0 && content.trim().length > 0;
  const totalPages = announcements?.totalPages ?? 0;
  const pageNumbers = [...new Set([0, page - 1, page, page + 1, totalPages - 1])]
    .filter((number) => number >= 0 && number < totalPages)
    .sort((first, second) => first - second);
  const paginationDisabled = isBusy || isPlaceholderData;

  const changePage = (nextPage: number) => {
    if (nextPage === page) return;
    listRef.current?.scrollTo({ top: 0 });
    setPage(nextPage);
  };

  const filteredAnnouncements = useMemo(() => {
    return (announcements?.content ?? []).filter((announcement) =>
      matchesSearch(announcement, keyword.trim(), searchType)
    );
  }, [announcements, keyword, searchType]);

  const resetForm = () => {
    setEditingAnnouncement(null);
    setTitle("");
    setContent("");
    setPinned(false);
    setPublished(true);
    setNotify(false);
  };

  const startEdit = (announcement: AnnouncementDetailResponse) => {
    setEditingAnnouncement(announcement);
    setTitle(announcement.title);
    setContent(announcement.content);
    setPinned(announcement.pinned);
    setPublished(announcement.published);
    setNotify(false);
  };

  const handleSubmit = () => {
    const request = {
      title: title.trim(),
      content: content.trim(),
      pinned,
      published,
      notify: canNotify && notify,
    };

    if (editingId) {
      updateAnnouncement({ id: editingId, request }, { onSuccess: resetForm });
    } else {
      createAnnouncement(request, { onSuccess: resetForm });
    }
  };

  const handleDelete = (id: number) => {
    if (window.confirm("이 공지사항을 삭제할까요?")) {
      deleteAnnouncement(id, { onSuccess: resetForm });
    }
  };

  return (
    <section className="rounded-[2.2rem] bg-white p-5 shadow-2xl shadow-slate-200/50 md:p-6">
      <div className="mb-5 flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <Megaphone className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-lg font-black tracking-tight text-slate-900">공지사항 관리</h2>
            <p className="mt-0.5 text-xs font-medium text-slate-500">좌측 목록, 우측 작성/수정</p>
          </div>
        </div>
        <Button type="button" variant="outline" onClick={resetForm} disabled={isBusy} className="rounded-xl text-xs font-bold">
          <Plus className="mr-1 h-4 w-4" />
          새 공지 작성
        </Button>
      </div>

      <div className="grid gap-6 lg:grid-cols-10">
        <div className="lg:col-span-4">
          <div className="rounded-2xl border border-slate-200 bg-slate-50/60 p-3">
            <div className="mb-3 grid gap-2 sm:grid-cols-[140px_1fr] lg:grid-cols-1 xl:grid-cols-[140px_1fr]">
              <select
                value={searchType}
                onChange={(event) => setSearchType(event.target.value as AnnouncementSearchType)}
                className="h-9 rounded-lg border border-slate-200 bg-white px-3 text-xs outline-none ring-primary/20 transition focus:border-primary focus:ring-2"
              >
                <option value="TITLE">제목</option>
                <option value="CONTENT">내용</option>
                <option value="TITLE_CONTENT">제목 + 내용</option>
              </select>
              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="현재 페이지에서 검색"
                className="h-9 text-xs"
              />
            </div>

            {isLoading ? (
              <div className="flex h-40 items-center justify-center text-xs text-slate-500">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                공지 목록 불러오는 중...
              </div>
            ) : isError ? (
              <div role="alert" className="flex flex-wrap items-center gap-2 rounded-xl bg-red-50 p-3 text-xs text-red-800">
                공지 목록을 불러오지 못했습니다.
                <Button type="button" variant="outline" size="sm" onClick={() => { void refetch(); }}>다시 불러오기</Button>
              </div>
            ) : (
              <div ref={listRef} aria-busy={isPlaceholderData} className="max-h-[640px] space-y-2 overflow-y-auto pr-1">
                {filteredAnnouncements.map((announcement) => (
                  <AnnouncementItem 
                    key={announcement.id}
                    announcement={announcement}
                    isActive={editingId === announcement.id}
                    disabled={isBusy || isPlaceholderData}
                    onClick={() => startEdit(announcement)}
                  />
                ))}
                {filteredAnnouncements.length === 0 && (
                  <div className="rounded-xl border border-dashed border-slate-300 bg-white px-4 py-8 text-center text-xs text-slate-500">
                    검색 결과가 없습니다.
                  </div>
                )}
              </div>
            )}
            {(page > 0 || totalPages > 1) && (
              <nav aria-label="관리 공지 페이지" className="mt-3 flex flex-wrap items-center justify-center gap-0.5">
                <Button type="button" variant="ghost" size="icon-sm" aria-label="이전 페이지" disabled={page === 0 || paginationDisabled} onClick={() => changePage(page - 1)}>
                  <ChevronLeft aria-hidden="true" />
                </Button>
                {pageNumbers.map((number, index) => (
                  <Fragment key={number}>
                    {index > 0 && number - pageNumbers[index - 1] > 1 && (
                      <span aria-hidden="true" className="px-0.5 text-xs text-slate-500">…</span>
                    )}
                    <Button
                      type="button"
                      variant={number === page ? "default" : "ghost"}
                      size="icon-sm"
                      aria-label={`${number + 1} 페이지`}
                      aria-current={number === page ? "page" : undefined}
                      disabled={paginationDisabled}
                      onClick={() => changePage(number)}
                    >
                      {number + 1}
                    </Button>
                  </Fragment>
                ))}
                <Button type="button" variant="ghost" size="icon-sm" aria-label="다음 페이지" disabled={page + 1 >= totalPages || paginationDisabled} onClick={() => changePage(page + 1)}>
                  <ChevronRight aria-hidden="true" />
                </Button>
              </nav>
            )}
          </div>
        </div>

        <div className="lg:col-span-6">
          <div className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
            <div className="grid gap-3">
              <Input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="공지 제목"
                disabled={isBusy || isDeleted}
              />
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder={"마크다운으로 공지 내용을 입력하세요.\n예) ## 수강신청 안내\n- 일정\n- 유의사항"}
                disabled={isBusy || isDeleted}
                className="min-h-[220px] w-full resize-y rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none ring-primary/20 transition focus:border-primary focus:ring-2 disabled:cursor-not-allowed disabled:opacity-70"
              />
              <div className="flex flex-wrap items-center gap-5">
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <Switch checked={pinned} onCheckedChange={setPinned} disabled={isBusy || isDeleted} />
                  상단 고정
                </label>
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <Switch checked={published} onCheckedChange={(checked) => {
                    setPublished(checked);
                    if (!checked) setNotify(false);
                  }} disabled={isBusy || isDeleted} />
                  공개
                </label>
              </div>
              {canNotify && (
                <div className="space-y-1">
                  <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                    <Switch checked={notify} onCheckedChange={setNotify} disabled={isBusy} aria-describedby="announcement-notify-description" />
                    중요 공지 알림 보내기
                  </label>
                  <p id="announcement-notify-description" className="text-xs text-slate-500">처음 공개할 때 알림함과 사용자가 켜 둔 채널로 보냅니다.</p>
                </div>
              )}
              <div className="flex gap-2">
                {isDeleted ? (
                  <Button
                    type="button"
                    onClick={() => restoreAnnouncement(editingAnnouncement.id, { onSuccess: startEdit })}
                    disabled={isBusy}
                    className="rounded-xl text-xs font-bold"
                  >
                    {isRestoring ? <Loader2 className="mr-1 h-4 w-4 animate-spin" /> : <RotateCcw className="mr-1 h-4 w-4" />}
                    공지 복원
                  </Button>
                ) : (
                  <Button
                    type="button"
                    onClick={handleSubmit}
                    disabled={!canSave || isBusy}
                    className="rounded-xl bg-primary text-xs font-bold text-white hover:bg-primary-dark"
                  >
                    {isSaving && <Loader2 className="mr-1 h-4 w-4 animate-spin" />}
                    {editingId ? "공지 수정" : "공지 등록"}
                  </Button>
                )}
                {editingId && (
                  <Button type="button" variant="outline" onClick={resetForm} disabled={isBusy} className="rounded-xl text-xs font-bold">
                    편집 취소
                  </Button>
                )}
              </div>
            </div>

            <div className="mt-4 rounded-xl border border-slate-200 bg-white p-4">
              <div className="mb-3 flex items-center justify-between">
                <p className="text-xs font-bold text-slate-600">미리보기</p>
                {editingId && !isDeleted && (
                  <div className="flex items-center gap-2">
                    <span className="inline-flex h-8 items-center rounded-lg border border-slate-200 px-3 text-xs font-bold text-slate-700">
                      <Pencil className="mr-1 h-3.5 w-3.5" />
                      수정 중
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => handleDelete(editingId)}
                      disabled={isBusy}
                      className="h-8 rounded-lg px-3 text-xs font-bold text-red-600 hover:text-red-700"
                    >
                      <Trash2 className="mr-1 h-3.5 w-3.5" />
                      삭제
                    </Button>
                  </div>
                )}
              </div>
              <h3 className="text-lg font-black text-slate-900">{title.trim() || "제목을 입력하세요."}</h3>
              <MarkdownViewer content={content} className="mt-3" />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
