"use client";

import { useState } from "react";
import { 
  Loader2, 
  MessageSquare, 
  ShieldAlert, 
  Lightbulb, 
  HelpCircle,
  Clock,
  ExternalLink,
  ChevronRight,
  Send,
  User,
  Monitor,
  Globe
} from "lucide-react";
import { format } from "date-fns";
import { toast } from "sonner";
import Image from "next/image";

import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Textarea } from "@/shared/ui/textarea";
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from "@/shared/ui/select";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { 
  useFeedbacksForAdmin, 
  useAdminFeedbackDetail, 
  useUpdateFeedbackStatus, 
  useCreateFeedbackReply,
  useUpdateFeedbackReply
} from "@/features/feedback/hooks/useFeedback";
import { FeedbackStatus, FeedbackType } from "@/shared/types/api";

/**
 * 관리자용 피드백 통합 관리 페이지
 */
export default function AdminFeedbackPage() {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState("");
  const [isReplySubmitting, setIsReplySubmitting] = useState(false);
  const [editingReplyId, setEditingReplyId] = useState<number | null>(null);

  const { data: listData, isLoading: isListLoading } = useFeedbacksForAdmin();
  const { data: detailData, isLoading: isDetailLoading } = useAdminFeedbackDetail(selectedId!, !!selectedId);
  
  const updateStatusMutation = useUpdateFeedbackStatus();
  const createReplyMutation = useCreateFeedbackReply();
  const updateReplyMutation = useUpdateFeedbackReply();

  const handleStatusChange = async (newStatus: FeedbackStatus) => {
    if (!selectedId) return;
    try {
      await updateStatusMutation.mutateAsync({ id: selectedId, request: { status: newStatus } });
      toast.success("상태가 변경되었습니다.");
    } catch {
      toast.error("변경에 실패했습니다.");
    }
  };

  const handleReplySubmit = async () => {
    if (!selectedId || !replyContent.trim()) return;
    setIsReplySubmitting(true);
    try {
      if (editingReplyId) {
        await updateReplyMutation.mutateAsync({ 
          replyId: editingReplyId, 
          feedbackId: selectedId, 
          request: { content: replyContent } 
        });
        toast.success("답변 수정 완료");
      } else {
        await createReplyMutation.mutateAsync({ 
          id: selectedId, 
          request: { content: replyContent } 
        });
        toast.success("답변 등록 완료");
      }
      setReplyContent("");
      setEditingReplyId(null);
    } catch {
      toast.error("답변 처리 실패");
    } finally {
      setIsReplySubmitting(false);
    }
  };

  const startEditReply = (id: number, content: string) => {
    setEditingReplyId(id);
    setReplyContent(content);
  };

  const getStatusBadge = (status: FeedbackStatus) => {
    switch (status) {
      case "PENDING": return <Badge variant="outline" className="bg-orange-50 text-orange-600 border-orange-100 rounded-md text-[11px] font-bold">대기</Badge>;
      case "IN_PROGRESS": return <Badge variant="outline" className="bg-blue-50 text-blue-600 border-blue-100 rounded-md text-[11px] font-bold">처리중</Badge>;
      case "COMPLETED": return <Badge variant="outline" className="bg-green-50 text-green-600 border-green-100 rounded-md text-[11px] font-bold">완료</Badge>;
      case "REJECTED": return <Badge variant="outline" className="bg-red-50 text-red-600 border-red-100 rounded-md text-[11px] font-bold">반려</Badge>;
    }
  };

  const getTypeBadge = (type: FeedbackType) => {
    switch (type) {
      case "BUG": return <span className="text-red-500 font-bold text-[11px]">[버그]</span>;
      case "SUGGESTION": return <span className="text-amber-500 font-bold text-[11px]">[건의]</span>;
      default: return <span className="text-blue-500 font-bold text-[11px]">[기타]</span>;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50/50 dark:bg-[#0F0F0F] py-10 px-4">
      <div className="max-w-7xl mx-auto flex flex-col gap-8">
        <div className="border-b border-gray-200 dark:border-gray-800 pb-6">
          <h1 className="text-2xl font-black text-gray-900 dark:text-white mb-1">피드백 관리 센터</h1>
          <p className="text-sm text-gray-400 font-medium">사용자들의 의견을 게시판 형식으로 관리합니다.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 h-[800px]">
          {/* 목록 사이드바 */}
          <div className="lg:col-span-4 bg-white dark:bg-[#151515] border border-gray-200 dark:border-gray-800 rounded-2xl overflow-hidden flex flex-col shadow-sm">
            <div className="p-4 border-b border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-black/20 flex justify-between items-center">
              <span className="text-xs font-black text-gray-500 uppercase tracking-wider">목록 (전체 {listData?.totalElements || 0}건)</span>
            </div>
            <div className="flex-1 overflow-y-auto divide-y divide-gray-50 dark:divide-gray-800">
              {isListLoading ? (
                <div className="p-10 flex justify-center"><Loader2 className="w-5 h-5 animate-spin text-primary/30" /></div>
              ) : listData?.content.map((item) => (
                <div 
                  key={item.id}
                  onClick={() => setSelectedId(item.id)}
                  className={`p-4 cursor-pointer transition-colors hover:bg-gray-50 dark:hover:bg-white/5 group ${selectedId === item.id ? "bg-primary/5 border-l-4 border-primary" : ""}`}
                >
                  <div className="flex flex-col gap-1.5 min-w-0">
                    <div className="flex items-center gap-2">
                      {getStatusBadge(item.status)}
                      <span className="text-[11px] text-gray-400 font-medium">{format(new Date(item.createdAt), "MM.dd HH:mm")}</span>
                    </div>
                    <h3 className={`text-[14px] font-bold truncate ${selectedId === item.id ? "text-primary" : "text-gray-800 dark:text-gray-200"}`}>
                      {getTypeBadge(item.type)} {item.title}
                    </h3>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* 상세 내용 판넬 */}
          <div className="lg:col-span-8 bg-white dark:bg-[#151515] border border-gray-200 dark:border-gray-800 rounded-2xl overflow-hidden flex flex-col shadow-sm">
            {!selectedId ? (
              <div className="flex-1 flex flex-col items-center justify-center p-12 opacity-30 text-center">
                <MessageSquare className="w-16 h-16 mb-4" />
                <p className="text-lg font-bold">열람할 피드백을 선택해주세요.</p>
              </div>
            ) : isDetailLoading ? (
              <div className="flex-1 flex items-center justify-center p-12">
                <Loader2 className="w-8 h-8 animate-spin text-primary/30" />
              </div>
            ) : detailData && (
              <div className="flex-1 flex flex-col h-full overflow-hidden animate-in fade-in duration-300">
                {/* 헤더 */}
                <div className="p-6 border-b border-gray-100 dark:border-gray-800">
                  <div className="flex justify-between items-start gap-4 mb-4">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 mb-1">
                        {getTypeBadge(detailData.type)}
                        <span className="text-xs text-gray-400 font-medium">{format(new Date(detailData.createdAt), "yyyy.MM.dd HH:mm:ss")}</span>
                      </div>
                      <h2 className="text-xl font-black text-gray-900 dark:text-white leading-tight">
                        {detailData.title}
                      </h2>
                    </div>
                    <Select value={detailData.status} onValueChange={(val) => handleStatusChange(val as FeedbackStatus)}>
                      <SelectTrigger className="w-[110px] h-9 text-xs font-bold rounded-lg px-3">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="rounded-xl">
                        <SelectItem value="PENDING" className="text-xs font-bold">대기</SelectItem>
                        <SelectItem value="IN_PROGRESS" className="text-xs font-bold">처리중</SelectItem>
                        <SelectItem value="COMPLETED" className="text-xs font-bold">완료</SelectItem>
                        <SelectItem value="REJECTED" className="text-xs font-bold">반려</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  
                  <div className="flex gap-4 text-[11px] font-bold text-gray-400">
                    <div className="flex items-center gap-1"><Monitor className="w-3 h-3" /> {JSON.parse(detailData.metaInfo || "{}").os || "N/A"}</div>
                    <div className="flex items-center gap-1"><Globe className="w-3 h-3" /> {JSON.parse(detailData.metaInfo || "{}").language || "N/A"}</div>
                  </div>
                </div>

                {/* 내용 및 답변 스크롤 영역 */}
                <div className="flex-1 overflow-y-auto p-6 space-y-8">
                  <div className="bg-gray-50/50 dark:bg-black/20 p-5 rounded-xl border border-gray-100 dark:border-gray-800">
                    <div className="text-[14px] text-gray-700 dark:text-gray-300 leading-relaxed font-medium whitespace-pre-wrap">
                      {detailData.content}
                    </div>
                    {detailData.imageUrls.length > 0 && (
                      <div className="flex flex-wrap gap-2 mt-4 pt-4 border-t border-gray-100 dark:border-gray-800">
                        {detailData.imageUrls.map((url, i) => (
                          <a key={i} href={url} target="_blank" rel="noopener noreferrer" className="relative w-20 h-20 rounded-lg overflow-hidden border border-gray-200 dark:border-gray-800 hover:ring-2 ring-primary transition-all">
                            <Image src={url} alt="첨부" fill className="object-cover" />
                          </a>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="space-y-4">
                    <h4 className="text-[13px] font-black text-gray-900 dark:text-white flex items-center gap-1.5 uppercase tracking-wide">
                      <ChevronRight className="w-3.5 h-3.5" /> 답변 기록
                    </h4>
                    {detailData.replies.length === 0 ? (
                      <div className="py-10 text-center border-2 border-dashed border-gray-100 dark:border-gray-800 rounded-xl text-xs font-bold text-gray-400">
                        등록된 답변이 없습니다.
                      </div>
                    ) : (
                      <div className="space-y-3">
                        {detailData.replies.map(reply => (
                          <div key={reply.id} className="bg-primary/5 p-4 rounded-xl border border-primary/10">
                            <div className="flex justify-between items-center mb-2">
                              <span className="text-[11px] font-bold text-primary">{reply.adminName} 관리자</span>
                              <div className="flex items-center gap-2">
                                <span className="text-[10px] text-gray-400 font-medium">{format(new Date(reply.createdAt), "yyyy-MM-dd HH:mm")}</span>
                                <Button variant="ghost" size="sm" className="h-6 text-[10px] font-bold px-1.5" onClick={() => startEditReply(reply.id, reply.content)}>수정</Button>
                              </div>
                            </div>
                            <p className="text-[13px] text-gray-700 dark:text-gray-300 leading-relaxed font-medium whitespace-pre-wrap">{reply.content}</p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* 답변 입력 */}
                <div className="p-4 border-t border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-black/20">
                  <div className="relative">
                    <Textarea 
                      placeholder="답변을 입력하세요..." 
                      value={replyContent}
                      onChange={(e) => setReplyContent(e.target.value)}
                      className="min-h-[100px] text-[13px] p-4 pr-16 rounded-xl border-gray-200 dark:border-gray-800 resize-none font-medium"
                    />
                    <Button 
                      onClick={handleReplySubmit}
                      disabled={isReplySubmitting || !replyContent.trim()}
                      className="absolute bottom-2 right-2 h-10 w-10 p-0 rounded-lg shadow-md"
                    >
                      {isReplySubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : editingReplyId ? <CheckIcon className="w-4 h-4"/> : <Send className="w-4 h-4" />}
                    </Button>
                  </div>
                  {editingReplyId && (
                    <div className="mt-2 flex justify-between items-center px-2">
                      <span className="text-[10px] font-bold text-primary animate-pulse underline">수정 모드 활성</span>
                      <Button variant="link" className="h-auto p-0 text-[10px] text-gray-400 hover:text-red-500 font-bold" onClick={() => { setEditingReplyId(null); setReplyContent(""); }}>수정 취소</Button>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      width="24" 
      height="24" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="3" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
      className={className}
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
