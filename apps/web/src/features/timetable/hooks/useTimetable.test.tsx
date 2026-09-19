import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import {
  useTimetables,
  useTimetableDetail,
  usePrimaryTimetable,
  useAddCourseToTimetable,
  useAddCustomSchedule,
} from './useTimetable';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';
import * as timetableApi from '@/features/timetable/api/timetable.api';
import { useUser } from '@/features/user/hooks/useUser';
import { toast } from 'sonner';

vi.mock('@/features/user/hooks/useUser', () => ({
  useUser: vi.fn(),
}));

vi.mock('@/features/auth/store/useAuthStore', () => ({
  useAuthStore: (selector: (state: { isAuthenticated: boolean }) => unknown) =>
    selector({ isAuthenticated: true }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

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
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useUser).mockReturnValue({ data: mockUser } as never);
  });

  it('useTimetables fetches all timetables', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useTimetables(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(2);
  });

  it('인증 스토어가 인증된 상태면 사용자 쿼리 데이터 없이 시간표를 조회한다', async () => {
    vi.mocked(useUser).mockReturnValue({ data: null } as never);

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

  it('useTimetableDetail does not fetch when disabled', async () => {
    const spy = vi.spyOn(timetableApi.timetableApi, 'getTimetable');
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    renderHook(() => useTimetableDetail(1, false), { wrapper });

    expect(spy).not.toHaveBeenCalled();
    spy.mockRestore();
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

  it('useAddCustomSchedule shows the server error message when adding fails', async () => {
    const error = { response: { data: { message: '시간이 겹치는 일정이 있습니다.' } } };
    const addCustomScheduleSpy = vi.spyOn(timetableApi.timetableApi, 'addCustomSchedule')
      .mockRejectedValue(error);
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useAddCustomSchedule(), { wrapper });

    await act(async () => {
      await expect(result.current.mutateAsync({
        timetableId: 1,
        data: {
          title: '중복 일정',
          professor: '',
          schedules: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }],
        },
      })).rejects.toBe(error);
    });

    expect(toast.error).toHaveBeenCalledWith('시간이 겹치는 일정이 있습니다.');
    await waitFor(() => expect(result.current.isError).toBe(true));
    addCustomScheduleSpy.mockRestore();
  });
});
