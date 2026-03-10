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
      <div className="h-full flex flex-col items-center justify-center text-center opacity-50 py-20">
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
    <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
      <div className="pb-6 border-b border-gray-100 dark:border-gray-800 mb-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-gray-50 dark:bg-black/20 text-[11px] font-bold text-gray-500">
              {getTypeIcon(feedbackDetail.type)}
              {feedbackDetail.type}
            </div>
          </div>
          <Button 
            variant="ghost" 
            size="sm" 
            className="text-gray-400 hover:text-red-500 hover:bg-red-50 text-[12px] font-bold h-8 px-2"
            onClick={() => onDelete(feedbackDetail.id)}
          >
            <Trash2 className="w-3.5 h-3.5 mr-1" /> 삭제하기
          </Button>
        </div>
        
        <h3 className="text-2xl font-extrabold text-gray-900 dark:text-white mb-4 tracking-tight leading-tight">
          {feedbackDetail.title}
        </h3>
        
        <div className="flex items-center gap-3 text-[12px] text-gray-400 font-medium">
          <span>작성일: {feedbackDetail.createdAt.split('T')[0]}</span>
          <span className="w-px h-3 bg-gray-200 dark:bg-gray-800" />
          <span>상태: <span className="text-primary font-bold">{feedbackDetail.status}</span></span>
        </div>
      </div>
      
      <div className="min-h-[200px] mb-10">
        <div className="text-[15px] text-gray-700 dark:text-gray-300 leading-[1.8] font-medium whitespace-pre-wrap break-all">
          {feedbackDetail.content}
        </div>

        {feedbackDetail.imageUrls.length > 0 && (
          <div className="flex flex-wrap gap-3 mt-8">
            {feedbackDetail.imageUrls.map((url, i) => (
              <div key={i} className="relative w-24 h-24 rounded-lg overflow-hidden border border-gray-100 dark:border-gray-800">
                <Image src={url} alt="첨부사진" fill className="object-cover" />
              </div>
            ))}
          </div>
        ) }
      </div>

      <div className="pt-8 border-t border-gray-100 dark:border-gray-800">
        <h5 className="text-[14px] font-bold text-gray-900 dark:text-gray-100 mb-6 flex items-center gap-2">
          답변 <span className="text-primary">{feedbackDetail.replies.length}</span>
        </h5>
        
        {feedbackDetail.replies.length === 0 ? (
          <div className="bg-gray-50 dark:bg-black/20 rounded-xl p-8 text-center">
            <p className="text-[13px] font-bold text-gray-400">등록된 답변이 없습니다.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {feedbackDetail.replies.map(reply => (
              <div key={reply.id} className="bg-gray-50 dark:bg-black/20 rounded-xl p-5 border border-gray-100 dark:border-gray-800">
                <div className="flex items-center justify-between mb-3 pb-3 border-b border-gray-100 dark:border-gray-800/50">
                  <div className="flex items-center gap-2">
                    <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center text-[10px] font-bold text-primary">ADM</div>
                    <span className="text-[13px] font-bold text-gray-900 dark:text-gray-100">{reply.adminName} 관리자</span>
                  </div>
                  <span className="text-[11px] text-gray-400 font-medium">{reply.createdAt.split('T')[0]}</span>
                </div>
                <p className="text-[14px] text-gray-700 dark:text-gray-300 leading-relaxed font-medium whitespace-pre-wrap">{reply.content}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
