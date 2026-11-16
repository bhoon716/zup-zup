import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import {
  useTimetables,
  useTimetableDetail,
  useAddCourseToTimetable,
  useRemoveCourseFromTimetable,
  useAddCustomSchedule,
} from './useTimetable';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';
import * as timetableApi from '@/features/timetable/api/timetable.api';
import { useUser } from '@/features/user/hooks/useUser';
import type { TimetableEntryResponse } from '@/shared/types/api';
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
  http.get('*/api/auth/csrf', () => HttpResponse.json({ token: 'timetable-csrf-token' })),
  http.get('*/api/v1/users/me', () => {
    return HttpResponse.json(mockUser);
  }),
  http.get('*/api/v1/timetables', () => {
    return HttpResponse.json(mockTimetables);
  }),
  http.get('*/api/v1/timetables/1', () => {
    return HttpResponse.json(mockDetail);
  }),
  http.post('*/api/v1/timetables/1/courses', ({ request }) => {
    expect(request.headers.get('X-CSRF-TOKEN')).toBe('timetable-csrf-token');
    return HttpResponse.json(null);
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

  it('useAddCourseToTimetable adds a course to a timetable', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    const { result } = renderHook(() => useAddCourseToTimetable(), { wrapper });

    result.current.mutate({ timetableId: 1, courseKey: 'COURSE1' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it('삭제가 성공하면 상세 재조회가 끝나기 전에 해당 시간표의 강의와 학점을 갱신한다', async () => {
    const removedCourse: TimetableEntryResponse = {
      courseKey: 'COURSE1',
      name: '삭제할 강의',
      professor: '',
      classTime: '',
      credits: '3',
      classification: '',
      classroom: '',
      schedules: [],
    };
    const remainingCourse = { ...removedCourse, courseKey: 'COURSE2', name: '남은 강의', credits: '1.5' };
    const detail = { ...mockDetail, courses: [removedCourse, remainingCourse], totalCredits: '4.5' };
    const deleteResponse = Promise.withResolvers<void>();
    const refetchResponse = Promise.withResolvers<void>();
    let deleteConfirmed = false;

    server.use(
      http.get('*/api/v1/timetables/1', async () => {
        if (!deleteConfirmed) return HttpResponse.json(detail);

        await refetchResponse.promise;
        return new HttpResponse(null, { status: 503 });
      }),
      http.get('*/api/v1/timetables/2', () => HttpResponse.json({ ...detail, id: 2 })),
      http.delete('*/api/v1/timetables/1/courses/COURSE1', async () => {
        await deleteResponse.promise;
        deleteConfirmed = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    const wrapper = createQueryWrapper(createTestQueryClient());
    const { result } = renderHook(() => ({
      selected: useTimetableDetail(1),
      other: useTimetableDetail(2),
      removal: useRemoveCourseFromTimetable(),
    }), { wrapper });
    await waitFor(() => {
      expect(result.current.selected.data?.courses).toEqual(detail.courses);
      expect(result.current.other.data?.courses).toEqual(detail.courses);
    });

    result.current.removal.mutate({ timetableId: 1, courseKey: removedCourse.courseKey });
    try {
      await waitFor(() => expect(result.current.removal.isPending).toBe(true));
      expect(result.current.selected.data?.courses).toEqual(detail.courses);

      deleteResponse.resolve();
      await waitFor(() => {
        expect(result.current.removal.isSuccess).toBe(true);
        expect(result.current.selected.isFetching).toBe(true);
        expect(result.current.selected.data?.courses).toEqual([remainingCourse]);
        expect(result.current.selected.data?.totalCredits).toBe('1.5');
        expect(result.current.other.data?.courses).toEqual(detail.courses);
      });

      refetchResponse.resolve();
      await waitFor(() => expect(result.current.selected.isRefetchError).toBe(true));
      expect(result.current.selected.data?.courses).toEqual([remainingCourse]);
    } finally {
      deleteResponse.resolve();
      refetchResponse.resolve();
    }
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
