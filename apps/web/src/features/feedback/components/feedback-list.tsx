"use client";

import { Loader2 } from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import {
  FeedbackResponse,
  FeedbackStatus,
  FeedbackType,
} from "@/shared/types/api";

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
  getStatusBadge,
}: FeedbackListProps) {
  if (isLoading) {
    return (
      <div className="p-10 flex justify-center">
        <Loader2 className="w-6 h-6 animate-spin text-primary/30" />
      </div>
    );
  }

  if (!feedbackList || feedbackList.content.length === 0) {
    return (
      <div className="py-20 text-center text-gray-400 font-medium text-sm">
        작성한 문의 내역이 없습니다.
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="hidden md:grid grid-cols-[80px_100px_1fr_120px_100px] gap-4 px-4 py-3 border-y border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-black/20 text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
        <div className="text-center">번호</div>
        <div>유형</div>
        <div>제목</div>
        <div className="text-center">작성일</div>
        <div className="text-center">상태</div>
      </div>

      <div className="divide-y divide-gray-100 dark:divide-gray-800 border-b border-gray-100 dark:border-gray-800">
        {feedbackList.content.map((item, index) => (
          <div
            key={item.id}
            onClick={() => onSelectFeedback(item.id)}
            className={`group grid grid-cols-1 md:grid-cols-[80px_100px_1fr_120px_100px] gap-2 md:gap-4 px-4 py-4 transition-colors cursor-pointer items-center hover:bg-gray-50/80 dark:hover:bg-white/5 ${
              selectedFeedbackId === item.id ? "bg-primary/5" : ""
            }`}
          >
            <div className="hidden md:block text-center text-sm text-gray-400 font-medium">
              {feedbackList.content.length - index}
            </div>

            <div className="flex items-center gap-2">
              <span className="md:hidden text-[10px] font-bold text-gray-400 uppercase">
                유형:
              </span>
              <div className="flex items-center gap-1.5 text-xs font-bold text-gray-600 dark:text-gray-400">
                {getTypeIcon(item.type)}
                <span className="text-[11px]">{item.type}</span>
              </div>
            </div>

            <div className="flex flex-col gap-1">
              <h4 className="text-[15px] font-bold text-gray-900 dark:text-gray-100 group-hover:text-primary transition-colors line-clamp-1">
                {item.title}
                {item.hasReplies && (
                  <span className="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-primary/10 text-primary border border-primary/20">
                    답변됨
                  </span>
                )}
              </h4>
            </div>

            <div className="flex items-center md:justify-center gap-2 text-[13px] text-gray-400 font-medium">
              <span className="md:hidden font-bold">작성일:</span>
              {item.createdAt.split("T")[0]}
            </div>

            <div className="flex items-center md:justify-center gap-2">
              <span className="md:hidden text-[10px] font-bold text-gray-400 uppercase">
                상태:
              </span>
              {getStatusBadge(item.status)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
