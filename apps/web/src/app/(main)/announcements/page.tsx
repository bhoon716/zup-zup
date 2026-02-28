"use client";

import { useDeferredValue, useState } from "react";
import Link from "next/link";
import { motion, AnimatePresence, type Variants } from "framer-motion";
import { 
  ArrowRight, 
  Loader2, 
  Megaphone, 
  Pin, 
  Search, 
  Calendar,
  ChevronRight,
  FilterX
} from "lucide-react";
import { useAnnouncements } from "@/features/announcement/hooks/useAnnouncements";
import type { AnnouncementSearchType } from "@/shared/types/api";
import { Input } from "@/shared/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { cn } from "@/shared/lib/utils";

/**
 * 날짜 문자열을 한국어 양식(YYYY. MM. DD.)으로 변환합니다.
 */
const formatDateTime = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(date);
};

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1
    }
  }
};

const itemVariants: Variants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: {
      type: "spring",
      stiffness: 100,
      damping: 15
    }
  }
};

/**
 * 공지사항 목록을 검색 조건과 함께 렌더링하는 페이지 컴포넌트입니다.
 */
export default function AnnouncementsPage() {
  const [keyword, setKeyword] = useState("");
  const [searchType, setSearchType] = useState<AnnouncementSearchType>("TITLE_CONTENT");
  const deferredKeyword = useDeferredValue(keyword);
  const { data: announcements, isLoading } = useAnnouncements({
    keyword: deferredKeyword,
    searchType,
  });

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
      <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
        {/* 헤더 섹션 */}
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-10 flex flex-col items-center text-center sm:items-start sm:text-left"
        >
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary shadow-sm ring-1 ring-primary/20">
            <Megaphone className="h-7 w-7" />
          </div>
          <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
            공지사항
          </h1>
          <p className="mt-2 max-w-2xl text-base font-medium text-slate-500">
            전북대학교 수강신청 도우미의 새로운 소식과 안내를 확인하세요.
          </p>
        </motion.div>

        {/* 검색 및 필터 섹션 */}
        <motion.div 
          initial={{ opacity: 0, scale: 0.98 }}
          animate={{ opacity: 1, scale: 1 }}
          className="mb-8 rounded-3xl border border-white/40 bg-white/70 p-3 shadow-[0_8px_30px_rgb(0,0,0,0.04)] backdrop-blur-xl sm:p-4"
        >
          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="w-full sm:w-40 lg:w-48">
              <Select
                value={searchType}
                onValueChange={(value) => setSearchType(value as AnnouncementSearchType)}
              >
                <SelectTrigger className="h-11 rounded-2xl border-none bg-slate-100/50 font-bold focus:ring-2 focus:ring-primary/20">
                  <SelectValue placeholder="검색 기준" />
                </SelectTrigger>
                <SelectContent className="rounded-2xl border-none shadow-xl">
                  <SelectItem value="TITLE" className="rounded-xl font-medium">제목</SelectItem>
                  <SelectItem value="CONTENT" className="rounded-xl font-medium">내용</SelectItem>
                  <SelectItem value="TITLE_CONTENT" className="rounded-xl font-medium">제목 + 내용</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="relative flex-1">
              <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">
                <Search className="h-4 w-4" />
              </div>
              <Input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="검색어를 입력하여 공지를 찾아보세요"
                className="h-11 rounded-2xl border-none bg-slate-100/50 pl-11 font-bold placeholder:text-slate-400 focus-visible:ring-2 focus-visible:ring-primary/20"
              />
            </div>
          </div>
        </motion.div>

        {/* 공지사항 목록 */}
        {isLoading ? (
          <div className="flex flex-col items-center justify-center rounded-3xl border border-white bg-white/50 py-24 shadow-sm backdrop-blur">
            <Loader2 className="h-8 w-8 animate-spin text-primary/50" />
            <p className="mt-4 text-sm font-bold text-slate-400">공지사항을 불러오는 중입니다...</p>
          </div>
        ) : (
          <motion.div 
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="grid gap-4 sm:gap-5"
          >
            <AnimatePresence mode="popLayout">
              {(announcements ?? []).map((announcement) => (
                <motion.div
                  layout
                  key={announcement.id}
                  variants={itemVariants}
                  exit={{ opacity: 0, scale: 0.95 }}
                >
                  <Link href={`/announcements/${announcement.id}`} className="group block">
                    <article className={cn(
                      "relative overflow-hidden rounded-[2rem] border border-white bg-white p-5 shadow-[0_4px_20px_rgba(0,0,0,0.03)] transition-all duration-300 group-hover:-translate-y-1 group-hover:border-primary/20 group-hover:shadow-[0_12px_30px_rgba(0,0,0,0.06)] sm:p-7",
                      announcement.pinned && "ring-1 ring-primary/20"
                    )}>
                      {/* 고정 뱃지 */}
                      {announcement.pinned && (
                        <div className="mb-4 flex">
                          <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-[11px] font-black text-primary ring-1 ring-primary/30">
                            <Pin className="h-3 w-3" />
                            중요 공지
                          </span>
                        </div>
                      )}

                      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between sm:gap-6">
                        <div className="flex-1">
                          <h2 className="line-clamp-2 text-lg font-black leading-tight text-slate-900 transition-colors group-hover:text-primary sm:text-xl">
                            {announcement.title}
                          </h2>
                          <p className="mt-3 line-clamp-2 text-sm font-medium leading-relaxed text-slate-500 sm:text-base">
                            {announcement.previewContent || "본문 내용이 없습니다."}
                          </p>
                        </div>
                        <div className="mt-5 flex items-center justify-between border-t border-slate-50 pt-4 sm:mt-0 sm:flex-col sm:items-end sm:justify-center sm:border-none sm:pt-0">
                          <div className="flex items-center gap-1.5 text-xs font-bold text-slate-400 sm:text-sm">
                            <Calendar className="h-3.5 w-3.5" />
                            {formatDateTime(announcement.createdAt)}
                          </div>
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-50 text-slate-400 transition-colors group-hover:bg-primary group-hover:text-white sm:mt-3">
                            <ChevronRight className="h-5 w-5" />
                          </div>
                        </div>
                      </div>
                    </article>
                  </Link>
                </motion.div>
              ))}
            </AnimatePresence>

            {(announcements ?? []).length === 0 && !isLoading && (
              <motion.div 
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="flex flex-col items-center justify-center rounded-[2.5rem] border-2 border-dashed border-slate-200 bg-slate-50/50 py-24 text-center"
              >
                <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-3xl bg-slate-100 text-slate-300">
                  <FilterX className="h-8 w-8" />
                </div>
                <h3 className="text-lg font-black text-slate-900">검색 결과가 없습니다</h3>
                <p className="mt-2 text-sm font-medium text-slate-400">다른 검색어를 입력해 보시겠어요?</p>
              </motion.div>
            )}
          </motion.div>
        )}
      </main>
    </div>
  );
}
