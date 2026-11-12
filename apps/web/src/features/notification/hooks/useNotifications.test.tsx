import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { useNotifications } from './useNotifications';
import { createQueryWrapper, createTestQueryClient } from '@/test/query-client';
import { useAuthStore } from '@/features/auth/store/useAuthStore';

const mockNotifications = [
  {
    id: 1,
    channel: 'WEB',
    courseKey: 'TEST-101',
    title: 'Test Title',
    message: 'Test Message',
    sentAt: '2024-01-01T12:00:00',
  },
];

const handlers = [
  http.get('*/api/v1/notifications/history', () => {
    return HttpResponse.json({
        content: mockNotifications,
        number: 0,
        last: true,
      });
  }),
];

const server = setupServer(...handlers);
const user = {
  id: 1, email: 'test@example.invalid', name: '테스트 사용자', role: 'USER',
  emailEnabled: true, webPushEnabled: false, discordEnabled: false, onboardingCompleted: true,
};

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('useNotifications hook', () => {
  it('fetches notifications successfully', async () => {
    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);

    useAuthStore.setState({ isAuthenticated: true, user });

    const { result } = renderHook(() => useNotifications(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data![0].courseKey).toBe('TEST-101');
  });

  it('treats a notification page without content as empty', async () => {
    server.use(
      http.get('*/api/v1/notifications/history', () => {
        return HttpResponse.json({
            number: 0,
            last: true,
          });
      }),
    );

    const queryClient = createTestQueryClient();
    const wrapper = createQueryWrapper(queryClient);
    useAuthStore.setState({ isAuthenticated: true, user });

    const { result } = renderHook(() => useNotifications(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual([]);
  });

  it('계정이 바뀌면 이전 계정의 발송 이력을 표시하지 않는다', async () => {
    const queryClient = createTestQueryClient();
    queryClient.setDefaultOptions({ queries: { staleTime: Infinity, retry: false } });
    const wrapper = createQueryWrapper(queryClient);
    useAuthStore.setState({ isAuthenticated: true, user });
    const { result } = renderHook(() => useNotifications(), { wrapper });
    await waitFor(() => expect(result.current.data).toEqual(mockNotifications));

    server.use(http.get('*/api/v1/notifications/history', () => HttpResponse.json({ content: [], number: 0, last: true })));
    act(() => useAuthStore.setState({ user: { ...user, id: 2 } }));

    expect(result.current.data).toBeUndefined();
    await waitFor(() => expect(result.current.data).toEqual([]));
  });
});
