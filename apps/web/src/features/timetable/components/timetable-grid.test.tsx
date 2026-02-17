import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TimetableGrid } from './timetable-grid';
import { TimetableResponse } from '@/shared/types/api';

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
        dayOfWeek: '화',
        startTime: '12:00',
        endTime: '13:00',
        color: '#FF0000',
      },
    ],
  };

  it('renders all blocks correctly', () => {
    render(<TimetableGrid timetable={mockTimetable} />);

    expect(screen.getByText('Java Programming')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('09:00-10:15')).toBeInTheDocument();

    expect(screen.getByText('Lunch')).toBeInTheDocument();
    expect(screen.getByText('12:00-13:00')).toBeInTheDocument();
  });

  it('renders days and hours labels', () => {
    render(<TimetableGrid timetable={mockTimetable} />);

    expect(screen.getByText('월')).toBeInTheDocument();
    expect(screen.getByText('화')).toBeInTheDocument();
    expect(screen.getByText('수')).toBeInTheDocument();
    expect(screen.getByText('목')).toBeInTheDocument();
    expect(screen.getByText('금')).toBeInTheDocument();

    expect(screen.getByText('9:00')).toBeInTheDocument();
    expect(screen.getByText('10:00')).toBeInTheDocument();
    expect(screen.getByText('11:00')).toBeInTheDocument();
  });

  it('applies overlap indicator when blocks overlap', () => {
    const overlappingTimetable: TimetableResponse = {
      ...mockTimetable,
      courses: [
        ...mockTimetable.courses,
        {
          courseKey: 'COURSE2',
          name: 'Database',
          professor: 'Jane Smith',
          classTime: '월 10:00-11:15',
          credits: '3',
          classification: '전공',
          classroom: '공학관 102',
          schedules: [
            { dayOfWeek: '월', startTime: '10:00', endTime: '11:15' },
          ],
        },
      ],
    };

    render(<TimetableGrid timetable={overlappingTimetable} />);

    const overlapLabels = screen.getAllByText('겹침');
    expect(overlapLabels.length).toBeGreaterThanOrEqual(2);
  });
});
