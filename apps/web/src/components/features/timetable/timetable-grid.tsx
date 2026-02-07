"use client";

import React, { useMemo, useState } from 'react';
import { TimetableResponse } from '@/types/api';
import { cn } from '@/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, RenderingBlock, formatPeriod } from '@/lib/utils/timetable';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
  isPreview?: boolean;
}

const DAYS = ['월', '화', '수', '목', '금', '토', '일'];

export function TimetableGrid({ timetable, className, isPreview = false }: TimetableGridProps) {
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);
  const [overlapDialogOpen, setOverlapDialogOpen] = useState(false);
  const [selectedOverlap, setSelectedOverlap] = useState<{
    currentBlock: string;
    overlappingBlocks: { title: string; subTitle?: string }[];
    timeRange: string;
  } | null>(null);

  const [courseDetailOpen, setCourseDetailOpen] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<RenderingBlock | null>(null);

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

  const handleOverlapClick = (
    block: RenderingBlock,
    region: { startTime: string; endTime: string; overlappingBlocks: { title: string; subTitle?: string }[] }
  ) => {
    setSelectedOverlap({
      currentBlock: block.title,
      overlappingBlocks: region.overlappingBlocks,
      timeRange: `${region.startTime} - ${region.endTime}`,
    });
    setOverlapDialogOpen(true);
  };

  const handleCourseClick = (block: RenderingBlock) => {
    setSelectedCourse(block);
    setCourseDetailOpen(true);
  };

  return (
    <>
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
            "flex items-center justify-center text-[10px] font-medium text-muted-foreground",
            isPreview && "text-[7px]"
          )}>
            시간
          </div>
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

        {/* Time Grid */}
        <div className="grid grid-cols-[60px_repeat(7,1fr)] divide-x divide-border/50">
          {/* Time column */}
          <div className="divide-y divide-border/50 bg-muted/10">
            {hoursArray.map((hour) => (
              <div
                key={hour}
                className={cn(
                  "h-16 flex items-start justify-center pt-1 text-[9px] text-muted-foreground font-medium",
                  isPreview && "h-12 text-[7px]"
                )}
              >
                {hour}:00
              </div>
            ))}
          </div>

          {/* Day columns */}
          {DAYS.map((day) => (
            <div key={day} className="relative divide-y divide-border/50">
              {hoursArray.map((hour) => (
                <div
                  key={hour}
                  className={cn(
                    "h-16 bg-background relative",
                    isPreview && "h-12"
                  )}
                />
              ))}

              {/* Timetable blocks for this day */}
              <div className="absolute inset-0 pointer-events-none">
                {blocks
                  .filter((block) => block.dayOfWeek === day)
                  .map((block: RenderingBlock) => {
                    const start = getTimeInMinutes(block.startTime);
                    const end = getTimeInMinutes(block.endTime);
                    
                    const topPercent = ((start - GRID_START_TIME) / TOTAL_MINUTES) * 100;
                    const heightPercent = ((end - start) / TOTAL_MINUTES) * 100;

                    return (
                      <div
                        key={block.key}
                        className={cn(
                          "absolute left-0 right-0 p-1.5 rounded text-[9px] leading-snug overflow-hidden transition-shadow duration-150 hover:z-50 hover:shadow-md border cursor-pointer pointer-events-auto",
                          isPreview && "p-1 text-[7px]"
                        )}
                        style={{
                          top: `${topPercent}%`,
                          height: `${heightPercent}%`,
                          backgroundColor: block.color ? `${block.color}10` : '#f9fafb',
                          borderColor: block.color || '#e5e7eb',
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          if (block.overlapRegions && block.overlapRegions.length > 0) {
                            handleOverlapClick(block, block.overlapRegions[0]);
                          } else {
                            handleCourseClick(block);
                          }
                        }}
                      >
                        {/* Overlap region warnings - now just visual indicator */}
                        {block.overlapRegions?.map((region, idx) => {
                          const regionStart = getTimeInMinutes(region.startTime);
                          const regionEnd = getTimeInMinutes(region.endTime);
                          const blockStart = getTimeInMinutes(block.startTime);
                          const blockEnd = getTimeInMinutes(block.endTime);
                          const blockDuration = blockEnd - blockStart;
                          
                          const regionTopPercent = ((regionStart - blockStart) / blockDuration) * 100;
                          const regionHeightPercent = ((regionEnd - regionStart) / blockDuration) * 100;
                          
                          return (
                            <div
                              key={`overlap-${idx}`}
                              className="absolute left-0 right-0 bg-rose-50/90 border-y border-rose-200 flex items-center justify-center pointer-events-none"
                              style={{
                                top: `${regionTopPercent}%`,
                                height: `${regionHeightPercent}%`,
                              }}
                            >
                              <div className="flex items-center gap-1 text-rose-600">
                                <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                                </svg>
                                <span className="text-[8px] font-semibold">겹침</span>
                              </div>
                            </div>
                          );
                        })}
                        
                        <div 
                          className={cn(
                            "font-medium truncate text-[10px] leading-tight relative z-10",
                            isPreview && "text-[8px]"
                          )}
                          style={{ color: block.color || '#1f2937' }}
                        >
                          {block.title}
                        </div>
                        {block.subTitle && !isPreview && (
                          <div 
                            className="text-[8px] opacity-60 truncate leading-tight mt-0.5 relative z-10"
                            style={{ color: block.color || '#6b7280' }}
                          >
                            {block.subTitle}
                          </div>
                        )}
                        
                        {!isPreview && heightPercent > 8 && (
                          <div className="mt-1 relative z-10">
                            <span 
                              className="text-[7px] font-mono opacity-50 leading-tight"
                              style={{ color: block.color || '#9ca3af' }}
                            >
                              {block.startTime}-{block.endTime}
                            </span>
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

      {/* Overlap Dialog */}
      <Dialog open={overlapDialogOpen} onOpenChange={setOverlapDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-rose-600">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              시간표 겹침 경고
            </DialogTitle>
          </DialogHeader>
          {selectedOverlap && (
            <div className="space-y-4">
              <div className="bg-muted/50 rounded-lg p-3">
                <p className="text-sm font-medium mb-1">현재 수업</p>
                <p className="text-sm text-muted-foreground">{selectedOverlap.currentBlock}</p>
              </div>
              
              <div className="bg-rose-50 rounded-lg p-3">
                <p className="text-sm font-medium mb-2 text-rose-700">겹치는 시간</p>
                <p className="text-sm text-rose-600 font-mono">{selectedOverlap.timeRange}</p>
              </div>

              <div>
                <p className="text-sm font-medium mb-2">겹치는 수업 목록</p>
                <div className="space-y-2">
                  {selectedOverlap.overlappingBlocks.map((overlap, idx) => (
                    <div key={idx} className="flex items-start gap-2 text-sm bg-muted/30 rounded-md p-2">
                      <span className="text-muted-foreground">•</span>
                      <div>
                        <p className="font-medium">{overlap.title}</p>
                        {overlap.subTitle && (
                          <p className="text-xs text-muted-foreground">{overlap.subTitle}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Course Detail Dialog */}
      <Dialog open={courseDetailOpen} onOpenChange={setCourseDetailOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <svg className="w-5 h-5 text-primary" fill="currentColor" viewBox="0 0 20 20">
                <path d="M9 4.804A7.968 7.968 0 005.5 4c-1.255 0-2.443.29-3.5.804v10A7.969 7.969 0 015.5 14c1.669 0 3.218.51 4.5 1.385A7.962 7.962 0 0114.5 14c1.255 0 2.443.29 3.5.804v-10A7.968 7.968 0 0014.5 4c-1.255 0-2.443.29-3.5.804V12a1 1 0 11-2 0V4.804z" />
              </svg>
              수업 상세 정보
            </DialogTitle>
          </DialogHeader>
          {selectedCourse && (
            <div className="space-y-4">
              <div className="bg-muted/50 rounded-lg p-4">
                <h3 className="text-lg font-bold mb-2" style={{ color: selectedCourse.color || '#1f2937' }}>
                  {selectedCourse.title}
                </h3>
                {selectedCourse.subTitle && (
                  <p className="text-sm text-muted-foreground">{selectedCourse.subTitle}</p>
                )}
              </div>
              
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-background rounded-lg p-3 border">
                  <p className="text-xs text-muted-foreground mb-1">요일</p>
                  <p className="text-sm font-semibold">{selectedCourse.dayOfWeek}요일</p>
                </div>
                <div className="bg-background rounded-lg p-3 border">
                  <p className="text-xs text-muted-foreground mb-1">시간</p>
                  <p className="text-sm font-semibold font-mono">{selectedCourse.startTime} - {selectedCourse.endTime}</p>
                </div>
              </div>

              {selectedCourse.period && (
                <div className="bg-primary/5 rounded-lg p-3">
                  <p className="text-xs text-muted-foreground mb-1">교시</p>
                  <p className="text-sm font-semibold text-primary">{formatPeriod(selectedCourse.period)}</p>
                </div>
              )}

              <div className="bg-gradient-to-r from-background to-muted/30 rounded-lg p-3 border-l-4" style={{ borderLeftColor: selectedCourse.color || '#e5e7eb' }}>
                <p className="text-xs text-muted-foreground mb-1">수업 유형</p>
                <p className="text-sm font-semibold">{selectedCourse.type === 'course' ? '정규 수업' : '사용자 지정'}</p>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}

