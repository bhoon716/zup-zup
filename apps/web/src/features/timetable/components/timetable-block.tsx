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

// 주어진 색상과 흰색을 주어진 비율로 혼합하여 좀 더 밝은 rgb 색상을 생성합니다.
const mixWithWhite = (color: string | undefined, whiteRatio: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) return fallback;
  const ratio = Math.max(0, Math.min(1, whiteRatio));
  const mix = (value: number) => Math.round(value + (255 - value) * ratio);
  return `rgb(${mix(rgb.r)}, ${mix(rgb.g)}, ${mix(rgb.b)})`;
};

// 주어진 색상에 투명도(alpha)를 적용한 rgba 문자열을 생성합니다.
const withAlpha = (color: string | undefined, alpha: number, fallback: string) => {
  const rgb = color ? hexToRgb(color) : null;
  if (!rgb) return fallback;
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
};

/**
 * 시간표의 개별 강의 블록을 렌더링하는 컴포넌트입니다.
 * 클릭 시 상세 정보를 보여주며, 배경색과 테두리 색상을 강의 테마에 맞춰 계산합니다.
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
  const badgeBgColor = mixWithWhite(baseColor, 0.90, '#ede9fe');
  const bgColor = mixWithWhite(baseColor, 0.96, '#F5F3FF');
  const infoColor = withAlpha(baseColor, 0.65, '#6b7280');
  const infoText = [block.classroom, block.subTitle].filter(Boolean).join(' • ') || block.subTitle;

  return (
    <div
      data-testid={`timetable-block-${block.key}`}
      className={cn(
        'absolute p-0.5 z-10 group cursor-pointer transition-all duration-200 pointer-events-auto',
        !isPreview && 'p-1'
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
          'w-full h-full p-2 flex flex-col justify-between rounded-lg overflow-hidden border-l-4 transition-transform hover:-translate-y-px',
          !isPreview && 'hover:shadow-[0_4px_12px_rgba(0,0,0,0.05)]',
          isPreview && 'p-1 rounded border-l-2'
        )}
        style={{ backgroundColor: bgColor, borderLeftColor: borderColor }}
      >
        <div className="relative">
          <h4 
            className={cn("font-bold leading-tight mb-0.5 break-all", isPreview ? 'text-[8px] line-clamp-2' : 'text-[10px] sm:text-sm')} 
            style={{ color: borderColor }}
          >
            {block.title}
          </h4>
          {!isPreview && infoText && (
            <p className="text-[9px] sm:text-[11px] font-medium leading-tight truncate" style={{ color: infoColor }}>
              {infoText}
            </p>
          )}
        </div>

        {!isPreview && (
          <div className="flex items-center gap-1 mt-1">
            <span
              className="text-[8px] sm:text-[10px] px-1.5 py-0.5 rounded font-bold whitespace-nowrap overflow-hidden text-ellipsis"
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
}
