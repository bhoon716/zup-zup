import { render, screen } from '@testing-library/react';
import type { ComponentProps } from 'react';
import { describe, it, expect } from 'vitest';
import { TimetableGrid } from './timetable-grid';
import { TimetableResponse } from '@/shared/types/api';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TooltipProvider } from '@/shared/ui/tooltip';

type GridOptions = Pick<ComponentProps<typeof TimetableGrid>, 'isPreview' | 'allowHorizontalScroll'>;

const stackedPositions = [
  { left: '0%', width: '100%' },
  { left: 'min(22px, 15%)', width: 'calc(100% - min(22px, 15%))' },
  { left: 'min(44px, 30%)', width: 'calc(100% - min(44px, 30%))' },
];

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

describe('TimetableGrid', () => {
  const mockTimetable: TimetableResponse = {
    id: 1,
    name: 'Test Timetable',
    primary: true,
    courses: [
      {
        courseKey: 'COURSE1',
        name: 'Java Programming',
        professor: 'John Doe',
        classTime: '월 09:00-10:15',
        credits: '3',
        classification: '전공',
        classroom: '공학관 101',
        schedules: [
          { dayOfWeek: '월', startTime: '09:00', endTime: '10:15' },
        ],
      },
    ],
    customSchedules: [
      {
        id: 1,
        title: 'Lunch',
        professor: 'Self',
        schedules: [
          { id: 101, dayOfWeek: '화', startTime: '12:00', endTime: '13:00', classroom: 'Cafeteria' },
        ],
      },
    ],
  };

  const renderGrid = (timetable: TimetableResponse, options?: GridOptions) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          <TimetableGrid timetable={timetable} {...options} />
        </TooltipProvider>
      </QueryClientProvider>
    );
  };

  it('renders course and custom blocks with their day and hour labels', () => {
    renderGrid(mockTimetable);

    expect(screen.getByText('Java Programming')).toBeInTheDocument();

    expect(screen.getByText(/John Doe/)).toBeInTheDocument();
    expect(screen.getByText(/공학관 101/)).toBeInTheDocument();

    expect(screen.getByText('Lunch')).toBeInTheDocument();
    expect(screen.getByText('월')).toBeInTheDocument();
    expect(screen.getByText('화')).toBeInTheDocument();
    expect(screen.getByText('수')).toBeInTheDocument();
    expect(screen.getByText('목')).toBeInTheDocument();
    expect(screen.getByText('금')).toBeInTheDocument();

    expect(screen.getByText('9:00', { exact: true })).toBeInTheDocument();
    expect(screen.getByText('10:00', { exact: true })).toBeInTheDocument();
  });

  it.each([
    { isPreview: false, slotClasses: ['[--slot-height:42px]', 'md:[--slot-height:60px]'] },
    { isPreview: true, slotClasses: ['[--slot-height:45px]'] },
  ])('화면에 맞는 시간 칸 높이를 인라인 스타일 태그 없이 지정한다', ({ isPreview, slotClasses }) => {
    const { container } = renderGrid(mockTimetable, { isPreview });
    const grid = container.querySelector<HTMLElement>('.timetable-grid-content');

    expect(grid).not.toBeNull();
    expect(grid).toHaveClass(...slotClasses);
    expect(container.querySelector('style')).not.toBeInTheDocument();
  });

  it.each<{ options: GridOptions; label: string }>([
    { options: {}, label: '대시보드' },
    { options: { allowHorizontalScroll: true }, label: '내 시간표' },
    { options: { isPreview: true }, label: '미리보기' },
  ])('$label에서 충돌한 일정의 배치와 요일 칸 경계를 지킨다', ({ options }) => {
    renderGrid({
      ...mockTimetable,
      customSchedules: [
        { id: 2, title: '동시 일정 1', schedules: [{ id: 102, dayOfWeek: '월', startTime: '09:30', endTime: '10:15' }] },
        { id: 3, title: '동시 일정 2', schedules: [{ id: 103, dayOfWeek: '월', startTime: '10:00', endTime: '10:15' }] },
        { id: 4, title: '바로 다음 일정', schedules: [{ id: 104, dayOfWeek: '월', startTime: '10:15', endTime: '11:00' }] },
        { id: 5, title: '다른 요일 일정', schedules: [{ id: 105, dayOfWeek: '화', startTime: '09:30', endTime: '10:15' }] },
      ],
    }, options);

    expect(screen.getByRole('button', { name: /바로 다음 일정/ })).toHaveStyle({ left: '0%', width: '100%' });
    expect(screen.getByRole('button', { name: /다른 요일 일정/ })).toHaveStyle({ left: '0%', width: '100%' });

    [/Java Programming/, /동시 일정 1/, /동시 일정 2/].forEach((name, index) => {
      const card = screen.getByRole('button', { name });
      if (options.isPreview) {
        expect(parseFloat(card.style.left)).toBeCloseTo(index * 100 / 3);
        expect(parseFloat(card.style.width)).toBeCloseTo(100 / 3);
        return;
      }

      expect(card).toHaveStyle(stackedPositions[index]);
    });
  });
});
