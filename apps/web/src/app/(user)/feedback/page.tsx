"use client";

import { 
  ShieldAlert, 
  Lightbulb, 
  HelpCircle,
  Loader2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Suspense } from "react";

import { Badge } from "@/shared/ui/badge";
import { useMyFeedbacks } from "@/features/feedback/hooks/useFeedback";
import { FeedbackStatus, FeedbackType } from "@/shared/types/api";
import { FeedbackList } from "@/features/feedback/components/feedback-list";

/**
 * 건의 게시판 목록 페이지 컴포넌트
 */
function FeedbackPageContent() {
  const router = useRouter();
  const { data: feedbackList, isLoading: isListLoading } = useMyFeedbacks();

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
    <div className="min-h-screen bg-white dark:bg-[#0F0F0F] py-12 px-4 md:px-8">
      <div className="container max-w-5xl mx-auto space-y-10">
        <div className="border-b border-gray-100 dark:border-gray-800 pb-8 mt-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white mb-2">건의 게시판</h1>
          <p className="text-[15px] font-medium text-gray-400">보내주신 소중한 의견들을 확인하실 수 있습니다.</p>
        </div>

        <div className="flex justify-end mb-6">
          <Link 
            href="/feedback/write" 
            className="flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-bold text-white hover:bg-primary/90 transition-all shadow-md shadow-primary/10"
          >
            의견 남기기
          </Link>
        </div>

        <div className="bg-white dark:bg-black/10 min-h-[500px]">
          <FeedbackList
            isLoading={isListLoading}
            feedbackList={feedbackList}
            selectedFeedbackId={null}
            onSelectFeedback={(id) => router.push(`/feedback/${id}`)}
            getTypeIcon={getTypeIcon}
            getStatusBadge={getStatusBadge}
          />
        </div>
      </div>
    </div>
  );
}

export default function FeedbackPage() {
  return (
    <Suspense fallback={<div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary/30" /></div>}>
      <FeedbackPageContent />
    </Suspense>
  );
}
