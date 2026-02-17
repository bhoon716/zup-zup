import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useNotifications } from './useNotifications';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';

const mockNotifications = [
  {
    id: 1,
    channel: 'FCM',
    courseKey: 'TEST-101',
    title: 'Test Title',
    message: 'Test Message',
    sentAt: '2024-01-01T12:00:00',
  },
];

const handlers = [
  http.get('*/api/v1/notifications/history', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockNotifications,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe('useNotifications hook', () => {
  it('fetches notifications successfully', async () => {
    const { result } = renderHook(() => useNotifications(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data![0].courseKey).toBe('TEST-101');
  });
});
