import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import {
  useTimetables,
  useTimetableDetail,
  usePrimaryTimetable,
  useAddCourseToTimetable,
} from './useTimetable';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';

const mockUser = {
  id: 1,
  email: 'test@jbnu.ac.kr',
  name: 'Tester',
  role: 'USER',
  notificationEmail: 'test@jbnu.ac.kr',
  emailEnabled: true,
  webPushEnabled: true,
  fcmEnabled: true,
  discordEnabled: false,
  onboardingCompleted: true,
};

const mockTimetables = [
  { id: 1, name: 'Default', primary: true },
  { id: 2, name: 'Secondary', primary: false },
];

const mockDetail = {
  id: 1,
  name: 'Default',
  primary: true,
  courses: [],
  customSchedules: [],
  totalCredits: '0',
};

const handlers = [
  http.get('*/api/v1/users/me', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockUser,
    });
  }),
  http.get('*/api/v1/timetables', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockTimetables,
    });
  }),
  http.get('*/api/v1/timetables/1', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockDetail,
    });
  }),
  http.get('*/api/v1/timetables/primary', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockDetail,
    });
  }),
  http.post('*/api/v1/timetables/1/courses', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Course added',
      data: null,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
});
afterAll(() => server.close());

describe('useTimetable hooks', () => {
  it('useTimetables fetches all timetables', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useTimetables(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(2);
  });

  it('useTimetableDetail fetches details for a specific timetable', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useTimetableDetail(1), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.id).toBe(1);
  });

  it('usePrimaryTimetable fetches the primary timetable', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => usePrimaryTimetable(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.primary).toBe(true);
  });

  it('useAddCourseToTimetable adds a course to a timetable', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useAddCourseToTimetable(), { wrapper });

    result.current.mutate({ timetableId: 1, courseKey: 'COURSE1' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});
