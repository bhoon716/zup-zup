"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { ChevronLeft, Send, Loader2 } from "lucide-react";
import Link from "next/link";
import { useSendTestNotification } from "@/hooks/useAdminActions";
import { useAuthStore } from "@/store/useAuthStore";
import { useEffect } from "react";
import { getSubscription, subscribeToPush } from "@/lib/webpush";
import { useRegisterDevice } from "@/hooks/useUserDevices";
import { toast } from "sonner";

const CHANNELS = [
  { id: "EMAIL", label: "이메일" },
  { id: "WEB", label: "웹 푸시" },
  { id: "FCM", label: "앱 푸시(FCM)" },
];

export default function NotificationTestPage() {
  const user = useAuthStore((state) => state.user);
  const [email, setEmail] = useState("");
  const [selectedChannels, setSelectedChannels] = useState<string[]>(["EMAIL"]);
  const [isRegistering, setIsRegistering] = useState(false);
  
  const { mutate: sendTest, isPending } = useSendTestNotification();
  const { mutateAsync: registerDevice } = useRegisterDevice();

  useEffect(() => {
    if (user?.email) {
      setEmail(user.email);
    }
  }, [user]);

  const handleChannelToggle = async (channelId: string) => {
    const isAdding = !selectedChannels.includes(channelId);

    if (isAdding && channelId === "WEB") {
      setIsRegistering(true);
      try {
        let subscription = await getSubscription();
        
        if (!subscription) {
          subscription = await subscribeToPush();
          
          const p256dh = btoa(String.fromCharCode(...new Uint8Array(subscription.getKey("p256dh")!)));
          const auth = btoa(String.fromCharCode(...new Uint8Array(subscription.getKey("auth")!)));
          
          await registerDevice({
            type: "WEB",
            token: subscription.endpoint,
            p256dh: p256dh,
            auth: auth,
          });
          toast.success("웹 푸시 기기가 등록되었습니다.");
        }
        
        setSelectedChannels((prev) => [...prev, "WEB"]);
      } catch (error) {
        console.error("Web push registration failed:", error);
        toast.error("웹 푸시 권한 승인이 필요합니다.");
      } finally {
        setIsRegistering(false);
      }
      return;
    }

    setSelectedChannels((prev) =>
      isAdding
        ? [...prev, channelId]
        : prev.filter((id) => id !== channelId)
    );
  };

  const handleSend = () => {
    if (!email || selectedChannels.length === 0) return;
    sendTest({ email, channels: selectedChannels });
  };

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <Link href="/admin">
          <Button variant="ghost" size="icon" className="rounded-full">
            <ChevronLeft className="w-5 h-5" />
          </Button>
        </Link>
        <h1 className="text-xl font-bold">알림 전송 테스트</h1>
      </div>

      <Card className="shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base text-muted-foreground">테스트 설정</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Email Input */}
          <div className="space-y-2">
            <Label htmlFor="email" className="text-sm font-medium">수신자 (관리자 본인)</Label>
            <Input
              id="email"
              value={email}
              readOnly
              className="bg-gray-50 dark:bg-gray-900 border-dashed cursor-default focus-visible:ring-0"
            />
          </div>

          {/* Channel Checkboxes */}
          <div className="space-y-3">
            <Label className="text-sm font-medium">발송 채널</Label>
            <div className="flex flex-wrap gap-6 p-4 rounded-lg bg-gray-50 dark:bg-gray-900 border">
              {CHANNELS.map((channel) => (
                <div key={channel.id} className="flex items-center space-x-2">
                  <Checkbox
                    id={channel.id}
                    checked={selectedChannels.includes(channel.id)}
                    onCheckedChange={() => handleChannelToggle(channel.id)}
                    disabled={isRegistering || (isPending && channel.id !== "EMAIL")}
                    className="data-[state=checked]:bg-orange-600 data-[state=checked]:border-orange-600"
                  />
                  <Label
                    htmlFor={channel.id}
                    className="text-sm cursor-pointer select-none"
                  >
                    {channel.label}
                    {channel.id === "WEB" && isRegistering && (
                      <Loader2 className="inline w-3 h-3 ml-2 animate-spin text-orange-600" />
                    )}
                  </Label>
                </div>
              ))}
            </div>
          </div>

          {/* Submit Button */}
          <Button
            className="w-full h-11 font-bold bg-orange-600 hover:bg-orange-700"
            disabled={!email || selectedChannels.length === 0 || isPending || isRegistering}
            onClick={handleSend}
          >
            {isPending || isRegistering ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Send className="w-4 h-4 mr-2" />
            )}
            {isRegistering ? "기기 등록 중..." : "테스트 알림 전송"}
          </Button>
        </CardContent>
      </Card>

      <p className="text-xs text-center text-muted-foreground">
        가입된 사용자의 정확한 이메일을 입력해야 발송이 가능합니다.
      </p>
    </div>
  );
}
