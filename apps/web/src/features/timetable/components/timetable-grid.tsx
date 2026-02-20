"use client";

import React, { useMemo, useState } from 'react';
import { TimetableResponse } from '@/shared/types/api';
import { cn } from '@/shared/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, RenderingBlock } from '@/features/timetable/lib/timetable';
import { CourseDetailDialog } from '@/features/course/components/course-detail-dialog';
import { CustomScheduleDetailDialog } from './custom-schedule-detail-dialog';
import { Course } from '@/shared/types/api';
import { Button } from '@/shared/ui/button';

import { TimetableBlock } from './timetable-block';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
  isPreview?: boolean;
}

const DAYS = ['월', '화', '수', '목', '금', '토'];

/**
 * 시간표의 그리드 UI를 렌더링하고 각 강의 블록을 배치하는 컴포넌트입니다.
 * 시작/종료 시간을 동적으로 계산하여 스크롤 및 배치 레이아웃을 생성합니다.
 */
export function TimetableGrid({ timetable, className, isPreview = false }: TimetableGridProps) {
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);
  const [courseDetailOpen, setCourseDetailOpen] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState<RenderingBlock | null>(null);
  const [customDetailOpen, setCustomDetailOpen] = useState(false);

  const initialCourse = useMemo(() => {
    if (!selectedCourse) return null;
    return {
      courseKey: selectedCourse.courseKey || '',
      name: selectedCourse.title,
      professor: selectedCourse.subTitle || '',
      credits: selectedCourse.credits || '',
      classification: selectedCourse.classification || '',
      classroom: selectedCourse.classroom || '',
    } as Course;
  }, [selectedCourse]);

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
    if (block.type === 'course') {
      setCourseDetailOpen(true);
    } else {
      setCustomDetailOpen(true);
    }
  };

  return (
    <>
      <div className="w-full overflow-x-auto custom-scrollbar bg-white rounded-2xl md:bg-transparent">
        <div
          className={cn(
            'relative bg-white timetable-grid-content min-w-[380px] md:min-w-0',
            !isPreview && 'shadow-sm min-h-[500px] md:min-h-[600px] flex flex-col',
            className
          )}
          style={{ '--slot-height': isPreview ? '45px' : '50px' } as React.CSSProperties}
        >
          <style jsx>{`
            @media (min-width: 768px) {
              .timetable-grid-content {
                --slot-height: ${isPreview ? '45px' : '60px'};
              }
            }
          `}</style>
        <div
          className={cn(
            'grid grid-cols-[35px_repeat(6,1fr)] md:grid-cols-[60px_repeat(6,1fr)] border-b border-slate-200 sticky top-0 bg-white z-40',
            !isPreview && 'shadow-[0_4px_6px_-4px_rgba(0,0,0,0.05)]',
            isPreview && 'grid-cols-[40px_repeat(6,1fr)] h-8 shadow-none'
          )}
        >
          <div className="p-3 border-r border-slate-200 bg-slate-50 md:bg-slate-50/50 sticky left-0 z-50"></div>
          {DAYS.map((day) => (
            <div
              key={day}
              className={cn(
                'p-1.5 md:p-3 text-center font-bold text-[10px] md:text-sm text-slate-600 border-r border-slate-100 bg-white last:border-r-0',
                isPreview && 'p-1 text-[10px]',
                day === '토' && 'text-blue-500 bg-blue-50/30'
              )}
            >
              {day}
            </div>
          ))}
        </div>

        <div className={cn("flex-1 grid grid-cols-[35px_repeat(6,1fr)] md:grid-cols-[60px_repeat(6,1fr)] relative", isPreview && "grid-cols-[40px_repeat(6,1fr)]")}>
          {/* Y-axis timeline */}
          <div className="col-start-1 col-span-1 row-start-1 border-r border-slate-200 bg-slate-50 md:bg-slate-50/30 text-xs text-slate-400 font-medium select-none z-30 sticky left-0">
            {hoursArray.map((hour) => (
              <div key={hour} className={cn("border-b border-slate-200 relative flex flex-col items-center justify-center gap-0.5", isPreview ? "h-[45px]" : "h-[50px] md:h-[60px]")}>
                <span className={cn(
                  "text-[8px] md:text-[11px] font-bold text-slate-500",
                  isPreview && "text-[8px]"
                )}>
                  {hour - 8}
                </span>
                <span className={cn(
                  "text-[7px] md:text-[9px] text-slate-400",
                  isPreview && "text-[6px]"
                )}>
                  {hour}:00
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
              <div key={hour} className="w-full border-b border-slate-100 relative h-[var(--slot-height)]">
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
                      
                      const top = `calc((${start} - ${GRID_START_TIME}) / 60 * var(--slot-height))`;
                      const height = `calc((${end} - ${start}) / 60 * var(--slot-height))`;

                      const widthFraction = 1 / count;
                      const leftOffset = widthFraction * idx;

                      return (
                        <TimetableBlock
                          key={block.key}
                          block={block}
                          top={top}
                          height={height}
                          leftOffset={leftOffset}
                          widthFraction={widthFraction}
                          isPreview={isPreview}
                          onClick={handleCourseClick}
                        />
                      );
                    });
                  })}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>

      <CourseDetailDialog 
        course={initialCourse}
        open={courseDetailOpen}
        onOpenChange={setCourseDetailOpen}
      />

      <CustomScheduleDetailDialog
        block={selectedCourse}
        timetableId={timetable.id}
        open={customDetailOpen}
        onOpenChange={setCustomDetailOpen}
      />
    </>
  );
}
