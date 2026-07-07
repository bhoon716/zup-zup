"use client";

import React from 'react';
import { GraduationCap, Info } from 'lucide-react';
import { cn } from '@/shared/lib/utils';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/shared/ui/tooltip';

interface CreditStatsCardProps {
  totalCredits: number;
  maxCredits?: number;
}

/**
 * 시간표의 총 신청 학점 현황을 시각화하여 보여주는 카드 컴포넌트입니다.
 * 학점 범위를 벗어날 경우(6학점 미만, 18학점 초과) 경고 색상과 툴팁을 표시합니다.
 */
export function CreditStatsCard({ totalCredits, maxCredits = 18 }: CreditStatsCardProps) {
  // 모바일 사용자를 위해 수동으로 툴팁 상태 관리
  const [open, setOpen] = React.useState(false);
  
  // 학점 범위 및 경고 메시지 계산
  const creditPercent = Math.max(0, Math.min(100, (totalCredits / maxCredits) * 100));
  const isCreditWarning = totalCredits < 6 || totalCredits > maxCredits;
  const creditWarningMessage = totalCredits < 6 ? '하한 학점 경고' : '상한 학점 경고';

  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      {/* 제목 및 학점 표시부 */}
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
      
      {/* 학점 진척도 바 */}
      <div className="h-1.5 overflow-hidden rounded-full bg-slate-200">
        <div
          className={cn('h-full rounded-full transition-all', isCreditWarning ? 'bg-yellow-500' : 'bg-primary')}
          style={{ width: `${creditPercent}%` }}
        />
      </div>
    </div>
  );
}
