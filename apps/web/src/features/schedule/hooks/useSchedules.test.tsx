import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/shared/api/client';
import * as adminHooks from '@/features/admin/hooks/useAdminSchedules';
import * as scheduleHooks from './useSchedules';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';

vi.mock('@/shared/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

const request = { scheduleType: '수강신청', startDate: '2026-10-26', endDate: '2026-10-27' };
const schedule = { id: 7, ...request, dDay: 'D-Day' };
const response = { data: { code: 'SUCCESS', message: 'ok', data: schedule } };

beforeEach(() => vi.resetAllMocks());

describe.each([
  { name: '일정 페이지', hooks: scheduleHooks, keys: [['schedules']] },
  { name: '관리자 패널', hooks: adminHooks, keys: [['adminSchedules'], ['schedules', 'upcoming']] },
])('$name의 일정 변경 계약', ({ hooks, keys }) => {
  it('생성 결과를 반환하고 기존 범위의 캐시만 무효화한다', async () => {
    vi.mocked(api.post).mockResolvedValueOnce(response);
    const client = createTestQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const { result } = renderHook(() => hooks.useCreateSchedule(), { wrapper: createQueryWrapper(client) });

    await act(async () => {
      expect(await result.current.mutateAsync(request)).toEqual(schedule);
    });

    expect(api.post).toHaveBeenCalledExactlyOnceWith('/api/v1/admin/schedules', request);
    expect(invalidate.mock.calls).toEqual(keys.map(queryKey => [{ queryKey }]));
  });

  it('수정 대상 id와 요청 본문을 그대로 전달한다', async () => {
    vi.mocked(api.put).mockResolvedValueOnce(response);
    const client = createTestQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const { result } = renderHook(() => hooks.useUpdateSchedule(), { wrapper: createQueryWrapper(client) });

    await act(async () => {
      expect(await result.current.mutateAsync({ id: 7, request })).toEqual(schedule);
    });

    expect(api.put).toHaveBeenCalledExactlyOnceWith('/api/v1/admin/schedules/7', request);
    expect(invalidate.mock.calls).toEqual(keys.map(queryKey => [{ queryKey }]));
  });

  it('삭제 실패는 전파하고 성공한 뒤에만 캐시를 무효화한다', async () => {
    const failure = new Error('삭제 실패');
    vi.mocked(api.delete).mockRejectedValueOnce(failure).mockResolvedValueOnce({ status: 204 });
    const client = createTestQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const { result } = renderHook(() => hooks.useDeleteSchedule(), { wrapper: createQueryWrapper(client) });

    await act(async () => {
      await expect(result.current.mutateAsync(7)).rejects.toBe(failure);
    });
    expect(invalidate).not.toHaveBeenCalled();

    await act(async () => {
      expect(await result.current.mutateAsync(7)).toBeUndefined();
    });
    expect(api.delete).toHaveBeenNthCalledWith(1, '/api/v1/admin/schedules/7');
    expect(api.delete).toHaveBeenNthCalledWith(2, '/api/v1/admin/schedules/7');
    expect(invalidate.mock.calls).toEqual(keys.map(queryKey => [{ queryKey }]));
  });
});

it('공개 일정의 enabled와 두 관리자 조회의 기존 캐시 키를 유지한다', async () => {
  vi.mocked(api.get).mockResolvedValue({ data: { data: [schedule] } });
  const client = createTestQueryClient();
  const wrapper = createQueryWrapper(client);
  const { result, rerender } = renderHook(({ enabled }) => scheduleHooks.useUpcomingSchedules(enabled), {
    wrapper, initialProps: { enabled: false },
  });
  expect(api.get).not.toHaveBeenCalled();

  rerender({ enabled: true });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
  expect(api.get).toHaveBeenCalledWith('/api/v1/schedules');
  expect(client.getQueryData(['schedules', 'upcoming'])).toEqual([schedule]);

  const admin = renderHook(() => adminHooks.useAdminSchedules(), { wrapper });
  const page = renderHook(() => scheduleHooks.useAdminAllSchedules(), { wrapper });
  await waitFor(() => expect(admin.result.current.isSuccess && page.result.current.isSuccess).toBe(true));
  expect(api.get).toHaveBeenCalledWith('/api/v1/admin/schedules');
  expect(client.getQueryData(['adminSchedules'])).toEqual([schedule]);
  expect(client.getQueryData(['schedules', 'admin-all'])).toEqual([schedule]);
});
