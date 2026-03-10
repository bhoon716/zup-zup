"use client";

import { 
  MessageSquare, 
  Loader2, 
  Trash2
} from "lucide-react";
import Image from "next/image";
import { Button } from "@/shared/ui/button";
import { FeedbackDetailResponse, FeedbackType } from "@/shared/types/api";

interface FeedbackDetailViewProps {
  isLoading: boolean;
  feedbackDetail?: FeedbackDetailResponse;
  selectedFeedbackId: number | null;
  onDelete: (id: number) => Promise<void>;
  getTypeIcon: (type: FeedbackType) => React.ReactNode;
}

/**
 * 피드백 상세 내용 및 답변을 보여주는 컴포넌트
 */
export function FeedbackDetailView({
  isLoading,
  feedbackDetail,
  selectedFeedbackId,
  onDelete,
  getTypeIcon
}: FeedbackDetailViewProps) {
  if (!selectedFeedbackId) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-center opacity-50">
        <MessageSquare className="w-10 h-10 mb-3 text-gray-300" />
        <p className="text-sm font-bold text-gray-400 tracking-tight">문의 내용을 선택하면<br/>답변을 확인할 수 있습니다.</p>
      </div>
    );
  }

  if (isLoading) {
    return <div className="p-10 flex justify-center"><Loader2 className="w-6 h-6 animate-spin text-primary/30" /></div>;
  }

  if (!feedbackDetail) return null;

  return (
    <div className="space-y-5 animate-in fade-in slide-in-from-right-2 duration-300">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1 text-[10px] font-bold text-gray-400">
          {getTypeIcon(feedbackDetail.type)} {feedbackDetail.type}
        </div>
        <Button 
          variant="ghost" 
          size="icon" 
          className="w-7 h-7 text-gray-300 hover:text-red-500 rounded-lg hover:bg-red-50"
          onClick={() => onDelete(feedbackDetail.id)}
        >
          <Trash2 className="w-4 h-4" />
        </Button>
      </div>
      
      <div>
        <h3 className="text-base font-bold text-gray-900 mb-1.5 tracking-tight">{feedbackDetail.title}</h3>
        <p className="text-sm text-gray-600 leading-relaxed font-medium whitespace-pre-wrap">{feedbackDetail.content}</p>
      </div>

      {feedbackDetail.imageUrls.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1">
          {feedbackDetail.imageUrls.map((url, i) => (
            <Image key={i} src={url} alt="첨부사진" width={64} height={64} className="w-16 h-16 rounded-lg object-cover border border-gray-100" />
          ))}
        </div>
      ) }

      <div className="pt-4 border-t border-gray-100">
        <h5 className="text-[11px] font-bold text-primary mb-3 flex items-center gap-2 uppercase tracking-wider">
          <MessageSquare className="w-3.5 h-3.5" /> 관리자 답변
        </h5>
        {feedbackDetail.replies.length === 0 ? (
          <div className="bg-gray-50/50 rounded-2xl p-4 text-center">
            <p className="text-[11px] font-bold text-gray-400">아직 등록된 답변이 없습니다.<br/>조금만 더 기다려주세요!</p>
          </div>
        ) : (
          <div className="space-y-3">
            {feedbackDetail.replies.map(reply => (
              <div key={reply.id} className="bg-primary/5 rounded-2xl p-4 border border-primary/10">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[11px] font-bold text-primary">{reply.adminName} 관리자</span>
                  <span className="text-[10px] text-gray-400">{reply.createdAt.split('T')[0]}</span>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed font-medium whitespace-pre-wrap">{reply.content}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
