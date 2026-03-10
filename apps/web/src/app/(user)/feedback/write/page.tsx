"use client";

import { useState, useRef } from "react";
import { 
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

/**
 * 건의사항 작성 페이지 컴포넌트
 */
function FeedbackWritePageContent() {
  const router = useRouter();

  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const createFeedbackMutation = useCreateFeedback();

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

  const removeFile = (index: number) => {
    if (previews[index]) {
      URL.revokeObjectURL(previews[index]);
    }
    setFiles(prev => prev.filter((_, i) => i !== index));
    setPreviews(prev => prev.filter((_, i) => i !== index));
  };

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
      router.push("/feedback");
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : "피드백 전송에 실패했습니다.";
      toast.error(errorMessage);
    }
  };

  return (
    <div className="min-h-screen bg-white dark:bg-[#0F0F0F] py-12 px-4 md:px-8">
      <div className="container max-w-4xl mx-auto space-y-10">
        <div className="border-b border-gray-100 dark:border-gray-800 pb-8 mt-4">
          <h1 className="text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white mb-2">건의글 작성</h1>
          <p className="text-[15px] font-medium text-gray-400">사소한 버그부터 기발한 아이디어까지, 무엇이든 환영합니다.</p>
        </div>

        <div className="flex justify-start mb-6">
          <Link 
            href="/feedback" 
            className="flex items-center gap-1.5 text-sm font-bold text-gray-500 hover:text-gray-900 dark:hover:text-gray-100 transition-colors"
          >
            <History className="w-4 h-4" /> 목록으로 돌아가기
          </Link>
        </div>

        <div className="bg-white dark:bg-black/10">
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
  );
}

export default function FeedbackWritePage() {
  return (
    <Suspense fallback={<div className="p-20 flex justify-center"><Loader2 className="w-8 h-8 animate-spin text-primary/30" /></div>}>
      <FeedbackWritePageContent />
    </Suspense>
  );
}
