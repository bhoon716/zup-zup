"use client";

import React, { useMemo } from 'react';
import { TimetableResponse } from '@/types/api';
import { cn } from '@/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, RenderingBlock } from '@/lib/utils/timetable';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
}

const DAYS = ['월', '화', '수', '목', '금'];
const HOURS = Array.from({ length: 15 }, (_, i) => i + 8); // 08:00 ~ 22:00

export function TimetableGrid({ timetable, className }: TimetableGridProps) {
  // 1. 데이터 평탄화 및 충돌 체크 (Util 이관)
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);

  const GRID_START_TIME = 8 * 60; // 08:00
  const GRID_END_TIME = 22 * 60;   // 22:00
  const TOTAL_MINUTES = GRID_END_TIME - GRID_START_TIME;

  return (
    <div className={cn("relative border rounded-lg bg-background overflow-hidden", className)}>
      <div className="grid grid-cols-[60px_repeat(5,1fr)] border-b divide-x bg-muted/30">
        <div className="h-10 flex items-center justify-center text-xs font-medium text-muted-foreground">시간</div>
        {DAYS.map((day) => (
          <div key={day} className="h-10 flex items-center justify-center text-sm font-bold">
            {day}
          </div>
        ))}
      </div>

      <div className="relative grid grid-cols-[60px_repeat(5,1fr)] divide-x min-h-[700px]">
        {/* Time Labels */}
        <div className="flex flex-col">
          {HOURS.map((hour) => (
            <div key={hour} className="flex-1 border-b last:border-0 relative">
              <span className="absolute -top-3 left-0 right-0 text-[10px] text-center text-muted-foreground bg-background px-1 z-10">
                {String(hour).padStart(2, '0')}:00
              </span>
            </div>
          ))}
        </div>

        {/* Day Columns & Grid Lines */}
        {DAYS.map((day) => (
          <div key={day} className="relative flex-1 group">
             {/* Horizontal Lines */}
            {HOURS.map((hour) => (
              <div key={hour} className="border-b last:border-0 flex-1 h-[calc(100%/14)]" />
            ))}

            {/* Blocks for this day */}
            {blocks
              .filter((b: RenderingBlock) => b.dayOfWeek === day)
              .map((block: RenderingBlock) => {
                const start = getTimeInMinutes(block.startTime);
                const end = getTimeInMinutes(block.endTime);
                
                const topPercent = ((start - GRID_START_TIME) / TOTAL_MINUTES) * 100;
                const heightPercent = ((end - start) / TOTAL_MINUTES) * 100;

                return (
                  <div
                    key={block.key}
                    className={cn(
                      "absolute left-1 right-1 p-1.5 rounded-sm text-[10px] leading-tight overflow-hidden transition-all hover:z-50 shadow-sm",
                      block.type === 'course' ? "bg-primary/10 border-l-2 border-primary text-primary-foreground dark:text-primary" : "border-l-2",
                      block.isOverlap && "after:absolute after:inset-0 after:bg-[repeating-linear-gradient(45deg,transparent,transparent_5px,rgba(0,0,0,0.05)_5px,rgba(0,0,0,0.05)_10px)] after:pointer-events-none"
                    )}
                    style={{
                      top: `${topPercent}%`,
                      height: `${heightPercent}%`,
                      backgroundColor: block.type === 'custom' ? `${block.color}20` : undefined,
                      borderColor: block.type === 'custom' ? block.color : undefined,
                      color: block.type === 'custom' ? block.color : undefined,
                    }}
                  >
                    <div className="font-bold truncate">{block.title}</div>
                    {block.subTitle && <div className="text-[9px] opacity-80 truncate">{block.subTitle}</div>}
                    <div className="text-[8px] mt-0.5 opacity-60">
                      {block.startTime} - {block.endTime}
                    </div>
                  </div>
                );
              })}
          </div>
        ))}
      </div>
    </div>
  );
}
