"use client";

import { useId, useMemo, useRef, useState, type CSSProperties } from 'react';
import { MoveHorizontal } from 'lucide-react';
import type { Course, TimetableResponse } from '@/shared/types/api';
import { parseCourseKey } from '@/shared/lib/course';
import { cn } from '@/shared/lib/utils';
import { getRenderingBlocks, getTimeInMinutes, WEEK_DAYS, type RenderingBlock } from '@/features/timetable/lib/timetable';
import { CourseDetailDialog } from '@/features/course/components/course-detail-dialog';
import { CustomScheduleDetailDialog } from './custom-schedule-detail-dialog';
import { TimetableBlock, type OpenTimetableBlock } from './timetable-block';

interface TimetableGridProps {
  timetable: TimetableResponse;
  className?: string;
  isPreview?: boolean;
  allowHorizontalScroll?: boolean;
}

const MONDAY_TO_SATURDAY = WEEK_DAYS.slice(0, 6);
interface BlockLayout {
  leftOffset: number;
  widthFraction: number;
}

function sortBlocksByTime(a: RenderingBlock, b: RenderingBlock): number {
  const startDiff = getTimeInMinutes(a.startTime) - getTimeInMinutes(b.startTime);
  if (startDiff !== 0) {
    return startDiff;
  }

  return getTimeInMinutes(a.endTime) - getTimeInMinutes(b.endTime);
}

function buildLayoutsForDay(dayBlocks: RenderingBlock[]): Map<string, BlockLayout> {
  const layouts = new Map<string, BlockLayout>();
  if (dayBlocks.length === 0) {
    return layouts;
  }

  const clusters: RenderingBlock[][] = [];
  let currentCluster: RenderingBlock[] = [];
  let clusterEnd = -1;

  for (const block of dayBlocks) {
    const start = getTimeInMinutes(block.startTime);
    const end = getTimeInMinutes(block.endTime);

    if (currentCluster.length === 0) {
      currentCluster.push(block);
      clusterEnd = end;
      continue;
    }

    if (start < clusterEnd) {
      currentCluster.push(block);
      clusterEnd = Math.max(clusterEnd, end);
      continue;
    }

    clusters.push(currentCluster);
    currentCluster = [block];
    clusterEnd = end;
  }

  if (currentCluster.length > 0) {
    clusters.push(currentCluster);
  }

  for (const cluster of clusters) {
    const columnEndTimes: number[] = [];
    const placements: { block: RenderingBlock; column: number }[] = [];

    for (const block of cluster) {
      const start = getTimeInMinutes(block.startTime);
      const end = getTimeInMinutes(block.endTime);

      let column = columnEndTimes.findIndex((columnEnd) => start >= columnEnd);
      if (column === -1) {
        column = columnEndTimes.length;
        columnEndTimes.push(end);
      } else {
        columnEndTimes[column] = end;
      }

      placements.push({ block, column });
    }

    const totalColumns = Math.max(columnEndTimes.length, 1);
    placements.forEach(({ block, column }) => {
      layouts.set(block.key, {
        leftOffset: column / totalColumns,
        widthFraction: 1 / totalColumns,
      });
    });
  }

  return layouts;
}

