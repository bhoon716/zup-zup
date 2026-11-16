import { ChevronLeft, ChevronRight } from "lucide-react";
import { Fragment } from "react";

import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";

export function FeedbackPagination({ page, totalPages, onChange, className }: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
  className?: string;
}) {
  const pages = [...new Set([0, page - 1, page, page + 1, totalPages - 1])]
    .filter((number) => number >= 0 && number < totalPages)
    .sort((first, second) => first - second);

  return (
    <nav aria-label="문의 목록 페이지" className={cn("flex flex-wrap items-center justify-center gap-1", className)}>
      <Button type="button" variant="ghost" size="icon-sm" aria-label="이전 페이지" disabled={page === 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft aria-hidden="true" />
      </Button>
      {pages.map((number, index) => (
        <Fragment key={number}>
          {index > 0 && number - pages[index - 1] > 1 && <span aria-hidden="true" className="text-xs text-muted-foreground">…</span>}
          <Button
            type="button"
            variant={number === page ? "default" : "ghost"}
            size="icon-sm"
            aria-label={`${number + 1} 페이지`}
            aria-current={number === page ? "page" : undefined}
            onClick={() => onChange(number)}
          >
            {number + 1}
          </Button>
        </Fragment>
      ))}
      <Button type="button" variant="ghost" size="icon-sm" aria-label="다음 페이지" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        <ChevronRight aria-hidden="true" />
      </Button>
    </nav>
  );
}
