"use client";

import type { CSSProperties } from 'react';
import { cn } from '@/shared/lib/utils';
import type { RenderingBlock } from '@/features/timetable/lib/timetable';
import styles from './timetable-block.module.css';

export type OpenTimetableBlock = (block: RenderingBlock, trigger: HTMLButtonElement) => void;

export interface TimetableBlockLayout {
  leftOffset: number;
  widthFraction: number;
  coveredFraction: number;
  layer: number;
}

const FULL_WIDTH_LAYOUT: TimetableBlockLayout = { leftOffset: 0, widthFraction: 1, coveredFraction: 0, layer: 0 };

interface TimetableBlockProps {
  block: RenderingBlock;
  top: string;
  height: string;
  layout?: TimetableBlockLayout;
  isPreview: boolean;
  onOpen: OpenTimetableBlock;
}

export function TimetableBlock({ block, top, height, layout = FULL_WIDTH_LAYOUT, isPreview, onOpen }: TimetableBlockProps) {
  const color = block.color ?? '#56296e';
  const start = block.startTime.slice(0, 5);
  const end = block.endTime.slice(0, 5);
  const time = `${start}–${end}`;
  const position: CSSProperties & { '--block-layer': number } = {
    top,
    height,
    left: `${layout.leftOffset * 100}%`,
    width: `${layout.widthFraction * 100}%`,
    '--block-layer': layout.layer,
  };

  return (
    <button
      type="button"
      aria-label={[block.title, `${block.dayOfWeek}요일 ${time}`, block.subTitle, block.classroom].filter(Boolean).join(', ')}
      aria-haspopup="dialog"
      data-testid={`timetable-block-${block.key}`}
      className="pointer-events-auto absolute z-(--block-layer) cursor-pointer p-px text-left focus-visible:z-20 focus-visible:rounded-lg focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-slate-900"
      style={position}
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
          paddingInlineEnd: `${layout.coveredFraction * 100}%`,
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