export function TimetableGrid({ timetable, className, isPreview = false, allowHorizontalScroll = false }: TimetableGridProps) {
  const blocks = useMemo(() => getRenderingBlocks(timetable), [timetable]);
  const displayDays = blocks.some((block) => block.dayOfWeek === '일') ? WEEK_DAYS : MONDAY_TO_SATURDAY;
  const [selectedBlock, setSelectedBlock] = useState<RenderingBlock | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const returnFocusRef = useRef<HTMLButtonElement>(null);
  const scrollHintId = useId();
  const hasWideColumns = allowHorizontalScroll && !isPreview;

  const initialCourse = useMemo(() => {
    if (selectedBlock?.type !== 'course') return null;
    const courseKey = selectedBlock.courseKey ?? '';
    const { subjectCode, classNumber } = parseCourseKey(courseKey);
    return {
      courseKey,
      subjectCode,
      classNumber,
      name: selectedBlock.title,
      professor: selectedBlock.subTitle,
      credits: selectedBlock.credits,
      classification: selectedBlock.classification,
      classroom: selectedBlock.classroom,
      classTime: `${selectedBlock.dayOfWeek}요일 ${selectedBlock.startTime.slice(0, 5)}–${selectedBlock.endTime.slice(0, 5)}`,
    } as Course;
  }, [selectedBlock]);

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
  const dayBlocksMap = useMemo(() => {
    const map = new Map<string, RenderingBlock[]>();

    displayDays.forEach((day) => {
      const sortedBlocks = blocks
        .filter((block) => block.dayOfWeek === day)
        .sort(sortBlocksByTime);
      map.set(day, sortedBlocks);
    });

    return map;
  }, [blocks, displayDays]);

  const dayLayoutMap = useMemo(() => {
    const map = new Map<string, Map<string, BlockLayout>>();

    displayDays.forEach((day) => {
      map.set(day, buildLayoutsForDay(dayBlocksMap.get(day) ?? []));
    });

    return map;
  }, [dayBlocksMap, displayDays]);

  const handleOpenBlock: OpenTimetableBlock = (block, trigger) => {
    returnFocusRef.current = trigger;
    setSelectedBlock(block);
    setDetailOpen(true);
  };

  const columns = 'grid grid-cols-[var(--time-column)_repeat(var(--day-count),minmax(0,1fr))]';

  return (
    <div className="@container relative min-w-0">
      {hasWideColumns && (
        <p
          id={scrollHintId}
          className={cn(
            'flex items-center gap-1.5 border-b border-border px-3 py-2 text-xs text-muted-foreground',
            displayDays.length === 7 ? '@min-[55.75rem]:hidden' : '@min-[48.25rem]:hidden',
          )}
        >
          <MoveHorizontal aria-hidden="true" className="size-3.5" />
          좌우로 밀어 요일 보기
        </p>
      )}
      <div
        role="region"
        aria-label="주간 시간표"
        aria-describedby={hasWideColumns ? scrollHintId : undefined}
        tabIndex={hasWideColumns ? 0 : undefined}
        className="custom-scrollbar w-full overflow-x-auto bg-white focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-primary"
      >
        <div
          className={cn(
            'timetable-grid-content relative bg-white',
            isPreview
              ? '[--time-column:40px] [--slot-height:45px]'
              : '[--time-column:42px] [--slot-height:42px] md:[--time-column:52px] md:[--slot-height:60px]',
            hasWideColumns ? 'min-w-[max(100%,calc(var(--time-column)+var(--day-count)*7.5rem))]' : 'min-w-full',
            className,
          )}
          style={{ '--day-count': displayDays.length } as CSSProperties}
        >
          <div className={cn(columns, 'sticky top-0 z-40 border-b border-slate-200 bg-slate-50', isPreview && 'h-8')}>
            <div className="sticky left-0 z-10 border-r border-slate-200 bg-slate-50" />
            {displayDays.map((day) => (
              <div
                key={day}
                className={cn(
                  'flex items-center justify-center border-r border-slate-200/70 p-2 text-center text-xs font-semibold text-slate-700 last:border-r-0 md:py-3 md:text-sm',
                  isPreview && 'p-0.5 text-[10px]',
                  (day === '토' || day === '일') && 'text-slate-500',
                )}
              >
                {day}
              </div>
            ))}
          </div>

          <div className={cn(columns, 'relative')}>
            <div className="sticky left-0 z-30 col-start-1 row-start-1 border-r border-slate-200 bg-slate-50 select-none">
              {hoursArray.map((hour) => (
                <div key={hour} className="flex h-(--slot-height) flex-col items-center justify-center border-b border-slate-200 text-slate-600">
                  <span className={cn('text-[10px] font-semibold tabular-nums md:text-xs', isPreview && 'text-[8px]')}>
                    {hour - 8}
                  </span>
                  <span className={cn('text-[9px] tabular-nums', isPreview && 'text-[7px]')}>
                    {hour}:00
                  </span>
                </div>
              ))}
            </div>

            <div className="pointer-events-none col-[2/-1] row-start-1 grid grid-cols-[repeat(var(--day-count),minmax(0,1fr))]">
              {displayDays.map((day) => (
                <div key={day} className="h-full border-r border-slate-100 last:border-r-0" />
              ))}
            </div>

            <div className="pointer-events-none col-[2/-1] row-start-1">
              {hoursArray.map((hour) => (
                <div key={hour} className="relative h-(--slot-height) border-b border-slate-100">
                  <div className="absolute inset-x-0 top-1/2 border-t border-dashed border-slate-100" />
                </div>
              ))}
            </div>

            <div className="pointer-events-none z-10 col-[2/-1] row-start-1 grid h-full grid-cols-[repeat(var(--day-count),minmax(0,1fr))]">
              {displayDays.map((day) => (
                <div key={day} className="relative h-full">
                  {dayBlocksMap.get(day)?.map((block) => {
                    const start = getTimeInMinutes(block.startTime);
                    const end = getTimeInMinutes(block.endTime);
                    const top = `calc((${start} - ${GRID_START_TIME}) / 60 * var(--slot-height))`;
                    const height = `calc((${end} - ${start}) / 60 * var(--slot-height))`;
                    const layout = dayLayoutMap.get(day)?.get(block.key);

                    return (
                      <TimetableBlock
                        key={block.key}
                        block={block}
                        top={top}
                        height={height}
                        leftOffset={layout?.leftOffset ?? 0}
                        widthFraction={layout?.widthFraction ?? 1}
                        isPreview={isPreview}
                        onOpen={handleOpenBlock}
                      />
                    );
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <CourseDetailDialog
        course={initialCourse}
        open={detailOpen && selectedBlock?.type === 'course'}
        onOpenChange={setDetailOpen}
        returnFocusRef={returnFocusRef}
      />

      <CustomScheduleDetailDialog
        schedule={selectedBlock?.type === 'custom' ? timetable.customSchedules?.find(({ id }) => id === selectedBlock.id) : undefined}
        timetableId={timetable.id}
        open={detailOpen && selectedBlock?.type === 'custom'}
        onOpenChange={setDetailOpen}
        returnFocusRef={returnFocusRef}
      />
    </div>
  );
}
