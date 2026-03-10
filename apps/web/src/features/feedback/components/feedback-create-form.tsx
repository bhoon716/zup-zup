"use client";

import { useForm, ControllerRenderProps } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { 
  ShieldAlert, 
  Lightbulb, 
  HelpCircle,
  ImageIcon,
  Loader2,
  X
} from "lucide-react";
import Image from "next/image";
import { Button } from "@/shared/ui/button";
import { 
  Form, 
  FormControl, 
  FormField, 
  FormItem, 
  FormMessage 
} from "@/shared/ui/form";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import { cn } from "@/shared/lib/utils";


const formSchema = z.object({
  type: z.enum(["BUG", "SUGGESTION", "OTHER"] as const),
  title: z.string().min(2, "제목은 2자 이상 입력해주세요.").max(100, "제목은 100자 이하로 입력해주세요."),
  content: z.string().min(5, "내용을 더 자세히 적어주세요.").max(1000, "내용은 1000자 이하로 입력해주세요."),
});

type FormValues = z.infer<typeof formSchema>;

interface FeedbackCreateFormProps {
  onSubmit: (values: FormValues) => Promise<void>;
  isPending: boolean;
  files: File[];
  previews: string[];
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onRemoveFile: (index: number) => void;
  fileInputRef: React.RefObject<HTMLInputElement | null>;
}

/**
 * 새로운 문의 및 건의사항 작성을 위한 폼 컴포넌트입니다.
 */
export function FeedbackCreateForm({
  onSubmit,
  isPending,
  files,
  previews,
  onFileChange,
  onRemoveFile,
  fileInputRef
}: FeedbackCreateFormProps) {
  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      type: "SUGGESTION",
      title: "",
      content: "",
    },
  });

  const handleSubmit = async (values: FormValues) => {
    await onSubmit(values);
    form.reset();
  };

  return (
    <div className="animate-in fade-in slide-in-from-bottom-2 duration-300">
      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">
          {/* 분류 선택 */}
          <FormField
            control={form.control}
            name="type"
            render={({ field }: { field: ControllerRenderProps<FormValues, "type"> }) => (
              <FormItem className="space-y-3">
                <div className="flex items-center gap-2 mb-2">
                  <span className="text-[13px] font-bold text-gray-900 dark:text-gray-100">문의 유형</span>
                </div>
                <FormControl>
                  <div className="flex flex-wrap gap-2">
                    {[
                      { value: "BUG", label: "버그 제보", icon: ShieldAlert, color: "text-red-500" },
                      { value: "SUGGESTION", label: "기능 건의", icon: Lightbulb, color: "text-amber-500" },
                      { value: "OTHER", label: "기타 문의", icon: HelpCircle, color: "text-blue-500" },
                    ].map((item) => (
                      <button 
                        key={item.value}
                        type="button" 
                        onClick={() => field.onChange(item.value)} 
                        className={cn(
                          "px-4 py-2.5 text-[13px] font-bold rounded-lg border transition-all flex items-center gap-2",
                          field.value === item.value 
                            ? "bg-primary text-white border-primary shadow-sm" 
                            : "bg-white dark:bg-black/20 text-gray-500 border-gray-200 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-white/5"
                        )}
                      >
                        <item.icon className={cn("w-3.5 h-3.5", field.value === item.value ? "text-white" : item.color)} />
                        {item.label}
                      </button>
                    ))}
                  </div>
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* 제목 입력 */}
          <FormField
            control={form.control}
            name="title"
            render={({ field }: { field: ControllerRenderProps<FormValues, "title"> }) => (
              <FormItem className="space-y-2">
                <div className="flex items-center gap-1.5 mb-1 text-[13px] font-bold text-gray-900 dark:text-gray-100">
                  제목
                </div>
                <FormControl>
                  <Input 
                    placeholder="제목을 입력해주세요." 
                    className="h-12 border-gray-200 dark:border-gray-800 bg-white dark:bg-black/20 focus-visible:ring-primary/20 font-bold px-4" 
                    {...field} 
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* 내용 입력 */}
          <FormField
            control={form.control}
            name="content"
            render={({ field }: { field: ControllerRenderProps<FormValues, "content"> }) => (
              <FormItem className="space-y-2">
                <div className="flex items-center gap-1.5 mb-1 text-[13px] font-bold text-gray-900 dark:text-gray-100">
                  내용
                </div>
                <FormControl>
                  <Textarea 
                    placeholder="상세한 내용을 입력해 주시면 더 정확한 답변을 드릴 수 있습니다." 
                    className="min-h-[200px] border-gray-200 dark:border-gray-800 bg-white dark:bg-black/20 focus-visible:ring-primary/20 font-medium p-4 resize-none leading-relaxed" 
                    {...field} 
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* 이미지 첨부 */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[13px] font-bold text-gray-900 dark:text-gray-100">이미지 첨부 (최대 3장)</span>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="h-9 border-gray-200 dark:border-gray-800 text-[12px] font-bold"
                onClick={() => fileInputRef.current?.click()}
              >
                <ImageIcon className="w-3.5 h-3.5 mr-1.5" /> 파일 선택
              </Button>
            </div>
            
            <input type="file" className="hidden" accept="image/*" multiple ref={fileInputRef} onChange={onFileChange} />

            {previews.length > 0 ? (
              <div className="grid grid-cols-3 sm:grid-cols-6 gap-3 pt-1">
                {previews.map((preview, index) => (
                  <div key={index} className="relative aspect-square rounded-lg overflow-hidden border border-gray-100 dark:border-gray-800">
                    <Image src={preview} alt="미리보기" fill className="object-cover" />
                    <button 
                      type="button" 
                      onClick={() => onRemoveFile(index)} 
                      className="absolute top-1 right-1 bg-black/50 text-white rounded-full p-0.5 hover:bg-black/70 transition-colors"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-8 border border-dashed border-gray-200 dark:border-gray-800 rounded-lg flex flex-col items-center justify-center bg-gray-50/50 dark:bg-black/10">
                <ImageIcon className="w-6 h-6 text-gray-300 mb-2" />
                <p className="text-[12px] text-gray-400 font-medium text-center">관련된 스크린샷이 있다면 첨부해주세요.</p>
              </div>
            )}
          </div>

          <div className="pt-4 border-t border-gray-100 dark:border-gray-800">
            <Button 
              type="submit" 
              className="w-full h-12 text-[15px] font-bold" 
              disabled={isPending}
            >
              {isPending ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> 전송 중...</> : "문의 등록하기"}
            </Button>
          </div>
        </form>
      </Form>
    </div>
  );
}
