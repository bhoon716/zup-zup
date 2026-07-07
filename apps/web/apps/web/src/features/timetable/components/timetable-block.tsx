"use client";

import React from 'react';
import { cn } from '@/shared/lib/utils';
import { RenderingBlock } from '@/features/timetable/lib/timetable';

interface TimetableBlockProps {
  block: RenderingBlock;
  top: string;
  height: string;
  leftOffset: number;
  widthFraction: number;
  isPreview?: boolean;
  onClick: (block: RenderingBlock) => void;
}

const DEFAULT_BLOCK_COLOR = '#56296E';

// HEX 색상 코드를 RGB 객체로 변환합니다.
const hexToRgb = (color: string) => {
  if (!color.startsWith('#')) return null;
  const normalized = color.length === 4
    ? `#${color[1]}${color[1]}${color[2]}${color[2]}${color[3]}${color[3]}`
    : color;
  const value = normalized.slice(1);
  if (value.length !== 6) return null;
  const num = Number.parseInt(value, 16);
  if (Number.isNaN(num)) return null;
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
};

// 지정된 색상에 흰색을 섞어 부드러운 배경색을 생성합니다.
const mixWithWhite = (color: string | undefined, whiteRatio: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) return fallback;
  const ratio = Math.max(0, Math.min(1, whiteRatio));
  const mix = (value: number) => Math.round(value + (255 - value) * ratio);
  return `rgb(${mix(rgb.r)}, ${mix(rgb.g)}, ${mix(rgb.b)})`;
};

// 지정된 색상에 투명도를 적용한 rgba 색상을 생성합니다.
const withAlpha = (color: string | undefined, alpha: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) return fallback;
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
};

/**
 * 시간표의 개별 수업 블록을 렌더링하는 컴포넌트입니다.
 * 클릭 시 상세 정보를 보여주며, 배경색과 테두리 색상을 강의 테마에 맞춰 동적으로 계산합니다.
 */
export function TimetableBlock({
  block,
  top,
  height,
  leftOffset,
  widthFraction,
  isPreview = false,
  onClick
}: TimetableBlockProps) {
  const baseColor = block.color || DEFAULT_BLOCK_COLOR;
  const borderColor = baseColor;
  const bgColor = mixWithWhite(baseColor, 0.96, '#F5F3FF');
  const infoColor = withAlpha(baseColor, 0.65, '#6b7280');
  const infoText = [block.classroom, block.subTitle].filter(Boolean).join(' • ') || block.subTitle;
  const isNarrow = widthFraction < 0.45;
  const isVeryNarrow = widthFraction < 0.34;

  return (
    <div
      data-testid={`timetable-block-${block.key}`}
      className={cn(
        'absolute p-px z-10 group cursor-pointer transition-all duration-200 pointer-events-auto',
        !isPreview && 'p-px'
      )}
      style={{
        top,
        height,
        left: `calc(${leftOffset * 100}%)`,
        width: `calc(${widthFraction * 100}%)`,
      }}
      onClick={(e) => {
        e.stopPropagation();
        onClick(block);
      }}
    >
      <div 
        className={cn(
          'w-full h-full p-1 flex flex-col justify-between rounded-lg overflow-hidden border-l-[3px] transition-transform hover:-translate-y-px',
          !isPreview && 'hover:shadow-[0_4px_12px_rgba(0,0,0,0.05)] md:p-1.5 md:border-l-4',
          isPreview && 'p-0.5 rounded border-l-2',
          isNarrow && !isPreview && 'p-1'
        )}
        style={{ backgroundColor: bgColor, borderLeftColor: borderColor }}
      >
        <div className="relative">
          <h4 
            className={cn(
              "font-bold leading-tight mb-0 break-keep overflow-hidden",
              isPreview ? 'text-[8px] line-clamp-2' : 'text-[10px] md:text-sm md:mb-0.5 line-clamp-2',
              isVeryNarrow && !isPreview && 'text-[9px] line-clamp-1'
            )} 
            style={{ color: borderColor }}
          >
            {block.title}
          </h4>
          {!isPreview && infoText && !isNarrow && (
            <p className="text-[9px] sm:text-[11px] font-medium leading-tight truncate" style={{ color: infoColor }}>
              {infoText}
            </p>
          )}
        </div>

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
}
