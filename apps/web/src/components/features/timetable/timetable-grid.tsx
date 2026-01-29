"use client";

import React, { useMemo } from 'react';
import { TimetableResponse } from '@/types/api';
import { cn } from '@/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, RenderingBlock } from '@/lib/utils/timetable';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
  isPreview?: boolean;
}

const DAYS = ['월', '화', '수', '목', '금', '토', '일'];

export function TimetableGrid({ timetable, className, isPreview = false }: TimetableGridProps) {
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);

  // 1. 가변적 시간 범위 계산
  const { startHour, endHour, hoursArray } = useMemo(() => {
    let minMin = 9 * 60; // 기본 시작 09:00
    let maxMin = 18 * 60; // 기본 종료 18:00

    blocks.forEach(block => {
      const start = getTimeInMinutes(block.startTime);
      const end = getTimeInMinutes(block.endTime);
      if (start < minMin) minMin = start;
      if (end > maxMin) maxMin = end;
    });

    const sHour = Math.floor(minMin / 60);
    const eHour = Math.ceil(maxMin / 60);
    const arr = Array.from({ length: eHour - sHour }, (_, i) => sHour + i);

    return { startHour: sHour, endHour: eHour, hoursArray: arr };
  }, [blocks]);

  const GRID_START_TIME = startHour * 60;
  const TOTAL_MINUTES = (endHour - startHour) * 60;

  return (
    <div className={cn(
      "relative border rounded-xl bg-background overflow-hidden shadow-sm", 
      isPreview && "rounded-[1.5rem] border-none shadow-none",
      className
    )}>
      {/* Header */}
      <div className={cn(
        "grid grid-cols-[60px_repeat(7,1fr)] border-b bg-muted/30 divide-x divide-border/50",
        isPreview && "grid-cols-[40px_repeat(7,1fr)] h-8"
      )}>
        <div className={cn(
          "h-10 flex items-center justify-center text-[10px] font-bold text-muted-foreground uppercase tracking-wider",
          isPreview && "h-8 text-[8px]"
        )}>시간</div>
        {DAYS.map((day) => (
          <div 
            key={day} 
            className={cn(
              "h-10 flex items-center justify-center text-sm font-black transition-colors",
              isPreview && "h-8 text-[11px]",
              day === '토' && "bg-blue-500/10 text-blue-600 dark:text-blue-400 border-b-2 border-b-blue-500/50",
              day === '일' && "bg-red-500/10 text-red-600 dark:text-red-400 border-b-2 border-b-red-500/50"
            )}
          >
            {day}
          </div>
        ))}
      </div>

      <div className={cn(
        "relative grid grid-cols-[60px_repeat(7,1fr)] divide-x divide-border/50 min-h-[650px]",
        isPreview && "grid-cols-[40px_repeat(7,1fr)] min-h-[450px]"
      )}>
        {/* Time Labels Column */}
        <div className="flex flex-col bg-muted/5 divide-y divide-border/30">
          {hoursArray.map((hour) => {
            const period = hour >= 9 ? hour - 8 : null;
            return (
              <div key={hour} className={cn(
                "flex-1 relative flex flex-col items-center justify-center py-4 group/row",
                isPreview && "py-2"
              )}>
                {period !== null && (
                  <span className={cn(
                    "text-[9px] font-bold text-primary/80 leading-none mb-1",
                    isPreview && "text-[7px] mb-0.5"
                  )}>
                    {period}교시
                  </span>
                )}
                <span className={cn(
                  "text-[10px] tabular-nums text-muted-foreground font-medium leading-none",
                  isPreview && "text-[8px]"
                )}>
                  {String(hour).padStart(2, '0')}:00
                </span>
                
                {/* Visual grid line helper */}
                <div className="absolute bottom-0 left-full w-[500%] h-[1px] bg-border/20 z-0 pointer-events-none" />
              </div>
            );
          })}
          {/* Bottom border helper for the very last row */}
          <div className="h-px bg-border/20" />
        </div>

        {/* Day Columns */}
        {DAYS.map((day) => (
          <div 
            key={day} 
            className="relative flex-1 bg-background/50"
          >
            {/* Grid background lines */}
            <div className="absolute inset-0 flex flex-col divide-y divide-border/20">
              {hoursArray.map((hour) => (
                <div key={hour} className="flex-1" />
              ))}
            </div>

            {/* Blocks for this day */}
            <div className="relative h-full z-10 mx-[2px]">
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
                        "absolute left-0 right-0 p-2 rounded-lg text-[10px] leading-tight overflow-hidden transition-all hover:z-50 shadow-md border group/block",
                        isPreview && "p-1 rounded-md text-[8px]",
                        block.type === 'course' 
                          ? "bg-primary/5 border-primary/20 hover:border-primary/40 text-primary-foreground dark:text-primary" 
                          : "border-border/50 hover:border-border",
                        block.isOverlap && "after:absolute after:inset-0 after:bg-[repeating-linear-gradient(45deg,transparent,transparent_5px,rgba(244,63,94,0.1)_5px,rgba(244,63,94,0.1)_10px)] after:pointer-events-none border-rose-500/50"
                      )}
                      style={{
                        top: `${topPercent}%`,
                        height: `${heightPercent}%`,
                        backgroundColor: block.type === 'custom' ? `${block.color}15` : undefined,
                        borderColor: block.type === 'custom' ? `${block.color}40` : undefined,
                        color: block.type === 'custom' ? block.color : undefined,
                      }}
                    >
                      <div className={cn(
                        "font-extrabold truncate text-[11px] mb-0.5 tracking-tight",
                        isPreview && "text-[9px] mb-0"
                      )}>{block.title}</div>
                      {block.subTitle && !isPreview && (
                        <div className="text-[9px] opacity-80 truncate font-semibold mb-1">{block.subTitle}</div>
                      )}
                      
                      {!isPreview && (
                        <div className="flex items-center gap-1.5 mt-auto">
                          <span className="text-[8px] font-mono font-medium opacity-60">
                            {block.startTime}~{block.endTime}
                          </span>
                          {block.period && (
                            <span className="px-1 py-0.5 bg-primary/10 text-primary rounded-[3px] font-black text-[7px] uppercase tracking-tighter">
                              {block.period}
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
