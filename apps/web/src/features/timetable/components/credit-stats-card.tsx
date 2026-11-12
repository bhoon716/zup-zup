"use client";

import React from 'react';
import { GraduationCap, Info } from 'lucide-react';
import { cn } from '@/shared/lib/utils';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/ui/tooltip';

interface CreditStatsCardProps {
  totalCredits: number;
  maxCredits?: number;
}

export function CreditStatsCard({ totalCredits, maxCredits = 18 }: CreditStatsCardProps) {

  const [open, setOpen] = React.useState(false);

  const creditPercent = Math.max(0, Math.min(100, (totalCredits / maxCredits) * 100));
  const isCreditWarning = totalCredits < 6 || totalCredits > maxCredits;
  const creditWarningMessage = totalCredits < 6 ? '하한 학점 경고' : '상한 학점 경고';

  return (
    <div className="rounded-lg bg-muted p-3">
      <div className="mb-2 flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 text-sm font-semibold text-foreground">
          <GraduationCap className={cn('size-4', isCreditWarning ? 'text-amber-700 dark:text-amber-400' : 'text-primary')} />
          총 신청 학점
          {isCreditWarning && (
            <Tooltip open={open} onOpenChange={setOpen} delayDuration={0}>
              <TooltipTrigger asChild>
                <button
                  type="button"
                  aria-label={creditWarningMessage}
                  className="inline-flex size-6 cursor-help items-center justify-center rounded-full text-amber-700 transition-colors hover:bg-amber-500/15 dark:text-amber-400"
                  onClick={() => setOpen((prev) => !prev)}
                  onMouseEnter={() => setOpen(true)}
                  onMouseLeave={() => setOpen(false)}
                >
                  <Info className="size-3.5" />
                </button>
              </TooltipTrigger>
              <TooltipContent side="top" className="mb-2">
                <p>{creditWarningMessage}</p>
              </TooltipContent>
            </Tooltip>
          )}
        </div>
        <span className={cn('text-sm font-semibold tabular-nums', isCreditWarning ? 'text-amber-700 dark:text-amber-400' : 'text-primary')}>
          {totalCredits}
          <span className="ml-1 text-xs font-medium text-muted-foreground">/ {maxCredits}</span>
        </span>
      </div>

      <div className="h-1.5 overflow-hidden rounded-full bg-foreground/10">
        <div
          className={cn('h-full rounded-full transition-all', isCreditWarning ? 'bg-yellow-500' : 'bg-primary')}
          style={{ width: `${creditPercent}%` }}
        />
      </div>
    </div>
  );
}
