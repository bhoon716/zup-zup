import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AccountSettings } from './account-settings';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import React from 'react';

const mockUser = {
  id: 1,
  email: 'test@example.com',
  name: 'Test User',
  role: 'USER',
};

const handlers = [
  http.get('*/api/v1/users/me', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Success',
      data: mockUser,
    });
  }),
  http.patch('*/api/v1/users/me', async ({ request }) => {
    const body = await request.json() as { name: string };
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Profile updated',
      data: { ...mockUser, name: body.name },
    });
  }),
  http.delete('*/api/v1/users/me', () => {
    return HttpResponse.json({
      code: 'SUCCESS',
      message: 'Withdrawn',
      data: null,
    });
  }),
];

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const createTestQueryClient = () => new QueryClient({
  defaultOptions: {
    queries: { retry: false },
  },
});

const renderWithClient = (ui: React.ReactElement) => {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
};

describe('AccountSettings Component', () => {
  it('renders user information correctly', async () => {
    renderWithClient(<AccountSettings />);
    
    expect(await screen.findByDisplayValue('test@example.com')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Test User')).toBeInTheDocument();
  });

  it('updates name when save button is clicked', async () => {
    renderWithClient(<AccountSettings />);
    
    const nameInput = await screen.findByDisplayValue('Test User');
    fireEvent.change(nameInput, { target: { value: 'New Name' } });
    
    const saveButton = screen.getByText('변경 사항 저장');
    fireEvent.click(saveButton);
    
    await waitFor(() => {
      expect(screen.getByDisplayValue('New Name')).toBeInTheDocument();
    });
  });

  it('shows confirmation dialog when delete button is clicked', async () => {
    renderWithClient(<AccountSettings />);
    
    const deleteButton = screen.getByText('회원 탈퇴');
    fireEvent.click(deleteButton);
    
    expect(screen.getByText('정말 탈퇴하시겠습니까?')).toBeInTheDocument();
  });
});
