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
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      {                 }
      <div className="mb-3 flex items-center justify-between">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-600">
          <GraduationCap className={cn('h-3.5 w-3.5', isCreditWarning ? 'text-yellow-500' : 'text-primary')} />
          총 신청 학점
          {isCreditWarning && (
            <Tooltip open={open} onOpenChange={setOpen} delayDuration={0}>
              <TooltipTrigger asChild>
                <button 
                  type="button" 
                  className="ml-1 inline-flex items-center justify-center p-0.5 rounded-full hover:bg-yellow-100 transition-colors cursor-help outline-hidden"
                  onClick={() => setOpen((prev) => !prev)}
                  onMouseEnter={() => setOpen(true)}
                  onMouseLeave={() => setOpen(false)}
                >
                  <Info className="h-3.5 w-3.5 text-yellow-500" />
                </button>
              </TooltipTrigger>
              <TooltipContent side="top" className="mb-2">
                <p>{creditWarningMessage}</p>
              </TooltipContent>
            </Tooltip>
          )}
        </div>
        <span className={cn('text-sm font-bold', isCreditWarning ? 'text-yellow-500' : 'text-primary')}>
          {totalCredits}
          <span className="ml-1 text-xs font-medium text-slate-400">/ {maxCredits}</span>
        </span>
      </div>
      
      {              }
      <div className="h-1.5 overflow-hidden rounded-full bg-slate-200">
        <div
          className={cn('h-full rounded-full transition-all', isCreditWarning ? 'bg-yellow-500' : 'bg-primary')}
          style={{ width: `${creditPercent}%` }}
        />
      </div>
    </div>
  );
}
