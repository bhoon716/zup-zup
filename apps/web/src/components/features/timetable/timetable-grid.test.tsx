import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TimetableGrid } from './timetable-grid';
import { TimetableResponse } from '@/types/api';

describe('TimetableGrid', () => {
  const mockTimetable: TimetableResponse = {
    id: 1,
    name: 'Test Timetable',
    isPrimary: true,
    entries: [
      {
        id: 1,
        courseKey: 'COURSE1',
        courseName: 'Java Programming',
        professor: 'John Doe',
        schedules: [
          { dayOfWeek: '월', period: '1-A', startTime: '09:00', endTime: '10:15' }
        ]
      }
    ],
    customSchedules: [
      {
        id: 1,
        title: 'Lunch',
        dayOfWeek: '화',
        startTime: '12:00',
        endTime: '13:00',
        color: '#FF0000'
      }
    ]
  };

  it('renders all blocks correctly', () => {
    render(<TimetableGrid timetable={mockTimetable} />);
    
    expect(screen.getByText('Java Programming')).toBeInTheDocument();
    expect(screen.getByText('John Doe')).toBeInTheDocument();
    expect(screen.getByText('09:00 - 10:15')).toBeInTheDocument();
    
    expect(screen.getByText('Lunch')).toBeInTheDocument();
    expect(screen.getByText('12:00 - 13:00')).toBeInTheDocument();
  });

  it('renders days and hours labels', () => {
    render(<TimetableGrid timetable={mockTimetable} />);
    
    expect(screen.getByText('월')).toBeInTheDocument();
    expect(screen.getByText('화')).toBeInTheDocument();
    expect(screen.getByText('수')).toBeInTheDocument();
    expect(screen.getByText('목')).toBeInTheDocument();
    expect(screen.getByText('금')).toBeInTheDocument();

    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('09:00')).toBeInTheDocument();
    expect(screen.getByText('10:00')).toBeInTheDocument();
  });

  it('applies overlap style when blocks overlap', () => {
    const overlappingTimetable: TimetableResponse = {
      ...mockTimetable,
      entries: [
        ...mockTimetable.entries,
        {
          id: 2,
          courseKey: 'COURSE2',
          courseName: 'Database',
          professor: 'Jane Smith',
          schedules: [
            { dayOfWeek: '월', period: '1-B', startTime: '10:00', endTime: '11:15' }
          ]
        }
      ]
    };

    const { container } = render(<TimetableGrid timetable={overlappingTimetable} />);
    
    // Check for striped pattern (repeating-linear-gradient)
    // The class is conditionally added when isOverlap is true
    const overlappingBlocks = container.querySelectorAll('.after\\:absolute');
    expect(overlappingBlocks.length).toBeGreaterThanOrEqual(2);
  });
});
