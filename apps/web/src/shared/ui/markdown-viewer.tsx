"use client";

import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "@/shared/lib/utils";

interface MarkdownViewerProps {
  /** 렌더링할 마크다운 문자열 */
  content: string;
  /** 최상위 컨테이너에 추가할 클래스 */
  className?: string;
}

/**
 * 전용 마크다운 렌더링 라이브러리(react-markdown)를 사용하여 
 * 마크다운 콘텐츠를 표준 HTML로 변환하여 보여주는 컴포넌트입니다.
 * GitHub Flavored Markdown(GFM) 기능을 지원하며, Tailwind CSS로 각 요소를 스타일링합니다.
 */
export function MarkdownViewer({ content, className }: MarkdownViewerProps) {
  if (!content || !content.trim()) {
    return <p className={cn("text-sm text-slate-500 italic", className)}>내용이 없습니다.</p>;
  }

  return (
    <div className={cn("markdown-content max-w-none wrap-anywhere", className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          // 제목 스타일링 (h1 ~ h6)
          h1: ({ className, ...props }) => (
            <h1 className={cn("mt-6 mb-4 text-3xl font-black text-slate-900 border-b pb-2", className)} {...props} />
          ),
          h2: ({ className, ...props }) => (
            <h2 className={cn("mt-6 mb-3 text-2xl font-black text-slate-900", className)} {...props} />
          ),
          h3: ({ className, ...props }) => (
            <h3 className={cn("mt-5 mb-2 text-xl font-bold text-slate-900", className)} {...props} />
          ),
          h4: ({ className, ...props }) => (
            <h4 className={cn("mt-4 mb-2 text-lg font-bold text-slate-900", className)} {...props} />
          ),

          // 본문 문단
          p: ({ className, ...props }) => (
            <p className={cn("mb-4 text-sm leading-7 text-slate-700 last:mb-0", className)} {...props} />
          ),

          // 목록 스타일링
          ul: ({ className, ...props }) => (
            <ul className={cn("mb-4 ml-5 list-disc space-y-1 text-sm leading-7 text-slate-700", className)} {...props} />
          ),
          ol: ({ className, ...props }) => (
            <ol className={cn("mb-4 ml-5 list-decimal space-y-1 text-sm leading-7 text-slate-700", className)} {...props} />
          ),
          li: ({ className, ...props }) => (
            <li className={cn("pl-1", className)} {...props} />
          ),

          // 인용구
          blockquote: ({ className, ...props }) => (
            <blockquote
              className={cn(
                "mt-4 mb-4 border-l-4 border-primary/40 bg-primary/5 px-4 py-2 text-sm italic text-slate-600 rounded-r-xl",
                className
              )}
              {...props}
            />
          ),

          // 코드 블록 & 인라인 코드
          code: ({ className, children, ...props }) => {
            // react-markdown은 인라인 코드일 때 inline 속성을 주지 않는 경우가 있어 
            // children의 형태나 className 유무로 판단할 수도 있음.
            const isInline = !className?.includes("language-");
            
            if (isInline) {
              return (
                <code
                  className={cn(
                    "rounded bg-slate-100 px-1.5 py-0.5 text-[0.85em] font-medium text-primary-dark/80",
                    className
                  )}
                  {...props}
                >
                  {children}
                </code>
              );
            }

            return (
              <pre className="mt-4 mb-4 overflow-x-auto rounded-xl bg-slate-900 p-4 text-xs leading-relaxed text-slate-100 scrollbar-thin scrollbar-thumb-slate-700 scrollbar-track-transparent">
                <code className={cn("font-mono", className)} {...props}>
                  {children}
                </code>
              </pre>
            );
          },

          // 링크
          a: ({ className, ...props }) => (
            <a
              className={cn("font-bold text-primary underline underline-offset-4 hover:text-primary-dark transition-colors", className)}
              target="_blank"
              rel="noreferrer"
              {...props}
            />
          ),

          // 가로줄
          hr: ({ className, ...props }) => (
            <hr className={cn("my-8 border-slate-200", className)} {...props} />
          ),

          // 테이블
          table: ({ className, ...props }) => (
            <div className="my-6 w-full overflow-x-auto rounded-xl border border-slate-200">
              <table className={cn("w-full border-collapse text-sm text-slate-700", className)} {...props} />
            </div>
          ),
          thead: ({ className, ...props }) => <thead className={cn("bg-slate-50", className)} {...props} />,
          th: ({ className, ...props }) => (
            <th className={cn("border-b border-slate-200 px-4 py-2.5 text-left font-bold text-slate-900", className)} {...props} />
          ),
          td: ({ className, ...props }) => (
            <td className={cn("border-b border-slate-100 px-4 py-3", className)} {...props} />
          ),
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
}

