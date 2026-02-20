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

  const renderGrid = (timetable: TimetableResponse) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <TooltipProvider>
          <TimetableGrid timetable={timetable} />
        </TooltipProvider>
      </QueryClientProvider>
    );
  };

  it('renders all blocks correctly', () => {
    renderGrid(mockTimetable);

    expect(screen.getByText('Java Programming')).toBeInTheDocument();
    
    // professor와 classroom은 하나의 문자열로 합쳐져서 렌더링되므로 부분 일치(regex)를 사용합니다.
    expect(screen.getByText(/John Doe/)).toBeInTheDocument();
    expect(screen.getByText(/공학관 101/)).toBeInTheDocument();

    expect(screen.getByText('Lunch')).toBeInTheDocument();
  });

  it('renders days and hours labels', () => {
    renderGrid(mockTimetable);

    expect(screen.getByText('월')).toBeInTheDocument();
    expect(screen.getByText('화')).toBeInTheDocument();
    expect(screen.getByText('수')).toBeInTheDocument();
    expect(screen.getByText('목')).toBeInTheDocument();
    expect(screen.getByText('금')).toBeInTheDocument();

    // 시간을 보여주는 텍스트가 존재하는지 확인 (정확한 매칭을 위해 정규표현식 사용)
    expect(screen.getByText(/9:00/)).toBeInTheDocument();
    expect(screen.getByText(/10:00/)).toBeInTheDocument();
  });
});
