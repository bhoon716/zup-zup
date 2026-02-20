"use client";

import React, { useMemo, useState } from 'react';
import { TimetableResponse } from '@/shared/types/api';
import { cn } from '@/shared/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, RenderingBlock } from '@/features/timetable/lib/timetable';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/shared/ui/dialog';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
  isPreview?: boolean;
}

const DAYS = ['월', '화', '수', '목', '금', '토'];
const DEFAULT_BLOCK_COLOR = '#56296E';

// HEX 색상 코드를 RGB 객체로 변환합니다.
// 유효하지 않은 색상 코드인 경우 null을 반환합니다.
const hexToRgb = (color: string) => {
  if (!color.startsWith('#')) {
    return null;
  }

  const normalized = color.length === 4
    ? `#${color[1]}${color[1]}${color[2]}${color[2]}${color[3]}${color[3]}`
    : color;
  const value = normalized.slice(1);
  if (value.length !== 6) {
    return null;
  }

  const num = Number.parseInt(value, 16);
  if (Number.isNaN(num)) {
    return null;
  }

  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
};

// 주어진 색상에 투명도(alpha)를 적용한 rgba 문자열을 생성합니다.
// 색상 변환 실패 시 기본 색상(fallback)을 반환합니다.
const withAlpha = (color: string | undefined, alpha: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) {
    return fallback;
  }

  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
};

// 주어진 색상과 흰색을 주어진 비율로 혼합하여 좀 더 밝은 rgb 색상을 생성합니다.
// 배경색이나 배지 색상을 기존 테마와 맞추기 위해 사용됩니다.
const mixWithWhite = (color: string | undefined, whiteRatio: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) {
    return fallback;
  }

  const ratio = Math.max(0, Math.min(1, whiteRatio));
  const mix = (value: number) => Math.round(value + (255 - value) * ratio);
  return `rgb(${mix(rgb.r)}, ${mix(rgb.g)}, ${mix(rgb.b)})`;
};

