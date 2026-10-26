
import { useState, useCallback } from 'react';
import * as webPush from '@/features/user/lib/webpush';
import { extractSubscriptionKeys } from '@/features/user/lib/webpush';
import * as userApi from '@/features/user/api/user.api';
import { toast } from 'sonner';
import { buildClientErrorEvent, reportClientError } from '@/shared/telemetry/client-error';

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
      reportClientError(error, { source: "web-push", operation: "subscribe" });
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
          console.warn('서버의 웹 푸시 기기 해제 실패. 브라우저 구독 해제는 계속 진행.',
            buildClientErrorEvent(e, { source: "web-push", operation: "unregister-device" }));
        }

        await subscription.unsubscribe();
      }

      toast.success('웹 푸시 알림이 비활성화되었습니다.');
      return true;
    } catch (error) {
      reportClientError(error, { source: "web-push", operation: "unsubscribe" });
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
