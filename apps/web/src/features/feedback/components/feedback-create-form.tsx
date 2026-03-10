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
 * 피드백 작성을 위한 폼 컴포넌트
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

  /**
   * 폼 전송 이벤트를 처리하고 폼 내부 상태를 초기화합니다.
   */
  const handleSubmit = async (values: FormValues) => {
    await onSubmit(values);
    form.reset();
  };

  return (
    <>
      <p className="text-gray-500 font-medium mb-4 text-sm">
        서비스를 이용하며 겪은 불편함이나 개선 아이디어를 들려주세요.
      </p>
      
      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
          <div className="flex flex-col gap-4">
            <FormField
              control={form.control}
              name="type"
              render={({ field }: { field: ControllerRenderProps<FormValues, "type"> }) => (
                <FormItem>
                  <FormControl>
                    <div className="flex bg-gray-100/50 dark:bg-[#202020] p-1.5 rounded-2xl w-full relative">
                      <button 
                        type="button" 
                        onClick={() => field.onChange("BUG")} 
                        className={cn(
                          "flex-1 py-3 text-sm font-bold rounded-xl flex items-center justify-center gap-2 transition-all",
                          field.value === "BUG" 
                            ? "bg-white dark:bg-[#2A2A2A] text-gray-900 dark:text-white shadow-[0_2px_8px_rgba(0,0,0,0.08)]" 
                            : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                        )}
                      >
                        <ShieldAlert className={cn("w-4 h-4", field.value === "BUG" ? "text-red-500" : "opacity-60")} />
                        버그 제보
                      </button>
                      <button 
                        type="button" 
                        onClick={() => field.onChange("SUGGESTION")} 
                        className={cn(
                          "flex-1 py-3 text-sm font-bold rounded-xl flex items-center justify-center gap-2 transition-all",
                          field.value === "SUGGESTION" 
                            ? "bg-white dark:bg-[#2A2A2A] text-gray-900 dark:text-white shadow-[0_2px_8px_rgba(0,0,0,0.08)]" 
                            : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                        )}
                      >
                        <Lightbulb className={cn("w-4 h-4", field.value === "SUGGESTION" ? "text-amber-500" : "opacity-60")} />
                        기능 건의
                      </button>
                      <button 
                        type="button" 
                        onClick={() => field.onChange("OTHER")} 
                        className={cn(
                          "flex-1 py-3 text-sm font-bold rounded-xl flex items-center justify-center gap-2 transition-all",
                          field.value === "OTHER" 
                            ? "bg-white dark:bg-[#2A2A2A] text-gray-900 dark:text-white shadow-[0_2px_8px_rgba(0,0,0,0.08)]" 
                            : "text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
                        )}
                      >
                        <HelpCircle className={cn("w-4 h-4", field.value === "OTHER" ? "text-primary" : "opacity-60")} />
                        기타 문의
                      </button>
                    </div>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button
              type="button"
              variant="outline"
              className="w-full rounded-2xl border-dashed border-gray-200 dark:border-gray-800 h-14 bg-white dark:bg-[#1A1A1A] hover:bg-primary/5 hover:border-primary/30 transition-all gap-2"
              onClick={() => fileInputRef.current?.click()}
            >
              <ImageIcon className="w-5 h-5 text-gray-400" />
              <span className="text-sm font-bold text-gray-500 dark:text-gray-400">{files.length > 0 ? `${files.length}장 선택됨` : "사진 첨부 (선택, 최대 3장)"}</span>
            </Button>
            <input type="file" className="hidden" accept="image/*" multiple ref={fileInputRef} onChange={onFileChange} />
          </div>

          {previews.length > 0 && (
            <div className="flex gap-2.5 overflow-x-auto pb-1 scrollbar-hide">
              {previews.map((preview, index) => (
                <div key={index} className="relative flex-none">
                  <Image src={preview} alt="미리보기" width={64} height={64} className="w-16 h-16 rounded-xl object-cover border border-gray-100" />
                  <button type="button" onClick={() => onRemoveFile(index)} className="absolute -top-1.5 -right-1.5 bg-gray-900/80 text-white rounded-full p-0.5 border border-white"><X className="w-2.5 h-2.5" /></button>
                </div>
              ))}
            </div>
          )}

          <FormField
            control={form.control}
            name="title"
            render={({ field }: { field: ControllerRenderProps<FormValues, "title"> }) => (
              <FormItem>
                <FormControl>
                  <Input placeholder="제목을 입력해주세요." className="rounded-xl border-gray-100 bg-white h-11 focus:ring-primary/20 font-semibold" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="content"
            render={({ field }: { field: ControllerRenderProps<FormValues, "content"> }) => (
              <FormItem>
                <FormControl>
                  <Textarea placeholder="선택하신 유형에 맞게 내용을 상세히 적어주세요." className="rounded-2xl border-gray-100 bg-white min-h-[140px] resize-none focus:ring-primary/20 font-medium leading-relaxed" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <Button type="submit" className="w-full rounded-2xl h-12 text-sm font-bold shadow-lg shadow-primary/20 active:scale-95 transition-all" disabled={isPending}>
            {isPending ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> 전송 중...</> : "피드백 보내기"}
          </Button>
        </form>
      </Form>
    </>
  );
}
