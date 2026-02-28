"use client";

import { useMemo, useState } from "react";
import { motion } from "framer-motion";
import { EyeOff, Loader2, Megaphone, Pencil, Pin, Plus, Trash2 } from "lucide-react";
import {
  useAdminAnnouncements,
  useCreateAdminAnnouncement,
  useDeleteAdminAnnouncement,
  useUpdateAdminAnnouncement,
} from "@/features/admin/hooks/useAdminAnnouncements";
import type { AnnouncementDetailResponse, AnnouncementSearchType } from "@/shared/types/api";
import { MarkdownViewer } from "@/shared/ui/markdown-viewer";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Switch } from "@/shared/ui/switch";

import { cn } from "@/shared/lib/utils";

/**
 * 날짜와 시간을 한국어 형식으로 포맷팅합니다.
 */
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

/**
 * 마크다운 문법을 제거하고 본문 미리보기를 생성합니다.
 */
const extractPreview = (content: string) => {
  const plainText = content
    .replace(/\*\*([^*]+)\*\*/g, "$1") // 강조 제거
    .replace(/\*([^*]+)\*/g, "$1")
    .replace(/`([^`]+)`/g, "$1") // 코드 제거
    .replace(/\[([^\]]+)]\(([^)]+)\)/g, "$1") // 링크 제거
    .replace(/#{1,6}\s*/g, "") // 제목 기호 제거
    .replace(/>\s*/g, "") // 인용 기호 제거
    .replace(/\r\n/g, " ")
    .replace(/\n/g, " ")
    .trim();
  
  return plainText.length <= 90 ? plainText : `${plainText.substring(0, 90)}...`;
};

/**
 * 검색 키워드와 타입에 따라 공지사항이 일치하는지 확인합니다.
 */
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

/**
 * 공지사항 목록 아이템 컴포넌트입니다.
 */
function AnnouncementItem({ 
  announcement, 
  isActive, 
  onClick 
}: { 
  announcement: AnnouncementDetailResponse;
  isActive: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
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
        {!announcement.published && (
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

/**
 * 관리자용 공지사항 관리 패널 컴포넌트입니다.
 */
export function AdminAnnouncementPanel() {
  const { data: announcements, isLoading } = useAdminAnnouncements();
  const { mutate: createAnnouncement, isPending: isCreating } = useCreateAdminAnnouncement();
  const { mutate: updateAnnouncement, isPending: isUpdating } = useUpdateAdminAnnouncement();
  const { mutate: deleteAnnouncement, isPending: isDeleting } = useDeleteAdminAnnouncement();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [pinned, setPinned] = useState(false);
  const [published, setPublished] = useState(true);
  const [searchType, setSearchType] = useState<AnnouncementSearchType>("TITLE_CONTENT");
  const [keyword, setKeyword] = useState("");

  const isSaving = isCreating || isUpdating;
  const canSave = useMemo(() => title.trim().length > 0 && content.trim().length > 0, [title, content]);

  const filteredAnnouncements = useMemo(() => {
    return (announcements ?? []).filter((announcement) =>
      matchesSearch(announcement, keyword.trim(), searchType)
    );
  }, [announcements, keyword, searchType]);

  const resetForm = () => {
    setEditingId(null);
    setTitle("");
    setContent("");
    setPinned(false);
    setPublished(true);
  };

  const startEdit = (announcement: AnnouncementDetailResponse) => {
    setEditingId(announcement.id);
    setTitle(announcement.title);
    setContent(announcement.content);
    setPinned(announcement.pinned);
    setPublished(announcement.published);
  };

  const handleSubmit = () => {
    const request = {
      title: title.trim(),
      content: content.trim(),
      pinned,
      published,
    };

    if (editingId) {
      updateAnnouncement({ id: editingId, request }, { onSuccess: resetForm });
    } else {
      createAnnouncement(request, { onSuccess: resetForm });
    }
  };

  const handleDelete = (id: number) => {
    if (window.confirm("이 공지사항을 삭제할까요?")) {
      deleteAnnouncement(id);
    }
  };

  return (
    <motion.section
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-[2.2rem] bg-white p-5 shadow-2xl shadow-slate-200/50 md:p-6"
    >
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
        <Button type="button" variant="outline" onClick={resetForm} className="rounded-xl text-xs font-bold">
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
                placeholder="공지 검색"
                className="h-9 text-xs"
              />
            </div>

            {isLoading ? (
              <div className="flex h-40 items-center justify-center text-xs text-slate-500">
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                공지 목록 불러오는 중...
              </div>
            ) : (
              <div className="max-h-[640px] space-y-2 overflow-y-auto pr-1">
                {filteredAnnouncements.map((announcement) => (
                  <AnnouncementItem 
                    key={announcement.id}
                    announcement={announcement}
                    isActive={editingId === announcement.id}
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
          </div>
        </div>

        <div className="lg:col-span-6">
          <div className="rounded-2xl border border-slate-200 bg-slate-50/60 p-4">
            <div className="grid gap-3">
              <Input
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="공지 제목"
                disabled={isSaving}
              />
              <textarea
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder={"마크다운으로 공지 내용을 입력하세요.\n예) ## 수강신청 안내\n- 일정\n- 유의사항"}
                disabled={isSaving}
                className="min-h-[220px] w-full resize-y rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none ring-primary/20 transition focus:border-primary focus:ring-2 disabled:cursor-not-allowed disabled:opacity-70"
              />
              <div className="flex flex-wrap items-center gap-5">
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <Switch checked={pinned} onCheckedChange={setPinned} disabled={isSaving} />
                  상단 고정
                </label>
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                  <Switch checked={published} onCheckedChange={setPublished} disabled={isSaving} />
                  공개
                </label>
              </div>
              <div className="flex gap-2">
                <Button
                  type="button"
                  onClick={handleSubmit}
                  disabled={!canSave || isSaving}
                  className="rounded-xl bg-primary text-xs font-bold text-white hover:bg-primary-dark"
                >
                  {isSaving && <Loader2 className="mr-1 h-4 w-4 animate-spin" />}
                  {editingId ? "공지 수정" : "공지 등록"}
                </Button>
                {editingId && (
                  <Button type="button" variant="outline" onClick={resetForm} disabled={isSaving} className="rounded-xl text-xs font-bold">
                    편집 취소
                  </Button>
                )}
              </div>
            </div>

            <div className="mt-4 rounded-xl border border-slate-200 bg-white p-4">
              <div className="mb-3 flex items-center justify-between">
                <p className="text-xs font-bold text-slate-600">미리보기</p>
                {editingId && (
                  <div className="flex items-center gap-2">
                    <span className="inline-flex h-8 items-center rounded-lg border border-slate-200 px-3 text-xs font-bold text-slate-700">
                      <Pencil className="mr-1 h-3.5 w-3.5" />
                      수정 중
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => handleDelete(editingId)}
                      disabled={isDeleting}
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
    </motion.section>
  );
}

