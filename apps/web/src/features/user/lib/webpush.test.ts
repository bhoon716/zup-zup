import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
vi.mock('@/shared/telemetry/client-error', () => ({ reportClientError: vi.fn(), buildClientErrorEvent: vi.fn() }));

describe('subscribeToPush', () => {
  beforeEach(() => { vi.resetModules(); vi.stubEnv('NEXT_PUBLIC_VAPID_KEY', 'AQID'); vi.useFakeTimers(); });
  afterEach(() => { vi.useRealTimers(); vi.unstubAllEnvs(); vi.unstubAllGlobals(); });
  it('미지원 환경은 외부 등록 없이 설치 안내와 대체 알림을 제시한다', async () => {
    vi.stubGlobal('navigator', {});
    const { subscribeToPush } = await import('./webpush');
    await expect(subscribeToPush()).rejects.toThrow(/iPhone\/iPad.*홈 화면.*이메일/);
  });
  it('지원 환경은 서비스 워커 준비 후 푸시를 구독한다', async () => {
    const subscription = {} as PushSubscription;
    const subscribe = vi.fn().mockResolvedValue(subscription);
    const registration = { pushManager: { getSubscription: vi.fn().mockResolvedValue(null), subscribe } };
    const register = vi.fn().mockResolvedValue(registration);
    vi.stubGlobal('navigator', { serviceWorker: { register, ready: Promise.resolve(registration) } });
    vi.stubGlobal('PushManager', class {});
    const { subscribeToPush } = await import('./webpush');
    await expect(subscribeToPush()).resolves.toBe(subscription);
    expect(register).toHaveBeenCalledWith('/sw.js');
    expect(subscribe).toHaveBeenCalledWith({ userVisibleOnly: true, applicationServerKey: new Uint8Array([1, 2, 3]) });
  });
});
