"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";
import { Bell, Shield, Smartphone } from "lucide-react";
import { useRegisterDevice, useUnregisterDevice } from "@/hooks/useUserDevices";
import { subscribeToPush, unsubscribeFromPush, getSubscription } from "@/lib/webpush";
import { toast } from "sonner";

export function NotificationSettings() {
  const [isPushEnabled, setIsPushEnabled] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  
  const { mutate: registerDevice } = useRegisterDevice();
  const { mutate: unregisterDevice } = useUnregisterDevice();

  useEffect(() => {
    const checkSubscription = async () => {
      try {
        const subscription = await getSubscription();
        setIsPushEnabled(!!subscription);
      } catch (error) {
        console.error("Failed to check push subscription:", error);
      } finally {
        setIsLoading(false);
      }
    };
    
    checkSubscription();
  }, []);

  const handlePushToggle = async (checked: boolean) => {
    setIsLoading(true);
    try {
      if (checked) {
        // Subscribe to push
        const subscription = await subscribeToPush();
        const p256dh = btoa(String.fromCharCode(...new Uint8Array(subscription.getKey("p256dh")!)));
        const auth = btoa(String.fromCharCode(...new Uint8Array(subscription.getKey("auth")!)));
        
        registerDevice({
          deviceType: "WEB",
          webPushEndpoint: subscription.endpoint,
          webPushP256dh: p256dh,
          webPushAuth: auth,
        });
        setIsPushEnabled(true);
      } else {
        // Unsubscribe from push
        const subscription = await getSubscription();
        if (subscription) {
          unregisterDevice(subscription.endpoint);
          await unsubscribeFromPush();
        }
        setIsPushEnabled(false);
      }
    } catch (error) {
      console.error("Push toggle failed:", error);
      toast.error("알림 설정 변경에 실패했습니다. 권한 설정을 확인해주세요.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-2xl mx-auto">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bell className="w-6 h-6 text-blue-600" />
          알림 설정
        </CardTitle>
        <CardDescription>
          강좌 빈자리 알림 수신 방법을 설정합니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex items-center justify-between space-x-2 border-b pb-4">
          <div className="space-y-0.5">
            <Label className="text-base flex items-center gap-2">
              <Smartphone className="w-4 h-4" />
              브라우저 푸시 알림
            </Label>
            <p className="text-sm text-muted-foreground">
              현재 브라우저에서 실시간으로 알림을 받습니다.
            </p>
          </div>
          <Switch
            checked={isPushEnabled}
            onCheckedChange={handlePushToggle}
            disabled={isLoading}
          />
        </div>

        <div className="flex items-start gap-3 p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
          <Shield className="w-5 h-5 text-blue-600 mt-0.5" />
          <div className="text-sm">
            <p className="font-semibold text-blue-900 dark:text-blue-300">알림 권한 안내</p>
            <p className="text-blue-800 dark:text-blue-400 mt-1">
              실시간 알림을 받기 위해서는 브라우저의 알림 권한 허용이 필요합니다. 
              설정 후 알림이 오지 않는다면 브라우저 주소창 왼쪽의 자물쇠 아이콘을 클릭하여 권한을 확인해주세요.
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