// 시간표의 그리드 UI를 렌더링하고 각 강의 블록을 배치하는 컴포넌트입니다.
// 시작/종료 시간을 동적으로 계산하여 스크롤 및 배치 레이아웃을 생성합니다.
export function TimetableGrid({ timetable, className, isPreview = false }: TimetableGridProps) {
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);
  const [courseDetailOpen, setCourseDetailOpen] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<RenderingBlock | null>(null);

  const { startHour, hoursArray } = useMemo(() => {
    let minMin = 9 * 60;
    let maxMin = 18 * 60;

    blocks.forEach((block) => {
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

  const handleCourseClick = (block: RenderingBlock) => {
    setSelectedCourse(block);
    setCourseDetailOpen(true);
  };

  return (
    <>
      <div
        className={cn(
          'relative bg-white timetable-grid-content',
          !isPreview && 'rounded-2xl shadow-sm min-h-[600px] flex flex-col',
          className
        )}
      >
        <div
          className={cn(
            'grid grid-cols-[60px_repeat(6,1fr)] border-b border-slate-200 sticky top-0 bg-white z-30',
            !isPreview && 'shadow-[0_4px_6px_-4px_rgba(0,0,0,0.05)]',
            isPreview && 'grid-cols-[40px_repeat(6,1fr)] h-8 shadow-none'
          )}
        >
          <div className="p-3 border-r border-slate-200 bg-slate-50/50"></div>
          {DAYS.map((day) => (
            <div
              key={day}
              className={cn(
                'p-2 md:p-3 text-center font-bold text-xs md:text-sm text-slate-600 border-r border-slate-100 bg-white last:border-r-0',
                isPreview && 'p-1 text-[10px]',
                day === '토' && 'text-blue-500 bg-blue-50/30'
              )}
            >
              {day}
            </div>
          ))}
        </div>

        <div className={cn("flex-1 grid grid-cols-[60px_repeat(6,1fr)] relative", isPreview && "grid-cols-[40px_repeat(6,1fr)]")}>
          {/* Y-axis timeline */}
          <div className="col-start-1 col-span-1 row-start-1 border-r border-slate-200 bg-slate-50/30 text-xs text-slate-400 font-medium select-none z-20">
            {hoursArray.map((hour) => (
              <div key={hour} className={cn("border-b border-slate-200 relative flex flex-col items-center justify-center gap-0.5", isPreview ? "h-[45px]" : "h-[60px]")}>
                <span className={cn(
                  "text-[10px] sm:text-[11px] font-bold text-slate-500",
                  isPreview && "text-[8px]"
                )}>
                  {hour - 8}교시
                </span>
                <span className={cn(
                  "text-[8px] sm:text-[9px] text-slate-400",
                  isPreview && "text-[6px]"
                )}>
                  {String(hour).padStart(2, '0')}:00
                </span>
              </div>
            ))}
          </div>

          {/* Vertical Guides */}
          <div className="col-start-2 col-span-6 row-start-1 grid grid-cols-6 pointer-events-none z-0">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className={cn("h-full", i < 5 && "border-r border-slate-100")}></div>
            ))}
          </div>

          {/* Horizontal Guides */}
          <div className="col-start-2 col-span-6 row-start-1 pointer-events-none z-0 flex flex-col">
            {hoursArray.map((hour) => (
              <div key={hour} className={cn("w-full border-b border-slate-100 relative", isPreview ? "h-[45px]" : "h-[60px]")}>
                <div className="absolute top-1/2 w-full border-t border-dashed border-slate-100/50"></div>
              </div>
            ))}
          </div>

          {/* Course Blocks Layer */}
          <div className="col-start-2 col-span-6 row-start-1 grid grid-cols-6 h-full pointer-events-none z-10">
            {DAYS.map((day) => {
              const dayBlocks = blocks.filter(b => b.dayOfWeek === day);
              
              // 겹치는 일정들을 그룹화하여, 좌우(세로 기둥)로 분할 배치
              const groups: typeof blocks[] = [];
              dayBlocks
                .sort((a, b) => getTimeInMinutes(a.startTime) - getTimeInMinutes(b.startTime))
                .forEach((sched) => {
                  let placed = false;
                  for (const group of groups) {
                    const isOverlapping = group.some((item) =>
                      Math.max(getTimeInMinutes(sched.startTime), getTimeInMinutes(item.startTime)) < Math.min(getTimeInMinutes(sched.endTime), getTimeInMinutes(item.endTime))
                    );
                    if (isOverlapping) {
                      group.push(sched);
                      placed = true;
                      break;
                    }
                  }
                  if (!placed) groups.push([sched]);
                });

              return (
                <div key={day} className="relative w-full h-full pointer-events-none hover:z-50">
                  {groups.map((group) => {
                    const count = group.length;
                    return group.map((block, idx) => {
                      const start = getTimeInMinutes(block.startTime);
                      const end = getTimeInMinutes(block.endTime);
                      const SLOT_HEIGHT = isPreview ? 45 : 60;
                      const topPx = ((start - GRID_START_TIME) / 60) * SLOT_HEIGHT;
                      const heightPx = ((end - start) / 60) * SLOT_HEIGHT;

                      const widthFraction = 1 / count;
                      const leftOffset = widthFraction * idx;

                      const baseColor = block.color || DEFAULT_BLOCK_COLOR;
                      const borderColor = baseColor;
                      const badgeBgColor = mixWithWhite(baseColor, 0.90, '#ede9fe');
                      const bgColor = mixWithWhite(baseColor, 0.96, '#F5F3FF');
                      const infoColor = withAlpha(baseColor, 0.65, '#6b7280');
                      const infoText = [block.classroom, block.subTitle].filter(Boolean).join(' • ') || block.subTitle;

                      return (
                        <div
                          key={block.key}
                          data-testid={`timetable-block-${block.key}`}
                          className={cn(
                            'absolute p-0.5 z-10 group cursor-pointer transition-all duration-200 pointer-events-auto',
                            !isPreview && 'p-1'
                          )}
                          style={{
                            top: `${topPx}px`,
                            height: `${heightPx}px`,
                            left: `calc(${leftOffset * 100}%)`,
                            width: `calc(${widthFraction * 100}%)`,
                          }}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleCourseClick(block);
                          }}
                        >
                          <div 
                            className={cn(
                              'w-full h-full p-2 flex flex-col justify-between rounded-lg overflow-hidden border-l-4 transition-transform hover:-translate-y-px',
                              !isPreview && 'hover:shadow-[0_4px_12px_rgba(0,0,0,0.05)]',
                              isPreview && 'p-1 rounded border-l-2'
                            )}
                            style={{ backgroundColor: bgColor, borderLeftColor: borderColor }}
                          >
                            <div className="relative">
                              <h4 
                                className={cn("font-bold leading-tight mb-0.5 break-words", isPreview ? 'text-[8px] line-clamp-2' : 'text-xs sm:text-sm')} 
                                style={{ color: borderColor }}
                              >
                                {block.title}
                              </h4>
                              {!isPreview && infoText && (
                                <p className="text-[10px] sm:text-[11px] font-medium leading-tight truncate" style={{ color: infoColor }}>
                                  {infoText}
                                </p>
                              )}
                            </div>

                            {!isPreview && (
                              <div className="flex items-center gap-1 mt-1">
                                <span
                                  className="text-[9px] sm:text-[10px] px-1.5 py-0.5 rounded font-bold whitespace-nowrap overflow-hidden text-ellipsis"
                                  style={{ backgroundColor: badgeBgColor, color: borderColor }}
                                >
                                  {('badgeText' in block ? block.badgeText as string : undefined) || (block.type === 'course' ? '강의' : '일정')}
                                </span>
                              </div>
                            )}
                            {isPreview && (
                              <div className="mt-0.5 flex items-center">
                                <span className="text-[7px] font-semibold" style={{ color: infoColor }}>
                                   {block.startTime}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      );
                    });
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>

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
              <div className="bg-slate-50 rounded-lg p-4">
                <h3 className="text-lg font-bold mb-2" style={{ color: selectedCourse.color || '#1f2937' }}>
                  {selectedCourse.title}
                </h3>
                {selectedCourse.subTitle && (
                  <p className="text-sm text-slate-500">{selectedCourse.subTitle}</p>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-white rounded-lg p-3 border border-slate-200">
                  <p className="text-xs text-slate-500 mb-1">요일</p>
                  <p className="text-sm font-semibold">{selectedCourse.dayOfWeek}요일</p>
                </div>
                <div className="bg-white rounded-lg p-3 border border-slate-200">
                  <p className="text-xs text-slate-500 mb-1">시간</p>
                  <p className="text-sm font-semibold font-mono">{selectedCourse.startTime} - {selectedCourse.endTime}</p>
                </div>
              </div>

              <div
                className="bg-linear-to-r from-white to-slate-50 rounded-lg p-3 border-l-4"
                style={{ borderLeftColor: selectedCourse.color || '#e5e7eb' }}
              >
                <p className="text-xs text-slate-500 mb-1">수업 유형</p>
                <p className="text-sm font-semibold">{selectedCourse.type === 'course' ? '정규 수업' : '사용자 지정'}</p>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
