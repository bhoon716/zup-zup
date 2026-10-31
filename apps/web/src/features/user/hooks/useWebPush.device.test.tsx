import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useWebPush } from './useWebPush';
import { subscribeToPush } from '@/features/user/lib/webpush';

const { registerDevice } = vi.hoisted(() => ({ registerDevice: vi.fn() }));
vi.mock('@/features/user/api/user.api', () => ({ registerDevice }));
vi.mock('@/features/user/lib/webpush', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/features/user/lib/webpush')>(),
  subscribeToPush: vi.fn(),
}));
vi.mock('@/shared/telemetry/client-error', () => ({ reportClientError: vi.fn() }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const subscription = (endpoint = 'https://example.invalid/push'): PushSubscription => ({
  endpoint,
  expirationTime: null,
  options: { userVisibleOnly: true, applicationServerKey: null },
  getKey: () => null,
  unsubscribe: vi.fn().mockResolvedValue(true),
  toJSON: () => ({ endpoint, keys: { p256dh: 'fake', auth: 'fake' } }),
});

const macUA = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/18.0 Safari/605.1.15';
const iphoneUA = 'Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1';
const ipadUA = 'Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X) AppleWebKit/605.1.15 Version/18.0 Mobile/15E148 Safari/604.1';
const androidUA = 'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36';

describe('웹 푸시 기기 이름과 등록 주소', () => {
  beforeEach(() => { vi.clearAllMocks(); });
  afterEach(() => { vi.restoreAllMocks(); });
  it.each([
    [macUA, 'Safari on Mac'],
    [iphoneUA, 'Safari on iOS Device'],
    [ipadUA, 'Safari on iOS Device'],
    [androidUA, 'Chrome on Android Device'],
  ])('사용자 지정 이름이 없으면 UA에서 운영체제를 구분한다: %s', async (userAgent, alias) => {
    vi.spyOn(navigator, 'userAgent', 'get').mockReturnValue(userAgent);
    vi.mocked(subscribeToPush).mockResolvedValue(subscription());
    vi.mocked(registerDevice).mockResolvedValue(undefined);
    const { result } = renderHook(() => useWebPush());

    await act(async () => { expect(await result.current.subscribe()).toBe(true); });

    expect(registerDevice).toHaveBeenCalledWith({
      type: 'WEB', token: 'https://example.invalid/push', p256dh: 'fake', auth: 'fake', alias,
    });
  });

  it('같은 계정의 맥북과 아이폰은 각 브라우저의 구독 주소와 입력한 별칭으로 등록한다', async () => {
    vi.mocked(registerDevice).mockResolvedValue(undefined);
    const userAgent = vi.spyOn(navigator, 'userAgent', 'get');
    for (const [ua, alias, endpoint] of [
      [macUA, '내 맥북', 'https://example.invalid/mac/push'],
      [iphoneUA, '내 아이폰', 'https://example.invalid/iphone/push'],
    ]) {
      userAgent.mockReturnValue(ua);
      vi.mocked(subscribeToPush).mockResolvedValue(subscription(endpoint));
      const { result, unmount } = renderHook(() => useWebPush());
      await act(async () => { expect(await result.current.subscribe(alias)).toBe(true); });
      expect(registerDevice).toHaveBeenLastCalledWith({
        type: 'WEB', token: endpoint, p256dh: 'fake', auth: 'fake', alias,
      });
      unmount();
    }
    expect(registerDevice).toHaveBeenCalledTimes(2);
  });

});
