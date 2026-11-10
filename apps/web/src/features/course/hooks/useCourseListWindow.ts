import { useCallback, useLayoutEffect, useRef, useState } from "react";
import { defaultRangeExtractor, useWindowVirtualizer, type Range } from "@tanstack/react-virtual";
import type { Course } from "@/shared/types/api";

const COURSE_LIST_WINDOW_THRESHOLD = 40;
const ESTIMATED_COURSE_CARD_HEIGHT = 132;
const COURSE_CARD_GAP = 10;

export function useCourseListWindow(courses: Course[], activeIndex: number | null) {
  const listRef = useRef<HTMLDivElement>(null);
  const [scrollMargin, setScrollMargin] = useState(0);
  const enabled = courses.length > COURSE_LIST_WINDOW_THRESHOLD;

  useLayoutEffect(() => {
    const list = listRef.current;
    if (!enabled || !list) return;

    const measureListOffset = () => setScrollMargin(list.getBoundingClientRect().top + window.scrollY);
    measureListOffset();

    const observer = new ResizeObserver(measureListOffset);
    observer.observe(list.parentElement ?? list);
    window.addEventListener("resize", measureListOffset);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", measureListOffset);
    };
  }, [enabled]);

  const getItemKey = useCallback((index: number) => `${courses[index].courseKey}-${index}`, [courses]);
  const rangeExtractor = useCallback((range: Range) => {
    const indices = defaultRangeExtractor(range);
    if (activeIndex === null || activeIndex >= range.count || indices.includes(activeIndex)) {
      return indices;
    }
    return [...indices, activeIndex].sort((a, b) => a - b);
  }, [activeIndex]);

  const virtualizer = useWindowVirtualizer({
    count: courses.length,
    enabled,
    estimateSize: () => ESTIMATED_COURSE_CARD_HEIGHT,
    gap: COURSE_CARD_GAP,
    getItemKey,
    scrollMargin,
    overscan: 6,
    rangeExtractor,
  });

  return { listRef, virtualizer, enabled, scrollMargin, getItemKey };
}
