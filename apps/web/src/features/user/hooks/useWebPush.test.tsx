import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useWebPush } from './useWebPush';
import { subscribeToPush } from '@/features/user/lib/webpush';
import { registerDevice } from '@/features/user/api/user.api';
import { reportClientError } from '@/shared/telemetry/client-error';

vi.mock('@/features/user/lib/webpush', () => ({
  subscribeToPush: vi.fn(), extractSubscriptionKeys: () => ({ endpoint: 'https://example.invalid/push', p256dh: 'fake', auth: 'fake' }),
}));
vi.mock('@/features/user/api/user.api', () => ({ registerDevice: vi.fn() }));
vi.mock('@/shared/telemetry/client-error', () => ({ reportClientError: vi.fn() }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

describe('useWebPush', () => {
  beforeEach(() => { vi.clearAllMocks(); });
  it('권한 거부 이유와 대체 완료 방법을 안내하고 실패를 보고한다', async () => {
    const denied = new DOMException('Permission denied', 'NotAllowedError');
    vi.mocked(subscribeToPush).mockRejectedValue(denied);
    const { result } = renderHook(() => useWebPush());
    let subscribed;
    await act(async () => { subscribed = await result.current.subscribe('test device'); });
    expect(subscribed).toBe(false);
    expect(registerDevice).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith(expect.stringMatching(/권한.*이메일/));
    expect(reportClientError).toHaveBeenCalledWith(denied, { source: 'web-push', operation: 'subscribe' });
    expect(result.current.loading).toBe(false);
  });
  it('일반 등록 오류를 숨기거나 성공 처리하지 않고 재시도할 수 있다', async () => {
    const failure = new Error('기기 등록 요청에 실패했습니다.');
    vi.mocked(subscribeToPush).mockResolvedValue({} as PushSubscription);
    vi.mocked(registerDevice).mockRejectedValueOnce(failure).mockResolvedValueOnce(undefined);
    const { result } = renderHook(() => useWebPush());
    await act(async () => { expect(await result.current.subscribe('test device')).toBe(false); });
    expect(toast.error).toHaveBeenCalledWith(failure.message);
    expect(toast.success).not.toHaveBeenCalled();
    expect(reportClientError).toHaveBeenCalledWith(failure, { source: 'web-push', operation: 'subscribe' });
    await act(async () => { expect(await result.current.subscribe('test device')).toBe(true); });
    expect(registerDevice).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith('웹 푸시 알림이 활성화되었습니다.');
  });
});
