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
import { Loader2, Mail, Bell, Globe, Smartphone, Save, History, Settings2, CheckCircle, Timer, ArrowLeft, MessageSquare, Trash2 } from "lucide-react";
import { getMyProfile, updateSettings, getDevices, deleteDevice } from "@/lib/api/user";
import * as userApi from "@/lib/api/user";
import { unlinkDiscord } from "@/lib/api/user";
import type { User, UserDeviceResponse } from "@/types/api";
import { AxiosError } from "axios";
import { motion, AnimatePresence } from "framer-motion";
import { useRouter, useSearchParams } from "next/navigation";
import { useWebPush } from "@/hooks/useWebPush";

const settingsSchema = z.object({
  notificationEmail: z.string().email("유효한 이메일 주소를 입력해 주세요.").or(z.literal("")),
  emailEnabled: z.boolean(),
  webPushEnabled: z.boolean(),
  fcmEnabled: z.boolean(),
  discordEnabled: z.boolean(),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof AxiosError) {
    const responseData = error.response?.data as { message?: string } | undefined;
    return responseData?.message || fallbackMessage;
  }
  return fallbackMessage;
};

export default function SettingsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");
  
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);

  const [isUnlinking, setIsUnlinking] = useState(false);

  const [devices, setDevices] = useState<UserDeviceResponse[]>([]);

  const { subscribe, unsubscribe, loading: loadingWebPush } = useWebPush();

  const DISCORD_CLIENT_ID = "1470147038564847719";

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    trigger,
    formState: { errors },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
      fcmEnabled: true,
      discordEnabled: false,
    },
  });

  const notificationEmail = watch("notificationEmail");

  useEffect(() => {
    if (discordStatus === "success") {
      toast.success("디스코드 연동이 성공적으로 완료되었습니다.");
      router.replace("/settings");
    } else if (discordStatus === "error") {
      toast.error("디스코드 연동 중 오류가 발생했습니다. 설정에서 주소를 다시 확인해주세요.");
      router.replace("/settings");
    }
  }, [discordStatus, router]);

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getMyProfile();
        const userData = response.data;
        setUser(userData);

        const initialEmail = userData.notificationEmail || "";

        reset({
          notificationEmail: initialEmail,
          emailEnabled: userData.emailEnabled,
          webPushEnabled: userData.webPushEnabled,
          fcmEnabled: userData.fcmEnabled,
          discordEnabled: userData.discordEnabled,
        });

        const deviceRes = await getDevices();
        setDevices(deviceRes.data);
      } catch (error) {
        console.error("Failed to fetch profile:", error);
        toast.error("프로필 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [reset]);

  useEffect(() => {
    if (user) {
      const currentInput = notificationEmail;
      const originalEmail = user.notificationEmail || "";
      const googleEmail = user.email;

      // 현재 입력값이 기존 알림 메일 또는 구글 메일이면 재인증 없이 저장 가능하다.
      if (currentInput === originalEmail || currentInput === googleEmail) {
        setVerified(true);
        setEmailSent(false);
      } else {
        setVerified(false);
        setEmailSent(false);
      }
    }
  }, [notificationEmail, user]);


  const onSendCode = async () => {
    const valid = await trigger("notificationEmail");
    if (!valid || !notificationEmail) return;

    setSending(true);
    try {
      await userApi.sendVerificationCode({ email: notificationEmail });
      setEmailSent(true);
      toast.success("인증 코드가 전송되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 코드 전송 실패"));
    } finally {
      setSending(false);
    }
  };

  const onVerifyCode = async () => {
    if (!authCode) return;
    setVerifying(true);
    try {
      await userApi.verifyEmail({ email: notificationEmail, code: authCode });
      setVerified(true);
      setEmailSent(false);
      toast.success("이메일이 인증되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 실패"));
    } finally {
      setVerifying(false);
    }
  };

  const handleDiscordConnect = () => {
    const DISCORD_REDIRECT_URI = encodeURIComponent(`${window.location.origin}/api/v1/users/discord/callback`);
    const DISCORD_OAUTH_URL = `https://discord.com/api/oauth2/authorize?client_id=${DISCORD_CLIENT_ID}&redirect_uri=${DISCORD_REDIRECT_URI}&response_type=code&scope=identify%20applications.commands&integration_type=1&state=settings`;
    window.location.href = DISCORD_OAUTH_URL;
  };

  const handleDiscordUnlink = async () => {
    if (!confirm("디스코드 연동을 해제하시겠습니까?")) return;
    
    setIsUnlinking(true);
    try {
      await unlinkDiscord();
      toast.success("디스코드 연동이 해제되었습니다.");
      const response = await getMyProfile();
      setUser(response.data);
      setValue("discordEnabled", false);
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "연동 해제 실패"));
    } finally {
      setIsUnlinking(false);
    }
  };


  const handleDeleteDevice = async (id: number) => {
    if (!confirm("이 기기를 삭제하시겠습니까? 더 이상 알림을 받을 수 없습니다.")) return;

    try {
      await deleteDevice(id);
      setDevices(prev => prev.filter(d => d.id !== id));
      toast.success("기기가 삭제되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "기기 삭제 실패"));
    }
  };


  const onSubmit = async (values: SettingsFormValues) => {
    if (user) {
      const isOriginal = values.notificationEmail === (user.notificationEmail || "");
      const isGoogle = values.notificationEmail === user.email || (!values.notificationEmail && !user.notificationEmail);

      if (!isOriginal && !isGoogle) {
        // 새 메일 주소를 활성화할 때는 인증 상태를 반드시 확인한다.
        if (values.emailEnabled && !verified) {
          toast.error("변경된 이메일 인증을 완료해주세요.");
          return;
        }
      }
    }
    
    setIsSubmitting(true);
    try {
      await updateSettings(values);
      toast.success("알림 설정이 저장되었습니다.");
      const response = await getMyProfile();
      setUser(response.data);
      reset({
        notificationEmail: response.data.notificationEmail || "",
        emailEnabled: response.data.emailEnabled,
        webPushEnabled: response.data.webPushEnabled,
        fcmEnabled: response.data.fcmEnabled,
        discordEnabled: response.data.discordEnabled,
      });
      setAuthCode("");
    } catch (error: unknown) {
      console.error("Failed to update settings:", error);
      toast.error(getErrorMessage(error, "설정 저장에 실패했습니다."));
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

  const isGoogleEmail = user?.email === notificationEmail;
  const isOriginal = user && notificationEmail === (user.notificationEmail || "");
  const needsVerification = !isOriginal && !isGoogleEmail && notificationEmail;

  return (
    <div className="container max-w-2xl py-8 px-4 md:px-6 space-y-6 md:space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex items-center gap-3 md:gap-4">
        <Button variant="ghost" size="icon" onClick={() => router.back()} className="rounded-full hover:bg-white/10 flex-shrink-0">
            <ArrowLeft className="h-5 w-5 md:h-6 md:w-6" />
        </Button>
        <div className="p-2 md:p-3 rounded-2xl bg-primary/10 border border-primary/20 flex-shrink-0">
          <Settings2 className="h-6 w-6 md:h-8 md:w-8 text-primary" />
        </div>
        <div>
          <h1 className="text-2xl md:text-3xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-primary to-purple-400">
            설정
          </h1>
          <p className="text-[11px] md:text-sm text-muted-foreground mt-0.5 md:mt-1">
            알림 수신 방식과 계정 설정을 관리합니다.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 md:space-y-6">
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl overflow-hidden group rounded-[1.5rem] md:rounded-[2rem]">
          <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader className="p-5 pb-2 md:p-6 md:pb-4">
            <CardTitle className="flex items-center gap-2 text-lg md:text-xl">
              <Bell className="h-5 w-5 text-primary" />
              알림 수신 설정
            </CardTitle>
            <CardDescription className="text-xs md:text-sm">
              여석 발생 시 알림을 받을 채널을 선택하세요.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5 pt-2 md:p-6 md:pt-4 space-y-4 md:space-y-6 relative">
            <div className="flex items-center justify-between p-3.5 md:p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-blue-400" />
                  <Label htmlFor="email-enabled" className="text-sm md:text-base font-semibold">이메일 알림</Label>
                </div>
                <p className="text-[11px] md:text-sm text-muted-foreground">
                  지정한 이메일 주소로 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="email-enabled"
                checked={watch("emailEnabled")}
                onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
                className="scale-90 md:scale-100"
              />
            </div>

            <div className="flex items-center justify-between p-3.5 md:p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Globe className="h-4 w-4 text-green-400" />
                  <Label htmlFor="web-enabled" className="text-sm md:text-base font-semibold">웹 푸시 (PC 브라우저)</Label>
                </div>
                <p className="text-[11px] md:text-sm text-muted-foreground">
                  브라우저가 켜져 있을 때 실시간 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="web-enabled"
                checked={watch("webPushEnabled")}
                onCheckedChange={async (checked) => {
                  setValue("webPushEnabled", checked, { shouldDirty: true });
                  if (checked) {
                    const success = await subscribe();
                    if (!success) {
                       setValue("webPushEnabled", false);
                    }
                  } else {
                    await unsubscribe();
                  }
                }}
                disabled={loadingWebPush}
                className="scale-90 md:scale-100"
              />
            </div>

            <div className="flex items-center justify-between p-3.5 md:p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <Smartphone className="h-4 w-4 text-purple-400" />
                  <Label htmlFor="fcm-enabled" className="text-sm md:text-base font-semibold">앱 푸시 (PWA/안드로이드)</Label>
                </div>
                <p className="text-[11px] md:text-sm text-muted-foreground">
                  모바일 기기에서 실시간 푸시 알림을 보냅니다.
                </p>
              </div>
              <Switch
                id="fcm-enabled"
                checked={watch("fcmEnabled")}
                onCheckedChange={(checked) => setValue("fcmEnabled", checked, { shouldDirty: true })}
                className="scale-90 md:scale-100"
              />
            </div>

            <div className="flex items-center justify-between p-3.5 md:p-4 rounded-xl bg-white/5 border border-white/10 hover:bg-white/10 transition-colors">
              <div className="space-y-0.5">
                <div className="flex items-center gap-2">
                  <MessageSquare className="h-4 w-4 text-indigo-400" />
                  <Label htmlFor="discord-enabled" className="text-sm md:text-base font-semibold">디스코드 DM 알림</Label>
                </div>
                <p className="text-[11px] md:text-sm text-muted-foreground">
                  지정한 디스코드 계정으로 직접 메시지를 보냅니다.
                </p>
              </div>
              <Switch
                id="discord-enabled"
                checked={watch("discordEnabled")}
                onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
                className="scale-90 md:scale-100"
              />
            </div>
          </CardContent>
        </Card>
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl group rounded-[1.5rem] md:rounded-[2rem]">
           <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader className="p-5 pb-2 md:p-6 md:pb-4">
            <CardTitle className="flex items-center gap-2 text-lg md:text-xl">
              <Mail className="h-5 w-5 text-primary" />
              알림 이메일 주소
            </CardTitle>
            <CardDescription className="text-xs md:text-sm">
              구글 계정 이메일 외에 다른 이메일로 알림을 받고 싶다면 입력하세요.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5 pt-2 md:p-6 md:pt-4 space-y-4 relative">
            <div className="space-y-2">
              <Label htmlFor="notificationEmail" className="text-xs md:text-sm font-semibold opacity-70">수신용 이메일 주소</Label>
              <div className="flex gap-2">
                  <Input
                    id="notificationEmail"
                    placeholder={user?.email || "example@email.com"}
                    {...register("notificationEmail")}
                    className="bg-white/5 border-white/10 focus:ring-primary h-11 md:h-12 text-sm md:text-lg rounded-xl"
                  />
                  
                  {needsVerification && !verified && (
                      <Button 
                        type="button"
                        variant="outline"
                        onClick={onSendCode}
                        disabled={sending || !!errors.notificationEmail || !notificationEmail}
                        className="h-11 md:h-12 px-4 md:px-6 rounded-xl text-xs md:text-sm font-bold bg-white/5"
                      >
                         {sending ? "전송 중" : "인증"}
                      </Button>
                  )}
              </div>
              
              {errors.notificationEmail && (
                <p className="text-xs md:text-sm text-destructive font-medium px-1">{errors.notificationEmail.message}</p>
              )}
              {!errors.notificationEmail && isGoogleEmail && (
                  <p className="text-[10px] md:text-xs text-green-500 flex items-center gap-1 px-1">
                      <CheckCircle className="w-3 h-3" /> 구글 계정 이메일 (자동 인증됨)
                  </p>
              )}
               {!errors.notificationEmail && verified && !isGoogleEmail && notificationEmail && (
                  <p className="text-[10px] md:text-xs text-green-500 flex items-center gap-1 px-1">
                      <CheckCircle className="w-3 h-3" /> 인증되었습니다
                  </p>
              )}

              <p className="text-[10px] md:text-xs text-muted-foreground flex items-center gap-1.5 mt-2 px-1">
                <History className="h-3 w-3 opacity-50" />
                입력하지 않으면 로그인한 구글 계정으로 발송됩니다.
              </p>

              <AnimatePresence>
                {needsVerification && !verified && emailSent && (
                    <motion.div 
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        className="space-y-2 pt-2"
                    >
                        <div className="flex gap-2">
                            <Input 
                                placeholder="인증 코드 6자리"
                                value={authCode}
                                onChange={(e) => setAuthCode(e.target.value)}
                                maxLength={6}
                                className="bg-primary/5 h-11 md:h-12 text-center text-lg font-black tracking-[0.3em] rounded-xl"
                            />
                            <Button type="button" onClick={onVerifyCode} disabled={verifying || authCode.length !== 6} className="h-11 md:h-12 px-6 rounded-xl font-bold">
                                {verifying ? <Loader2 className="w-4 h-4 animate-spin" /> : "확인"}
                            </Button>
                        </div>
                        <p className="text-[10px] md:text-xs text-primary font-bold flex items-center gap-1.5 px-1 animate-pulse">
                            <Timer className="w-3 h-3" /> 인증 코드가 발송되었습니다. (5분 내 입력)
                        </p>
                    </motion.div>
                )}
              </AnimatePresence>

            </div>
          </CardContent>
          <CardFooter className="p-5 pt-0 md:p-6 md:pt-0 relative">
            <Button 
                type="submit" 
                className="w-full h-12 text-base md:text-lg font-bold shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all duration-300 rounded-xl md:rounded-2xl"
                disabled={isSubmitting}
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
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl group rounded-[1.5rem] md:rounded-[2rem] overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader className="p-5 pb-2 md:p-6 md:pb-4">
            <CardTitle className="flex items-center gap-2 text-lg md:text-xl">
              <MessageSquare className="h-5 w-5 text-indigo-400" />
              디스코드 계정 연동
            </CardTitle>
            <CardDescription className="text-xs md:text-sm">
              디스코드 봇을 통해 계정을 연동하고 실시간 알림을 받아보세요.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5 pt-2 md:p-6 md:pt-4 space-y-4 relative">
            {user?.discordId ? (
              <div className="flex items-center justify-between p-4 rounded-2xl bg-indigo-500/10 border border-indigo-500/20">
                <div className="space-y-1">
                  <p className="text-sm font-medium text-indigo-200">연동된 계정 ID</p>
                  <p className="text-lg font-mono font-bold text-white">{user.discordId}</p>
                </div>
                <div className="p-2 rounded-full bg-indigo-500/20">
                  <CheckCircle className="h-6 w-6 text-indigo-400" />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-3">
                  <h4 className="text-sm font-bold flex items-center gap-2 text-indigo-300">
                    <CheckCircle className="h-4 w-4" /> 간편 연동 안내
                  </h4>
                  <p className="text-xs md:text-sm text-muted-foreground leading-relaxed">
                    아래 버튼을 눌러 디스코드 계정을 인증하면 즉시 연동됩니다.<br />
                    연동 시 <strong>&apos;자신의 계정에 설치&apos;</strong>를 선택해야 서버 없이도 알림을 받을 수 있습니다.
                  </p>
                  <div className="pt-2 p-3 rounded-lg bg-orange-500/10 border border-orange-500/20 space-y-2">
                    <p className="text-[11px] md:text-xs text-orange-200 font-bold flex items-center gap-1.5">
                      <Timer className="w-3 h-3" /> 알림 전송 실패(50007) 해결법
                    </p>
                    <ul className="text-[10px] md:text-[11px] text-orange-100/70 list-disc list-inside space-y-1">
                      <li>디스코드 설정 ➔ 개인정보 보호 ➔ <strong>&apos;내게 메시지를 보낼 수 있는 사람&apos;</strong> 설정 확인</li>
                      <li>봇을 차단했거나 개인정보 보호 수준이 너무 높으면 메시지가 전송되지 않습니다.</li>
                    </ul>
                  </div>
                </div>

                <Button
                  type="button"
                  onClick={handleDiscordConnect}
                  className="w-full h-12 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold shadow-lg shadow-indigo-500/20 transition-all duration-300"
                >
                  <MessageSquare className="h-5 w-5 mr-2" />
                  디스코드 계정 연결하기
                </Button>
              </div>
            )}

            {user?.discordId && (
              <div className="pt-2">
                <Button 
                  type="button" 
                  variant="ghost" 
                  onClick={handleDiscordUnlink}
                  disabled={isUnlinking}
                  className="w-full h-10 text-xs text-destructive hover:bg-destructive/10 rounded-xl"
                >
                  {isUnlinking ? <Loader2 className="h-3 w-3 animate-spin mr-2" /> : "연동 해제하기"}
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
        <Card className="border-none bg-white/5 backdrop-blur-xl shadow-2xl group rounded-[1.5rem] md:rounded-[2rem] overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-green-500/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <CardHeader className="p-5 pb-2 md:p-6 md:pb-4">
            <CardTitle className="flex items-center gap-2 text-lg md:text-xl">
              <Smartphone className="h-5 w-5 text-green-400" />
              등록된 기기 관리
            </CardTitle>
            <CardDescription className="text-xs md:text-sm">
              푸시 알림을 수신하는 기기 목록입니다.
            </CardDescription>
          </CardHeader>
          <CardContent className="p-5 pt-2 md:p-6 md:pt-4 space-y-4 relative">
             {devices.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground text-sm">
                  등록된 기기가 없습니다.
                </div>
             ) : (
                <div className="space-y-3">
                  {devices.map(device => (
                    <div key={device.id} className="flex items-center justify-between p-4 rounded-2xl bg-white/5 border border-white/10">
                      <div className="flex items-center gap-3">
                        <div className="p-2 rounded-full bg-white/10">
                          {device.type === 'WEB' ? <Globe className="h-4 w-4 text-blue-300"/> : <Smartphone className="h-4 w-4 text-purple-300"/>}
                        </div>
                        <div>
                          <p className="text-sm font-bold text-white">{device.alias || (device.type === 'WEB' ? 'Web Browser' : 'Mobile App')}</p>
                          <p className="text-[10px] md:text-xs text-muted-foreground">
                            {new Date(device.registeredAt).toLocaleDateString()} 등록
                          </p>
                        </div>
                      </div>
                      <Button 
                        type="button" 
                        variant="ghost" 
                        size="icon"
                        onClick={() => handleDeleteDevice(device.id)}
                        className="text-muted-foreground hover:text-destructive hover:bg-destructive/10 rounded-full h-8 w-8"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  ))}
                </div>
             )}
          </CardContent>
        </Card>
      </form>
    </div>
  );
}
