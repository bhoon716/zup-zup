"use client";

import { use } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ShieldAlert, Lightbulb, HelpCircle, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { FeedbackDetailView } from "@/features/feedback/components/feedback-detail-view";
import { useFeedbackDetail, useDeleteFeedback } from "@/features/feedback/hooks/useFeedback";
import { FeedbackType } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

/**
 * 건의사항 상세 페이지 컴포넌트
 */
export default function FeedbackDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const router = useRouter();
  const resolvedParams = use(params);
  const feedbackId = parseInt(resolvedParams.id, 10);

  const { data: feedbackDetail, isLoading: isDetailLoading } = useFeedbackDetail(feedbackId, !isNaN(feedbackId));
  const deleteFeedbackMutation = useDeleteFeedback();

  const getTypeIcon = (type: FeedbackType) => {
    switch (type) {
      case "BUG": return <ShieldAlert className="w-4 h-4 text-red-500" />;
      case "SUGGESTION": return <Lightbulb className="w-4 h-4 text-yellow-500" />;
      default: return <HelpCircle className="w-4 h-4 text-gray-400" />;
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("정말 이 건의사항을 삭제하시겠습니까? (삭제된 정보는 복구할 수 없습니다.)")) return;
    try {
      await deleteFeedbackMutation.mutateAsync(id);
      toast.success("삭제되었습니다.");
      router.push("/feedback");
    } catch {
      toast.error("삭제에 실패했습니다.");
    }
  };

  if (isNaN(feedbackId)) {
    return <div className="text-center py-20 text-gray-500">유효하지 않은 피드백 ID입니다.</div>;
  }

  return (
    <div className="container max-w-4xl py-8 px-4 md:px-8 mx-auto">
      <Button 
        variant="ghost" 
        size="sm" 
        onClick={() => router.push("/feedback")}
        className="mb-6 -ml-3 gap-1 text-gray-500 hover:text-gray-900 dark:hover:text-gray-100"
      >
        <ChevronLeft className="w-4 h-4" />
        목록으로 돌아가기
      </Button>

      <div className="bg-white dark:bg-[#151515] border border-gray-200 dark:border-gray-800 rounded-3xl overflow-hidden shadow-xl p-6 md:p-8 min-h-[500px]">
        {isDetailLoading ? (
          <div className="h-full flex items-center justify-center p-20">
            <Loader2 className="w-8 h-8 animate-spin text-primary/30" />
          </div>
        ) : (
          <FeedbackDetailView
            isLoading={isDetailLoading}
            feedbackDetail={feedbackDetail}
            selectedFeedbackId={feedbackId}
            onDelete={handleDelete}
            getTypeIcon={getTypeIcon}
          />
        )}
      </div>
    </div>
  );
}
