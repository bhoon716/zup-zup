"use client";

import { useState, useRef } from "react";
import { 
  MessageSquare, 
  History,
  Loader2,
} from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";
import { Suspense } from "react";

import { compressImage } from "@/shared/lib/image";
import { useCreateFeedback } from "@/features/feedback/hooks/useFeedback";
import { FeedbackCreateForm } from "@/features/feedback/components/feedback-create-form";

function FeedbackPageContent() {
  const router = useRouter();

  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const createFeedbackMutation = useCreateFeedback();

  /**
   * 사용자가 첨부한 파일을 확인하고 이미지 압축을 수행한 뒤 상태에 저장합니다.
   * 최대 3장까지만 첨부 가능합니다.
   */
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFiles = Array.from(e.target.files || []);
    if (files.length + selectedFiles.length > 3) {
      toast.error("최대 3장까지만 첨부할 수 있습니다.");
      return;
    }

    const compressedFiles: File[] = [];
    const newPreviews: string[] = [];

    try {
      await Promise.all(selectedFiles.map(async (file) => {
        const compressed = await compressImage(file);
        compressedFiles.push(compressed);
        newPreviews.push(URL.createObjectURL(compressed));
      }));
      setFiles(prev => [...prev, ...compressedFiles]);
      setPreviews(prev => [...prev, ...newPreviews]);
    } catch {
      toast.error("이미지 처리 중 오류가 발생했습니다.");
    }
  };

  /**
   * 사용자가 첨부한 파일 목록에서 특정 인덱스의 파일을 제거하고, 
   * 브라우저 메모리에 할당된 미리보기 URL을 해제합니다.
   */
  const removeFile = (index: number) => {
    if (previews[index]) {
      URL.revokeObjectURL(previews[index]);
    }
    setFiles(prev => prev.filter((_, i) => i !== index));
    setPreviews(prev => prev.filter((_, i) => i !== index));
  };

  /**
   * 제보 양식을 제출합니다.
   * 사용자 기기 및 브라우저 환경 정보를 메타데이터로 함께 서버에 전송합니다.
   */
  const handleFormSubmit = async (values: { type: "BUG" | "SUGGESTION" | "OTHER"; title: string; content: string }) => {
    try {
      const metaInfo = JSON.stringify({
        url: window.location.href,
        userAgent: navigator.userAgent,
        os: navigator.platform,
        language: navigator.language,
        timestamp: new Date().toISOString(),
      });

      await createFeedbackMutation.mutateAsync({ request: { ...values, metaInfo }, files });

      toast.success("소중한 의견 감사합니다!");
      setFiles([]);
      setPreviews([]);
      router.push("/feedback/history");
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : "피드백 전송에 실패했습니다.";
      toast.error(errorMessage);
    }
  };

  return (
    <div className="min-h-screen bg-linear-to-br from-gray-50 via-white to-gray-50 dark:from-black dark:via-[#0F0F0F] dark:to-[#141414] py-16 px-4 md:px-8">
      <div className="container max-w-3xl mx-auto space-y-8">
        <div className="flex flex-col items-center justify-center text-center space-y-4 mb-10">
          <div className="w-16 h-16 rounded-3xl bg-linear-to-br from-primary/80 to-primary/40 flex items-center justify-center shadow-lg shadow-primary/20 backdrop-blur-md">
            <MessageSquare className="w-8 h-8 text-white relative z-10" />
          </div>
          <div className="space-y-2">
            <h1 className="text-4xl font-extrabold tracking-tight text-gray-900 dark:text-white">현장의 목소리를<br/>들려주세요.</h1>
            <p className="text-lg font-medium text-gray-500 dark:text-gray-400">사소한 버그부터 기발한 아이디어까지, 무엇이든 환영합니다.</p>
          </div>
        </div>

        <div className="bg-white/80 dark:bg-[#1C1C1E]/80 backdrop-blur-2xl border border-gray-100 dark:border-gray-800/80 rounded-[2rem] overflow-hidden shadow-2xl p-6 md:p-10 transition-all duration-300 hover:shadow-primary/5">
          <div className="flex items-center justify-between mb-8 pb-6 border-b border-gray-100 dark:border-gray-800/50 gap-4">
            <h2 className="text-xl font-bold bg-clip-text text-transparent bg-linear-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-400">새로운 제보 작성</h2>
            <Link 
              href="/feedback/history" 
              className="flex items-center gap-2 rounded-2xl bg-gray-50/50 dark:bg-[#2C2C2E]/50 px-5 py-3 text-sm font-bold text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-[#3C3C3E] hover:scale-105 active:scale-95 transition-all outline-none"
            >
              <History className="w-4 h-4" /> 내 문의 내역
            </Link>
          </div>

        <div className="max-w-full mx-auto">
          <FeedbackCreateForm
            onSubmit={handleFormSubmit}
            isPending={createFeedbackMutation.isPending}
            files={files}
            previews={previews}
            onFileChange={handleFileChange}
            onRemoveFile={removeFile}
            fileInputRef={fileInputRef}
          />
        </div>
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
