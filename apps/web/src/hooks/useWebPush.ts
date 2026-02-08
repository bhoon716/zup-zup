
import { useState, useCallback } from 'react';
import * as webPush from '@/lib/webpush';
import * as userApi from '@/lib/api/user';
import { toast } from 'sonner';

export const useWebPush = () => {
  const [loading, setLoading] = useState(false);

  const subscribe = useCallback(async () => {
    setLoading(true);
    try {
      // 1. Subscribe to Push Manager (Browser)
      const subscription = await webPush.subscribeToPush();
      
      if (!subscription) {
        throw new Error('푸시 구독 객체를 생성할 수 없습니다.');
      }

      // 2. Parse subscription details
      const json = subscription.toJSON();
      const token = json.endpoint;
      const p256dh = json.keys?.p256dh;
      const auth = json.keys?.auth;

      if (!token || !p256dh || !auth) {
        throw new Error('푸시 토큰 정보를 가져올 수 없습니다.');
      }

      // 3. Register device to Backend
      await userApi.registerDevice({
        type: 'WEB',
        token,
        p256dh,
        auth,
      });

      toast.success('웹 푸시 알림이 활성화되었습니다.');
      return true;
    } catch (error: any) {
      console.error('Web Push Subscription Failed:', error);
      toast.error(error.message || '웹 푸시 설정에 실패했습니다.');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const unsubscribe = useCallback(async () => {
    setLoading(true);
    try {
      // 1. Get current subscription to find token (for backend unregistration)
      const subscription = await webPush.getSubscription();
      
      if (subscription) {
        const token = subscription.endpoint;
        
        // 2. Unregister from Backend
        try {
          await userApi.unregisterDevice(token);
        } catch (e) {
          console.warn('Backend unregistration failed (might be already deleted)', e);
        }

        // 3. Unsubscribe from Browser
        await subscription.unsubscribe();
      }

      toast.success('웹 푸시 알림이 비활성화되었습니다.');
      return true;
    } catch (error: any) {
      console.error('Web Push Unsubscription Failed:', error);
      toast.error('웹 푸시 해제에 실패했습니다.');
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    subscribe,
    unsubscribe,
    loading,
  };
};
