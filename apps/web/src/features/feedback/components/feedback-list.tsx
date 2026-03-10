"use client";

import { 
  Loader2, 
} from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { FeedbackResponse, FeedbackStatus, FeedbackType } from "@/shared/types/api";

interface FeedbackListProps {
  isLoading: boolean;
  feedbackList?: { content: FeedbackResponse[] };
  selectedFeedbackId: number | null;
  onSelectFeedback: (id: number) => void;
  getTypeIcon: (type: FeedbackType) => React.ReactNode;
  getStatusBadge: (status: FeedbackStatus) => React.ReactNode;
}

/**
 * 사용자의 피드백 히스토리 목록 컴포넌트
 */
export function FeedbackList({
  isLoading,
  feedbackList,
  selectedFeedbackId,
  onSelectFeedback,
  getTypeIcon,
  getStatusBadge
}: FeedbackListProps) {
  if (isLoading) {
    return <div className="p-10 flex justify-center"><Loader2 className="w-6 h-6 animate-spin text-primary/30" /></div>;
  }

  if (!feedbackList || feedbackList.content.length === 0) {
    return <div className="py-20 text-center text-gray-400 font-medium text-sm">작성한 문의 내역이 없습니다.</div>;
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {feedbackList.content.map((item) => (
        <div 
          key={item.id}
          onClick={() => onSelectFeedback(item.id)}
          className={`relative overflow-hidden rounded-2xl border p-5 transition-all duration-300 cursor-pointer group ${
            selectedFeedbackId === item.id 
              ? "bg-primary/5 border-primary/30 shadow-md shadow-primary/5" 
              : "bg-white dark:bg-[#202022] border-gray-100 dark:border-gray-800 hover:shadow-xl hover:shadow-black/5 dark:hover:shadow-white/5 hover:border-gray-200 dark:hover:border-gray-700 hover:-translate-y-1"
          }`}
        >
          {/* 마우스 오버 시 나타나는 은은한 그라디언트 배경 */}
          <div className="absolute inset-0 bg-linear-to-br from-white/10 to-transparent dark:from-white/0 dark:to-white/0 opacity-0 group-hover:opacity-100 transition-opacity" />

          <div className="relative z-10">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <div className="p-1.5 rounded-lg bg-gray-50 dark:bg-black/20 text-gray-700 dark:text-gray-300">
                  {getTypeIcon(item.type)}
                </div>
                {getStatusBadge(item.status)}
              </div>
              {item.hasReplies && (
                <Badge className="bg-linear-to-r from-primary/90 to-primary text-white border-none h-6 px-2 shadow-sm font-semibold tracking-wide">
                  답변 완료
                </Badge>
              )}
            </div>
            
            <h4 className="text-base font-bold text-gray-900 dark:text-gray-100 line-clamp-2 mt-1 group-hover:text-primary transition-colors leading-relaxed">
              {item.title}
            </h4>
            
            <div className="flex items-center justify-between mt-4">
              <span className="text-xs font-semibold text-gray-400 dark:text-gray-500 bg-gray-50 dark:bg-[#2A2A2A] px-2.5 py-1 rounded-md">
                {item.createdAt.split('T')[0]}
              </span>
              <div className="text-primary text-xs font-bold opacity-0 -translate-x-2 group-hover:opacity-100 group-hover:translate-x-0 transition-all duration-300">
                상세 보기 &rarr;
              </div>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
