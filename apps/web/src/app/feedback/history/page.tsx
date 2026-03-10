"use client";

import { 
  MessageSquare, 
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

function FeedbackHistoryPageContent() {
  const router = useRouter();
  const { data: feedbackList, isLoading: isListLoading } = useMyFeedbacks();

  /**
   * 피드백의 처리 상태(대기/처리중/완료/반려)에 따라 
   * 적절한 색상의 배지 컴포넌트를 반환합니다.
   */
  const getStatusBadge = (status: FeedbackStatus) => {
    switch (status) {
      case "PENDING": return <Badge variant="outline" className="bg-gray-100 dark:bg-gray-800 text-gray-500 border-none">대기 중</Badge>;
      case "IN_PROGRESS": return <Badge variant="outline" className="bg-blue-100 dark:bg-blue-900 text-blue-600 dark:text-blue-300 border-none">처리 중</Badge>;
      case "COMPLETED": return <Badge variant="outline" className="bg-green-100 dark:bg-green-900 text-green-600 dark:text-green-300 border-none">완료</Badge>;
      case "REJECTED": return <Badge variant="outline" className="bg-red-100 dark:bg-red-900 text-red-600 dark:text-red-300 border-none">반려</Badge>;
      default: return null;
    }
  };

  /**
   * 피드백의 유형(버그/제안/기타)에 맞는 
   * 아이콘 컴포넌트를 반환합니다.
   */
  const getTypeIcon = (type: FeedbackType) => {
    switch (type) {
      case "BUG": return <ShieldAlert className="w-4 h-4 text-red-500" />;
      case "SUGGESTION": return <Lightbulb className="w-4 h-4 text-yellow-500" />;
      default: return <HelpCircle className="w-4 h-4 text-gray-400" />;
    }
  };

  return (
    <div className="min-h-screen bg-linear-to-br from-gray-50 via-white to-gray-50 dark:from-black dark:via-[#0F0F0F] dark:to-[#141414] py-16 px-4 md:px-8">
      <div className="container max-w-4xl mx-auto space-y-8">
        <div className="flex flex-col items-center justify-center text-center space-y-4 mb-10">
          <div className="w-16 h-16 rounded-3xl bg-linear-to-br from-primary/80 to-primary/40 flex items-center justify-center shadow-lg shadow-primary/20 backdrop-blur-md">
            <MessageSquare className="w-8 h-8 text-white relative z-10" />
          </div>
          <div className="space-y-2">
            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">나의 제보 내역</h1>
            <p className="text-lg font-medium text-gray-500 dark:text-gray-400">그동안 보내주신 소중한 의견들을 모두 확인하세요.</p>
          </div>
        </div>

        <div className="bg-white/80 dark:bg-[#1C1C1E]/80 backdrop-blur-2xl border border-gray-100 dark:border-gray-800/80 rounded-[2rem] overflow-hidden shadow-2xl p-6 md:p-10 transition-all duration-300">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-8 pb-6 border-b border-gray-100 dark:border-gray-800/50 gap-4">
            <h2 className="text-xl font-bold bg-clip-text text-transparent bg-linear-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-400">문의 목록</h2>
            <Link 
              href="/feedback" 
              className="flex items-center gap-2 rounded-2xl bg-gray-50/50 dark:bg-[#2C2C2E]/50 px-5 py-3 text-sm font-bold text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#3C3C3E] hover:scale-105 active:scale-95 transition-all outline-none"
            >
              새로운 의견 남기기
            </Link>
          </div>

        <div className="min-h-[400px]">
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
    </div>
  );
}

export default function FeedbackHistoryPage() {
  return (
    <Suspense fallback={<div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary/30" /></div>}>
      <FeedbackHistoryPageContent />
    </Suspense>
  );
}
