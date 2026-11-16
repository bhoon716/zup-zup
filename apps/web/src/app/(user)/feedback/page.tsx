"use client";

import { 
  ShieldAlert, 
  Lightbulb, 
  HelpCircle,
  Loader2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Suspense, useRef, useState } from "react";

import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { useMyFeedbacks } from "@/features/feedback/hooks/useFeedback";
import { FeedbackStatus, FeedbackType } from "@/shared/types/api";
import { FeedbackList } from "@/features/feedback/components/feedback-list";
import { FeedbackPagination } from "@/features/feedback/components/feedback-pagination";

function FeedbackPageContent() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const listTop = useRef<HTMLDivElement>(null);
  const { data: feedbackList, isLoading: isListLoading, isFetching, isError, refetch } = useMyFeedbacks(page);

  if (feedbackList && !isFetching && !isError) {
    const lastPage = Math.max(0, feedbackList.totalPages - 1);
    if (page > lastPage) setPage(lastPage);
  }

  const changePage = (nextPage: number) => {
    if (nextPage === page) return;
    setPage(nextPage);
    listTop.current?.focus({ preventScroll: true });
    listTop.current?.scrollIntoView({ block: "start" });
  };

  const getStatusBadge = (status: FeedbackStatus) => {
    switch (status) {
      case "PENDING": return <Badge variant="outline" className="bg-gray-100 dark:bg-gray-800 text-gray-500 border-gray-200 dark:border-gray-800 rounded-md text-[10px]">대기 중</Badge>;
      case "IN_PROGRESS": return <Badge variant="outline" className="bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400 border-blue-100 dark:border-blue-800 rounded-md text-[10px]">처리 중</Badge>;
      case "COMPLETED": return <Badge variant="outline" className="bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 border-green-100 dark:border-green-800 rounded-md text-[10px]">완료</Badge>;
      case "REJECTED": return <Badge variant="outline" className="bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 border-red-100 dark:border-red-800 rounded-md text-[10px]">반려</Badge>;
      default: return null;
    }
  };

  const getTypeIcon = (type: FeedbackType) => {
    switch (type) {
      case "BUG": return <ShieldAlert className="w-3.5 h-3.5 text-red-500" />;
      case "SUGGESTION": return <Lightbulb className="w-3.5 h-3.5 text-amber-500" />;
      default: return <HelpCircle className="w-3.5 h-3.5 text-blue-500" />;
    }
  };

  return (
    <main className="min-h-screen px-4 py-12 md:px-8">
      <div className="container max-w-5xl mx-auto space-y-10">
        <div className="border-b border-gray-100 dark:border-gray-800 pb-8 mt-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white mb-2">문의 및 건의사항</h1>
          <p className="text-[15px] font-medium text-gray-400">사용자분들의 소중한 의견을 바탕으로 더욱 발전하는 서비스를 만들겠습니다.</p>
        </div>

        <div className="flex justify-end mb-6">
          <Link 
            href="/feedback/write" 
            className="flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-bold text-white hover:bg-primary/90 transition-all shadow-md shadow-primary/10"
          >
            새 문의 등록
          </Link>
        </div>

        <div ref={listTop} tabIndex={-1} className="scroll-mt-20">
          <section aria-label="문의 목록" className="overflow-hidden rounded-xl border bg-card">
            {isError ? (
              <div role="alert" className="flex flex-wrap items-center justify-center gap-3 px-4 py-12 text-sm text-muted-foreground">
                문의 목록을 불러오지 못했습니다.
                <Button type="button" variant="outline" size="sm" onClick={() => { void refetch(); }}>다시 시도</Button>
              </div>
            ) : (
              <FeedbackList
                isLoading={isListLoading}
                feedbackList={feedbackList}
                selectedFeedbackId={null}
                onSelectFeedback={(id) => router.push(`/feedback/${id}`)}
                getTypeIcon={getTypeIcon}
                getStatusBadge={getStatusBadge}
              />
            )}
          </section>
          {feedbackList && feedbackList.totalPages > 1 && (
            <FeedbackPagination className="mt-5" page={page} totalPages={feedbackList.totalPages} onChange={changePage} />
          )}
        </div>
      </div>
    </main>
  );
}

export default function FeedbackPage() {
  return (
    <Suspense fallback={<div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary/30" /></div>}>
      <FeedbackPageContent />
    </Suspense>
  );
}
