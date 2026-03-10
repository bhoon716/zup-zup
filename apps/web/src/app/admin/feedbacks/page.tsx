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

  /**
   * 피드백 처리 상태 변경 처리
   */
  const handleStatusChange = async (newStatus: FeedbackStatus) => {
    if (!selectedId) return;
    try {
      await updateStatusMutation.mutateAsync({ id: selectedId, request: { status: newStatus } });
      toast.success("상태가 변경되었습니다.");
    } catch {
      toast.error("데이터를 불러오는데 실패했습니다.");
    }
  };

  /**
   * 답변 등록 또는 수정 처리
   */
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
        toast.success("답변이 수정되었습니다.");
      } else {
        await createReplyMutation.mutateAsync({ 
          id: selectedId, 
          request: { content: replyContent } 
        });
        toast.success("답변이 등록되었습니다.");
      }
      setReplyContent("");
      setEditingReplyId(null);
    } catch {
      toast.error("답변 처리에 실패했습니다.");
    } finally {
      setIsReplySubmitting(false);
    }
  };

  /**
   * 답변 수정 모드 시작
   */
  const startEditReply = (id: number, content: string) => {
    setEditingReplyId(id);
    setReplyContent(content);
  };

  /**
   * 상태에 따른 뱃지 렌더링
   */
  const getStatusBadge = (status: FeedbackStatus) => {
    switch (status) {
      case "PENDING": return <Badge className="bg-orange-100 text-orange-600 hover:bg-orange-100 border-none px-2 py-0.5 font-bold">대기</Badge>;
      case "IN_PROGRESS": return <Badge className="bg-blue-100 text-blue-600 hover:bg-blue-100 border-none px-2 py-0.5 font-bold">처리중</Badge>;
      case "COMPLETED": return <Badge className="bg-green-100 text-green-600 hover:bg-green-100 border-none px-2 py-0.5 font-bold">완료</Badge>;
      case "REJECTED": return <Badge className="bg-red-100 text-red-600 hover:bg-red-100 border-none px-2 py-0.5 font-bold">반려</Badge>;
    }
  };

  /**
   * 유형에 따른 뱃지 렌더링
   */
  const getTypeBadge = (type: FeedbackType) => {
    switch (type) {
      case "BUG": return <Badge variant="outline" className="text-red-500 border-red-200 gap-1 font-bold whitespace-nowrap"><ShieldAlert className="w-3 h-3"/>버그</Badge>;
      case "SUGGESTION": return <Badge variant="outline" className="text-yellow-600 border-yellow-200 gap-1 font-bold whitespace-nowrap"><Lightbulb className="w-3 h-3"/>건의</Badge>;
      default: return <Badge variant="outline" className="text-gray-500 border-gray-200 gap-1 font-bold whitespace-nowrap"><HelpCircle className="w-3 h-3"/>기타</Badge>;
    }
  };

  return (
    <div className="container py-10 max-w-7xl">
      <div className="flex flex-col gap-6">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 mb-2 font-outfit">피드백 관리</h1>
          <p className="text-gray-500 font-medium font-outfit">사용자들이 남긴 소중한 의견과 버그 리포트를 확인하고 관리합니다.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* 목록 카드 */}
          <Card className="lg:col-span-5 border-none shadow-xl shadow-gray-200/50 rounded-3xl overflow-hidden h-[700px] flex flex-col">
            <CardHeader className="bg-gray-50/50 border-b border-gray-100 px-6 py-4">
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg font-bold">피드백 목록</CardTitle>
                <Badge variant="secondary" className="font-bold">{listData?.totalElements || 0}</Badge>
              </div>
            </CardHeader>
            <CardContent className="p-0 flex-1 overflow-y-auto overflow-x-hidden scrollbar-hide">
              {isListLoading ? (
                <div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary/20" /></div>
              ) : listData?.content.length === 0 ? (
                <div className="p-20 text-center text-gray-400 font-medium">데이터가 없습니다.</div>
              ) : (
                <div className="divide-y divide-gray-50">
                  {listData?.content.map((item) => (
                    <div 
                      key={item.id}
                      onClick={() => setSelectedId(item.id)}
                      className={`p-5 cursor-pointer transition-all hover:bg-primary/5 flex items-center justify-between group ${selectedId === item.id ? "bg-primary/5 active-item" : ""}`}
                    >
                      <div className="space-y-1.5 flex-1 min-w-0 pr-4 font-outfit">
                        <div className="flex items-center gap-2 mb-1">
                          {getTypeBadge(item.type)}
                          {getStatusBadge(item.status)}
                        </div>
                        <h3 className={`font-bold text-sm truncate transition-colors ${selectedId === item.id ? "text-primary font-black" : "text-gray-800"}`}>
                          {item.title}
                        </h3>
                        <div className="flex items-center gap-3 text-[11px] text-gray-400 font-medium">
                          <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> {format(new Date(item.createdAt), "yyyy.MM.dd HH:mm")}</span>
                          {item.hasReplies && <span className="text-primary font-bold">답변 완료</span>}
                        </div>
                      </div>
                      <ChevronRight className={`w-5 h-5 transition-all ${selectedId === item.id ? "text-primary translate-x-1" : "text-gray-200 group-hover:text-gray-400"}`} />
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* 상세 및 액션 카드 */}
          <Card className="lg:col-span-7 border-none shadow-xl shadow-gray-200/50 rounded-3xl overflow-hidden min-h-[700px]">
            {!selectedId ? (
              <CardContent className="h-full flex flex-col items-center justify-center p-20 opacity-40">
                <div className="bg-gray-50 p-6 rounded-full mb-6">
                  <MessageSquare className="w-16 h-16 text-gray-300" />
                </div>
                <p className="text-xl font-bold text-gray-400 font-outfit">목록에서 피드백을 선택해주세요.</p>
              </CardContent>
            ) : isDetailLoading ? (
              <CardContent className="h-full flex justify-center items-center p-20">
                <Loader2 className="w-10 h-10 animate-spin text-primary/20" />
              </CardContent>
            ) : detailData && (
              <div className="flex flex-col h-full animate-in fade-in duration-500">
                {/* 상세 헤더 */}
                <CardHeader className="bg-white border-b border-gray-100 p-8">
                  <div className="flex items-start justify-between gap-4 mb-4">
                    <div className="space-y-1 font-outfit">
                      <div className="flex items-center gap-2 mb-2">
                        {getTypeBadge(detailData.type)}
                        {getStatusBadge(detailData.status)}
                      </div>
                      <CardTitle className="text-2xl font-black text-gray-900 leading-tight">
                        {detailData.title}
                      </CardTitle>
                    </div>
                    <Select value={detailData.status} onValueChange={(val) => handleStatusChange(val as FeedbackStatus)}>
                      <SelectTrigger className="w-[130px] rounded-xl border-gray-200 font-bold bg-white active:scale-95 transition-all font-outfit">
                        <SelectValue placeholder="상태 변경" />
                      </SelectTrigger>
                      <SelectContent className="rounded-2xl border-gray-100 shadow-2xl font-outfit">
                        <SelectItem value="PENDING" className="rounded-lg">대기</SelectItem>
                        <SelectItem value="IN_PROGRESS" className="rounded-lg">처리중</SelectItem>
                        <SelectItem value="COMPLETED" className="rounded-lg">완료</SelectItem>
                        <SelectItem value="REJECTED" className="rounded-lg">반려</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="flex flex-wrap gap-4 items-center bg-gray-50/80 p-4 rounded-2xl border border-gray-100 font-outfit">
                     <div className="flex items-center gap-2 text-[11px] font-bold text-gray-600">
                       <User className="w-3.5 h-3.5 text-primary" /> 제보자 정보 확인 (내부용)
                     </div>
                     <div className="h-3 w-px bg-gray-200 hidden sm:block" />
                     <div className="flex items-center gap-2 text-[11px] font-bold text-gray-600">
                       <Monitor className="w-3.5 h-3.5 text-primary" /> {JSON.parse(detailData.metaInfo || "{}").os || "기기 미확인"}
                     </div>
                     <div className="h-3 w-px bg-gray-200 hidden sm:block" />
                     <div className="flex items-center gap-2 text-[11px] font-bold text-gray-600">
                       <Globe className="w-3.5 h-3.5 text-primary" /> {JSON.parse(detailData.metaInfo || "{}").language || "KO"}
                     </div>
                  </div>
                </CardHeader>

                {/* 상세 내용 */}
                <CardContent className="p-8 flex-1 overflow-y-auto space-y-8 scrollbar-hide">
                  <div className="space-y-4 font-outfit">
                    <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                      <ChevronRight className="w-3 h-3" /> 제보 내용
                    </h4>
                    <pre className="text-base text-gray-700 font-medium leading-relaxed bg-white border border-gray-100 p-5 rounded-2xl overflow-x-auto whitespace-pre-wrap font-outfit">
                      {detailData.content}
                    </pre>
                  </div>

                  {detailData.imageUrls.length > 0 && (
                    <div className="space-y-4 font-outfit">
                      <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                        <ChevronRight className="w-3 h-3" /> 첨부 이미지 ({detailData.imageUrls.length})
                      </h4>
                      <div className="flex gap-4 overflow-x-auto pb-4 scrollbar-hide">
                        {detailData.imageUrls.map((url, i) => (
                          <a key={i} href={url} target="_blank" rel="noopener noreferrer" className="relative group flex-none">
                            <Image src={url} alt="첨부" width={160} height={160} className="w-40 h-40 rounded-2xl object-cover border border-gray-100 shadow-md transition-all group-hover:scale-105 group-hover:ring-4 group-hover:ring-primary/10" />
                            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity rounded-2xl flex items-center justify-center">
                              <ExternalLink className="w-6 h-6 text-white" />
                            </div>
                          </a>
                        ))}
                      </div>
                    </div>
                  )}

                  <hr className="border-gray-50" />

                  {/* 답변 내역 */}
                  <div className="space-y-6 font-outfit">
                    <h4 className="text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                      <ChevronRight className="w-3 h-3" /> 답변 및 처리 내역
                    </h4>
                    
                    {detailData.replies.length === 0 ? (
                      <div className="bg-gray-50/50 border border-dashed border-gray-200 rounded-3xl p-10 text-center">
                         <p className="text-sm font-bold text-gray-400 leading-relaxed font-outfit">아직 등록된 답변이 없습니다.<br/>정확한 조치 후 답변을 남겨주세요.</p>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {detailData.replies.map(reply => (
                          <div key={reply.id} className="bg-primary/5 border border-primary/10 rounded-3xl p-6 relative group">
                            <div className="flex items-center justify-between mb-3">
                              <span className="flex items-center gap-2 text-xs font-bold text-primary">
                                <div className="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center text-[10px] font-black">AD</div>
                                {reply.adminName} 관리자
                              </span>
                              <div className="flex items-center gap-3">
                                <span className="text-[10px] text-gray-400 font-medium">{format(new Date(reply.createdAt), "yyyy.MM.dd HH:mm")}</span>
                                <Button 
                                  variant="ghost" 
                                  size="sm" 
                                  onClick={() => startEditReply(reply.id, reply.content)}
                                  className="h-7 text-[10px] font-bold opacity-0 group-hover:opacity-100 transition-opacity hover:bg-primary/10 hover:text-primary rounded-lg"
                                >
                                  수정하기
                                </Button>
                              </div>
                            </div>
                            <p className="text-sm text-gray-700 font-medium leading-relaxed whitespace-pre-wrap">
                              {reply.content}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* 답변 작성 영역 */}
                    <div className="pt-4 sticky bottom-0 bg-white">
                       <div className="relative">
                          <Textarea 
                            placeholder={editingReplyId ? "답변을 수정 중입니다..." : "관리자 답변을 작성해주세요... (사용자에게 알림이 전송됩니다.)"}
                            value={replyContent}
                            onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setReplyContent(e.target.value)}
                            className="min-h-[120px] rounded-3xl border-gray-100 bg-gray-50/80 p-6 pr-20 focus:bg-white transition-all shadow-inner focus:ring-primary/20 font-medium"
                          />
                          <Button 
                            disabled={isReplySubmitting || !replyContent.trim()}
                            onClick={handleReplySubmit}
                            className="absolute bottom-4 right-4 rounded-2xl w-12 h-12 p-0 shadow-lg shadow-primary/20 active:scale-95 transition-all"
                          >
                            {isReplySubmitting ? <Loader2 className="w-5 h-5 animate-spin" /> : editingReplyId ? <CheckIcon className="w-5 h-5"/> : <Send className="w-5 h-5" />}
                          </Button>
                       </div>
                       {editingReplyId && (
                         <div className="mt-2 flex items-center justify-between px-2">
                           <span className="text-[10px] font-bold text-primary animate-pulse italic">수정 모드 실행 중...</span>
                           <Button variant="link" onClick={() => { setEditingReplyId(null); setReplyContent(""); }} className="h-auto p-0 text-[10px] text-gray-400 hover:text-red-500 font-bold">취소</Button>
                         </div>
                       )}
                    </div>
                  </div>
                </CardContent>
              </div>
            )}
          </Card>
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
