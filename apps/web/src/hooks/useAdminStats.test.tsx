import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useAdminStats } from './useAdminStats';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import React from 'react';

const mockStats = {
  totalUsers: 100,
  totalActiveSubscriptions: 50,
  todayNotificationCount: 20,
  crawlingStatus: 'RUNNING',
  lastCrawledAt: '2024-01-01T12:00:00',
};

const handlers = [
  http.get('*/api/v1/admin/stats', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockStats,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useAdminStats hook', () => {
  it('fetches admin stats successfully', async () => {
    const { result } = renderHook(() => useAdminStats(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.totalUsers).toBe(100);
    expect(result.current.data?.crawlingStatus).toBe('RUNNING');
  });
});
