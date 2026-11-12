import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TimetableGrid } from './timetable-grid';
import { TimetableResponse } from '@/shared/types/api';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TooltipProvider } from '@/shared/ui/tooltip';

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

  const renderGrid = (timetable: TimetableResponse, isPreview = false) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          <TimetableGrid timetable={timetable} isPreview={isPreview} />
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
    const { container } = renderGrid(mockTimetable, isPreview);
    const grid = container.querySelector<HTMLElement>('.timetable-grid-content');

    expect(grid).not.toBeNull();
    expect(grid).toHaveClass(...slotClasses);
    expect(container.querySelector('style')).not.toBeInTheDocument();
  });
});
