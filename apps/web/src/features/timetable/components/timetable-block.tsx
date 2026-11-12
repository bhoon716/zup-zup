"use client";

import { cn } from '@/shared/lib/utils';
import type { RenderingBlock } from '@/features/timetable/lib/timetable';
import styles from './timetable-block.module.css';

export type OpenTimetableBlock = (block: RenderingBlock, trigger: HTMLButtonElement) => void;

interface TimetableBlockProps {
  block: RenderingBlock;
  top: string;
  height: string;
  leftOffset: number;
  widthFraction: number;
  isPreview: boolean;
  onOpen: OpenTimetableBlock;
}

export function TimetableBlock({ block, top, height, leftOffset, widthFraction, isPreview, onOpen }: TimetableBlockProps) {
  const color = block.color ?? '#56296e';
  const start = block.startTime.slice(0, 5);
  const end = block.endTime.slice(0, 5);
  const time = `${start}–${end}`;

  return (
    <button
      type="button"
      aria-label={[block.title, `${block.dayOfWeek}요일 ${time}`, block.subTitle, block.classroom].filter(Boolean).join(', ')}
      aria-haspopup="dialog"
      data-testid={`timetable-block-${block.key}`}
      className="pointer-events-auto absolute z-10 cursor-pointer p-px text-left focus-visible:z-20 focus-visible:rounded-lg focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-slate-900"
      style={{ top, height, left: `${leftOffset * 100}%`, width: `${widthFraction * 100}%` }}
      onClick={(event) => {
        event.stopPropagation();
        onOpen(block, event.currentTarget);
      }}
    >
      <span
        className={cn(styles.card, 'block hover:brightness-[0.97]', isPreview && styles.preview)}
        style={{
          backgroundColor: `color-mix(in srgb, ${color} 12%, white)`,
          borderInlineStartColor: `color-mix(in srgb, ${color} 70%, #475569)`,
        }}
      >
        <span className={styles.content}>
          <span className={styles.title}>{block.title}</span>
          {!isPreview && block.subTitle && <span className={styles.instructor}>{block.subTitle}</span>}
          {!isPreview && block.classroom && <span className={styles.classroom}>{block.classroom}</span>}
          <span className={cn(styles.time, 'tabular-nums')}>{isPreview ? start : time}</span>
        </span>
      </span>
    </button>
  );
}
