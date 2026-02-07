"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import { Loader2, Mail, Bell, Globe, Smartphone, Save, History, Settings2 } from "lucide-react";
import { getMyProfile, updateSettings } from "@/lib/api/user";
import type { User, UserSettingsRequest } from "@/types/api";

const settingsSchema = z.object({
  notificationEmail: z.string().email("유효한 이메일 주소를 입력해 주세요.").or(z.literal("")),
  emailEnabled: z.boolean(),
  webPushEnabled: z.boolean(),
  fcmEnabled: z.boolean(),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

export default function SettingsPage() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isDirty },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
      fcmEnabled: true,
    },
  });

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getMyProfile();
        setUser(response.data);
        reset({
          notificationEmail: response.data.notificationEmail || "",
          emailEnabled: response.data.emailEnabled,
          webPushEnabled: response.data.webPushEnabled,
          fcmEnabled: response.data.fcmEnabled,
        });
      } catch (error) {
        console.error("Failed to fetch profile:", error);
        toast.error("프로필 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [reset]);

  const onSubmit = async (values: SettingsFormValues) => {
    setIsSubmitting(true);
    try {
      await updateSettings(values);
      toast.success("알림 설정이 저장되었습니다.");
      // Refresh user data
      const response = await getMyProfile();
      setUser(response.data);
      reset({
        notificationEmail: response.data.notificationEmail || "",
        emailEnabled: response.data.emailEnabled,
        webPushEnabled: response.data.webPushEnabled,
        fcmEnabled: response.data.fcmEnabled,
      });
    } catch (error) {
      console.error("Failed to update settings:", error);
      toast.error("설정 저장에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex h-[60vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="container max-w-2xl py-10 space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex items-center gap-4">
        <div className="p-3 rounded-2xl bg-primary/10 border border-primary/20">
          <Settings2 className="h-8 w-8 text-primary" />
        </div>
        <div>
          <h1 className="text-3xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-primary to-purple-400">
            설정
          </h1>
          <p className="text-muted-foreground mt-1">
            알림 수신 방식과 계정 설정을 관리합니다.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {/* 알림 채널 설정 */}
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5 text-primary" />
              알림 수신 설정
            </CardTitle>
            <CardDescription>
              여석 발생 시 알림을 받을 채널을 선택하세요.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6 relative">
            <div className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-blue-400" />
                  <Label htmlFor="email-enabled" className="text-base font-semibold">이메일 알림</Label>
                </div>
                <p className="text-sm text-muted-foreground">
                  지정한 이메일 주소로 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="email-enabled"
                checked={watch("emailEnabled")}
                onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
              />
            </div>

            <div className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Globe className="h-4 w-4 text-green-400" />
                  <Label htmlFor="web-enabled" className="text-base font-semibold">웹 푸시 (PC 브라우저)</Label>
                </div>
                <p className="text-sm text-muted-foreground">
                  브라우저가 켜져 있을 때 실시간 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="web-enabled"
                checked={watch("webPushEnabled")}
                onCheckedChange={(checked) => setValue("webPushEnabled", checked, { shouldDirty: true })}
              />
            </div>

            <div className="flex items-center justify-between p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Smartphone className="h-4 w-4 text-purple-400" />
                  <Label htmlFor="fcm-enabled" className="text-base font-semibold">앱 푸시 (PWA/안드로이드)</Label>
                </div>
                <p className="text-sm text-muted-foreground">
                  모바일 기기에서 실시간 푸시 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="fcm-enabled"
                checked={watch("fcmEnabled")}
                onCheckedChange={(checked) => setValue("fcmEnabled", checked, { shouldDirty: true })}
              />
            </div>
          </CardContent>
        </Card>

        {/* 이메일 상세 설정 */}
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl group">
           <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5 text-primary" />
              알림 이메일 주소
            </CardTitle>
            <CardDescription>
              구글 계정 이메일 외에 다른 이메일로 알림을 받고 싶다면 입력하세요.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4 relative">
            <div className="space-y-2">
              <Label htmlFor="notificationEmail">수신용 이메일 주소</Label>
              <Input
                id="notificationEmail"
                placeholder={user?.email || "example@email.com"}
                {...register("notificationEmail")}
                className="bg-white/5 border-white/10 focus:ring-primary h-12 text-lg"
              />
              {errors.notificationEmail && (
                <p className="text-sm text-destructive font-medium">{errors.notificationEmail.message}</p>
              )}
              <p className="text-xs text-muted-foreground flex items-center gap-1.5 mt-2 px-1">
                <History className="h-3 w-3" />
                입력하지 않으면 로그인한 구글 계정({user?.email})으로 발송됩니다.
              </p>
            </div>
          </CardContent>
          <CardFooter className="relative">
            <Button 
                type="submit" 
                className="w-full h-12 text-lg font-bold shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all duration-300"
                disabled={!isDirty || isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                  저장 중...
                </>
              ) : (
                <>
                  <Save className="mr-2 h-5 w-5" />
                  설정 저장하기
                </>
              )}
            </Button>
          </CardFooter>
        </Card>
      </form>
    </div>
  );
}
