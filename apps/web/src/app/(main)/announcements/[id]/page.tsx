"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { motion } from "framer-motion";
import { 
  ChevronLeft, 
  Loader2, 
  Megaphone, 
  Pin, 
  Calendar,
  Clock,
  ArrowRight,
  Share2
} from "lucide-react";
import { useAnnouncement } from "@/features/announcement/hooks/useAnnouncements";
import { MarkdownViewer } from "@/shared/ui/markdown-viewer";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";

/**
 * 날짜 문자열을 한국어 양식(YYYY. MM. DD. HH:mm)으로 변환합니다.
 * 유효하지 않은 날짜의 경우 "-"를 반환합니다.
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
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
};

/**
 * 특정 공지사항의 상세 내용을 보여주는 페이지 컴포넌트입니다.
 */
export default function AnnouncementDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params?.id ?? 0);
  const { data: announcement, isLoading } = useAnnouncement(id);

  if (isLoading) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
        <Loader2 className="h-10 w-10 animate-spin text-primary/40" />
        <p className="mt-4 text-sm font-bold text-slate-400">공지사항을 불러오는 중입니다...</p>
      </div>
    );
  }

  if (!announcement) {
    return (
      <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
        <h2 className="text-xl font-black text-slate-900">공지사항을 찾을 수 없습니다</h2>
        <Link href="/announcements" className="mt-6">
          <Button variant="default" className="rounded-2xl">
            목록으로 돌아가기
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f7f7fb_45%,#f8fafc_100%)]">
      <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
        {/* 상단 네비게이션 */}
        <motion.div 
          initial={{ opacity: 0, x: -10 }}
          animate={{ opacity: 1, x: 0 }}
          className="mb-8 flex items-center justify-between"
        >
          <Link href="/announcements">
            <Button variant="ghost" className="group rounded-2xl border border-white bg-white/50 px-4 py-6 shadow-sm ring-1 ring-slate-100 backdrop-blur-md">
              <ChevronLeft className="mr-2 h-5 w-5 transition-transform group-hover:-translate-x-1" />
              <span className="font-bold">목록으로</span>
            </Button>
          </Link>
          <Button variant="ghost" size="icon" className="h-12 w-12 rounded-2xl border border-white bg-white/50 shadow-sm ring-1 ring-slate-100 backdrop-blur-md">
            <Share2 className="h-5 w-5 text-slate-400" />
          </Button>
        </motion.div>

        {/* 메인 공지사항 카드 */}
        <motion.article 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="relative overflow-hidden rounded-[2.5rem] border border-white bg-white shadow-[0_20px_40px_rgba(0,0,0,0.04)] sm:rounded-[3rem]"
        >
          {/* 장식용 상단 바 */}
          <div className="h-2 w-full bg-primary/20" />
          
          <div className="p-6 sm:p-10 lg:p-12">
            {/* 고정 상태 및 태그 */}
            <div className="mb-6 flex flex-wrap items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <Megaphone className="h-5 w-5" />
              </div>
              {announcement.pinned && (
                <span className="inline-flex items-center gap-1.5 rounded-full bg-primary/10 px-3 py-1 text-[11px] font-black text-primary ring-1 ring-primary/20">
                  <Pin className="h-3.5 w-3.5" />
                  중요 공지
                </span>
              )}
            </div>

            {/* 제목부 */}
            <header className="mb-10 lg:mb-12">
              <h1 className="text-2xl font-black leading-snug text-slate-900 sm:text-3xl lg:text-4xl">
                {announcement.title}
              </h1>
              
              <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3 border-y border-slate-50 py-5 sm:mt-10 sm:py-6">
                <div className="flex items-center gap-2 text-sm font-bold text-slate-400">
                  <Calendar className="h-4 w-4" />
                  등록일: {formatDateTime(announcement.createdAt)}
                </div>
                <div className="flex items-center gap-2 text-sm font-bold text-slate-400">
                  <Clock className="h-4 w-4" />
                  최종 수정: {formatDateTime(announcement.updatedAt)}
                </div>
              </div>
            </header>

            {/* 본문부 */}
            <div className="prose prose-slate max-w-none">
              <MarkdownViewer content={announcement.content} className="text-base text-slate-700 leading-relaxed sm:text-lg" />
            </div>

            {/* 하단 푸터 (다음글/이전글 등의 확장을 고려한 공간) */}
            <footer className="mt-16 sm:mt-24">
              <div className="rounded-3xl bg-slate-50/80 p-6 sm:p-8">
                <h3 className="mb-2 text-base font-black text-slate-900">도움이 필요하신가요?</h3>
                <p className="text-sm font-medium text-slate-500">본 공지 내용에 대해 궁금한 점이 있다면 고객센터로 문의해 주세요.</p>
                <Link href="/support" className="mt-5 inline-flex">
                  <Button variant="outline" className="h-11 rounded-xl bg-white px-5 font-bold shadow-sm">
                    문의하기
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </Link>
              </div>
            </footer>
          </div>
        </motion.article>

        {/* 이전 다음글 링크가 제공된다면 여기에 추가될 수 있음 */}
        <div className="mt-8 flex justify-center">
           <Link href="/announcements" className="text-sm font-bold text-slate-400 transition-colors hover:text-primary">
              모든 공지사항 보기
           </Link>
        </div>
      </main>
    </div>
  );
}
