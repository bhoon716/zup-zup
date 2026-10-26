import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import api from '@/shared/api/client';
import * as hooks from './useSchedules';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';

vi.mock('@/shared/api/client', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn(), patch: vi.fn() },
}));

const request = { scheduleType: '수강신청', startDate: '2026-10-26', endDate: '2026-10-27', ddayEnabled: true };
const schedule = { id: 7, ...request, dDay: '진행 중', deletedAt: null };
const cacheKeys = [['schedules'], ['dashboard', 'snapshot']];

beforeEach(() => vi.resetAllMocks());

describe('일정 변경 계약', () => {
  it.each(['create', 'update'] as const)('대표 지정 값을 포함한 %s 결과를 반환하고 모든 표시 캐시를 갱신한다', async (operation) => {
    vi.mocked(api.post).mockResolvedValueOnce({ data: schedule });
    vi.mocked(api.put).mockResolvedValueOnce({ data: schedule });
    const client = createTestQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const { result } = renderHook(() => ({ create: hooks.useCreateSchedule(), update: hooks.useUpdateSchedule() }), {
      wrapper: createQueryWrapper(client),
    });

    await act(async () => {
      const output = operation === 'create'
        ? await result.current.create.mutateAsync(request)
        : await result.current.update.mutateAsync({ id: 7, request });
      expect(output).toEqual(schedule);
    });

    if (operation === 'create') expect(api.post).toHaveBeenCalledExactlyOnceWith('/api/v1/admin/schedules', request);
    else expect(api.put).toHaveBeenCalledExactlyOnceWith('/api/v1/admin/schedules/7', request);
    expect(invalidate.mock.calls).toEqual(cacheKeys.map(queryKey => [{ queryKey }]));
  });

  it('삭제 실패를 전파하고 성공한 뒤에만 일반·대표·관리자·대시보드 캐시를 갱신한다', async () => {
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
    expect(api.delete).toHaveBeenCalledWith('/api/v1/admin/schedules/7');
    expect(invalidate.mock.calls).toEqual(cacheKeys.map(queryKey => [{ queryKey }]));
  });

  it('복원은 기존 대표 지정이 포함된 서버 결과를 그대로 반환하고 표시 캐시를 갱신한다', async () => {
    vi.mocked(api.patch).mockResolvedValueOnce({ data: schedule });
    const client = createTestQueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    const { result } = renderHook(() => hooks.useRestoreSchedule(), { wrapper: createQueryWrapper(client) });
    await act(async () => {
      expect(await result.current.mutateAsync(7)).toEqual(schedule);
    });
    expect(api.patch).toHaveBeenCalledExactlyOnceWith('/api/v1/admin/schedules/7/restore');
    expect(invalidate.mock.calls).toEqual(cacheKeys.map(queryKey => [{ queryKey }]));
  });
});

it('일반 목록과 선택된 대표 목록을 분리하고 서버 순서·진행 상태·삭제된 관리자 항목을 보존한다', async () => {
  const general = { ...schedule, id: 8, ddayEnabled: false };
  const deleted = { ...schedule, id: 9, deletedAt: '2026-10-26T10:00:00' };
  vi.mocked(api.get).mockImplementation(async (url) => ({
    data: url === '/api/v1/schedules' ? [general, schedule]
      : url === '/api/v1/schedules/ddays' ? [schedule] : [schedule, deleted],
  }));
  const client = createTestQueryClient();
  const wrapper = createQueryWrapper(client);
  const publicHooks = renderHook(({ enabled }) => ({
    general: hooks.useUpcomingSchedules(enabled), ddays: hooks.useScheduleDdays(enabled),
  }), { wrapper, initialProps: { enabled: false } });
  expect(api.get).not.toHaveBeenCalled();
  publicHooks.rerender({ enabled: true });
  const admin = renderHook(() => hooks.useAdminSchedules(), { wrapper });
  await waitFor(() => expect(publicHooks.result.current.general.isSuccess && publicHooks.result.current.ddays.isSuccess && admin.result.current.isSuccess).toBe(true));
  expect(publicHooks.result.current.general.data).toEqual([general, schedule]);
  expect(publicHooks.result.current.ddays.data).toEqual([schedule]);
  expect(admin.result.current.data).toEqual([schedule, deleted]);
  expect(client.getQueryData(['schedules', 'ddays'])).toEqual([schedule]);
});

it('대표 조회의 401은 게스트의 빈 목록으로 처리하며 refresh를 생략한다', async () => {
  vi.mocked(api.get).mockRejectedValueOnce({ response: { status: 401 } });
  const { result } = renderHook(() => hooks.useScheduleDdays(), { wrapper: createQueryWrapper(createTestQueryClient()) });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
  expect(result.current.data).toEqual([]);
  expect(api.get).toHaveBeenCalledWith('/api/v1/schedules/ddays', { skipAuthRefresh: true });
});

it('대표 조회의 서버 장애를 빈 목록으로 숨기지 않는다', async () => {
  const failure = new Error('제공 불가');
  vi.mocked(api.get).mockRejectedValueOnce(failure);
  const { result } = renderHook(() => hooks.useScheduleDdays(), { wrapper: createQueryWrapper(createTestQueryClient()) });
  await waitFor(() => expect(result.current.isError).toBe(true));
  expect(result.current.error).toBe(failure);
});
