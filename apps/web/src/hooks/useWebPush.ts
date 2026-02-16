
import { useState, useCallback } from 'react';
import * as webPush from '@/lib/webpush';
import { extractSubscriptionKeys } from '@/lib/webpush';
import * as userApi from '@/lib/api/user';
import { toast } from 'sonner';

export const useWebPush = () => {
  const [loading, setLoading] = useState(false);

  const subscribe = useCallback(async (customAlias?: string) => {
    setLoading(true);
    try {
      const subscription = await webPush.subscribeToPush();
      
      if (!subscription) {
        throw new Error('푸시 구독 객체를 생성할 수 없습니다.');
      }

      const { endpoint: token, p256dh, auth } = extractSubscriptionKeys(subscription);

      const ua = navigator.userAgent;
      let alias = customAlias;
      
      if (!alias) {
          // 사용자 지정 별칭이 없으면 기기/브라우저 정보를 조합해 기본 이름을 만든다.
          alias = "Unknown Device";
          if (ua.includes("Win")) alias = "Windows PC";
          else if (ua.includes("Mac")) alias = "Mac";
          else if (ua.includes("Linux")) alias = "Linux PC";
          else if (ua.includes("Android")) alias = "Android Device";
          else if (ua.includes("iPhone") || ua.includes("iPad")) alias = "iOS Device";

          if (ua.includes("Chrome")) alias = `Chrome on ${alias}`;
          else if (ua.includes("Firefox")) alias = `Firefox on ${alias}`;
          else if (ua.includes("Safari") && !ua.includes("Chrome")) alias = `Safari on ${alias}`;
          else if (ua.includes("Edge")) alias = `Edge on ${alias}`;
      }

      await userApi.registerDevice({
        type: 'WEB',
        token,
        p256dh,
        auth,
        alias,
      });

      toast.success('웹 푸시 알림이 활성화되었습니다.');
      return true;
    } catch (error) {
      console.error('Web Push Subscription Failed:', error);
      const message = error instanceof Error ? error.message : '웹 푸시 설정에 실패했습니다.';
      toast.error(message);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const unsubscribe = useCallback(async () => {
    setLoading(true);
    try {
      const subscription = await webPush.getSubscription();
      
      if (subscription) {
        const token = subscription.endpoint;
        
        try {
          await userApi.unregisterDevice(token);
        } catch (e) {
          console.warn('Backend unregistration failed (might be already deleted)', e);
        }

        await subscription.unsubscribe();
      }

      toast.success('웹 푸시 알림이 비활성화되었습니다.');
      return true;
    } catch (error) {
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
