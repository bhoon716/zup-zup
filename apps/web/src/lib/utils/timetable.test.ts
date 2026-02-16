import { describe, it, expect } from 'vitest';
import { getTimeInMinutes, isOverlapping, getRenderingBlocks } from './timetable';
import { TimetableResponse } from '@/types/api';

describe('Timetable Utils', () => {
  describe('getTimeInMinutes', () => {
    it('should convert time string to minutes correctly', () => {
      expect(getTimeInMinutes('09:00')).toBe(540);
      expect(getTimeInMinutes('10:30')).toBe(630);
      expect(getTimeInMinutes('00:00')).toBe(0);
      expect(getTimeInMinutes('23:59')).toBe(1439);
    });
  });

  describe('isOverlapping', () => {
    it('should detect overlap when intervals overlap', () => {
      expect(isOverlapping('09:00', '10:00', '09:30', '10:30')).toBe(true);
      expect(isOverlapping('09:00', '10:00', '08:30', '09:30')).toBe(true);
      expect(isOverlapping('09:00', '10:00', '09:15', '09:45')).toBe(true);
    });

    it('should detect overlap when one interval contains another', () => {
      expect(isOverlapping('09:00', '11:00', '09:30', '10:30')).toBe(true);
      expect(isOverlapping('09:30', '10:30', '09:00', '11:00')).toBe(true);
    });

    it('should NOT detect overlap when intervals are adjacent', () => {
      expect(isOverlapping('09:00', '10:00', '10:00', '11:00')).toBe(false);
      expect(isOverlapping('10:00', '11:00', '09:00', '10:00')).toBe(false);
    });

    it('should NOT detect overlap when intervals are far apart', () => {
      expect(isOverlapping('09:00', '10:00', '11:00', '12:00')).toBe(false);
    });
  });

  describe('getRenderingBlocks', () => {
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

    it('should flatten and detect overlaps correctly', () => {
      const blocks = getRenderingBlocks(mockTimetable);

      expect(blocks).toHaveLength(3);

      const course1 = blocks.find((b) => b.id === 'COURSE1');
      const course2 = blocks.find((b) => b.id === 'COURSE2');
      const lunch = blocks.find((b) => b.id === 1);

      expect(course1?.isOverlap).toBe(true);
      expect(course2?.isOverlap).toBe(true);
      expect(lunch?.isOverlap).toBe(false);
    });

    it('should not detect overlap on different days', () => {
      const differentDayTimetable: TimetableResponse = {
        ...mockTimetable,
        courses: [
          {
            ...mockTimetable.courses[0],
            schedules: [{ ...mockTimetable.courses[0].schedules[0], dayOfWeek: '월' }],
          },
          {
            ...mockTimetable.courses[1],
            schedules: [{ ...mockTimetable.courses[1].schedules[0], dayOfWeek: '화' }],
          },
        ],
      };

      const blocks = getRenderingBlocks(differentDayTimetable);
      expect(blocks.every((b) => !b.isOverlap)).toBe(true);
    });
  });
});
