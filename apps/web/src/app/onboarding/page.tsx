"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter, useSearchParams } from "next/navigation";
import { useUser, useCompleteOnboarding } from "@/hooks/useUser";
import { useWebPush } from "@/hooks/useWebPush"; // Added import
import * as userApi from "@/lib/api/user";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bell, Mail, Smartphone, CheckCircle, Check, Timer, MessageSquare, Link as LinkIcon } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";
import Link from "next/link";

interface OnboardingForm {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  discordEnabled: boolean;
  deviceName: string;
}

export default function OnboardingPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const { data: user, isLoading } = useUser();
  const { mutate: completeOnboarding, isPending } = useCompleteOnboarding();
  const { subscribe, unsubscribe, loading: loadingWebPush } = useWebPush();

  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);
  const [timeLeft, setTimeLeft] = useState(180); // 3 minutes

  // Discord Config
  const DISCORD_CLIENT_ID = "1470147038564847719";

  const { register, handleSubmit, setValue, watch, formState: { errors }, trigger } = useForm<OnboardingForm>({
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
      discordEnabled: false,
    },
  });

  const notificationEmail = watch("notificationEmail");
  const emailEnabled = watch("emailEnabled");
  const webPushEnabled = watch("webPushEnabled");
  const discordEnabled = watch("discordEnabled");

  useEffect(() => {
    if (user) {
      if (user.onboardingCompleted) {
        // If coming back from discord auth, allow stay? 
        // No, checking onboardingCompleted is correct.
        // But if we just linked discord, maybe backend updated it? 
        // Backend completeOnboarding sets it to true. Linking discord does NOT.
        // So we are safe.
        router.replace("/");
      } else {
        if (!watch("notificationEmail") && !verified) {
           setValue("notificationEmail", user.email);
        }
        // Sync discordEnabled with user profile if newly linked
        if (user.discordId) {
            setValue("discordEnabled", true);
        }
      }
    }
  }, [user, router, setValue, watch, verified]);

  // Handle Discord Redirect Status
  useEffect(() => {
    if (discordStatus === "success") {
      toast.success("디스코드 연동이 성공적으로 완료되었습니다.");
      // Clean up URL
      router.replace("/onboarding");
      // user re-fetch is handled by useUser hook automatically swr
    } else if (discordStatus === "error") {
      toast.error("디스코드 연동 중 오류가 발생했습니다.");
      router.replace("/onboarding");
    }
  }, [discordStatus, router]);

  // Timer logic
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (emailSent && timeLeft > 0 && !verified) {
      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0) {
      setEmailSent(false);
      toast.error("인증 시간이 만료되었습니다. 다시 시도해주세요.");
    }
    return () => clearInterval(timer);
  }, [emailSent, timeLeft, verified]);

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };


  const onSendCode = async () => {
    const valid = await trigger("notificationEmail");
    if (!valid) return;

    setSending(true);
    try {
      await userApi.sendVerificationCode({ email: notificationEmail });
      setEmailSent(true);
      setTimeLeft(180); // Reset timer
      toast.success("인증 코드가 전송되었습니다.");
    } catch (e: any) {
      toast.error(e.response?.data?.message || "인증 코드 전송 실패");
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
      setEmailSent(false); // Hide code input
      toast.success("이메일이 인증되었습니다.");
    } catch (e: any) {
      toast.error(e.response?.data?.message || "인증 실패");
    } finally {
      setVerifying(false);
    }
  };

  const handleDiscordConnect = () => {
    const DISCORD_REDIRECT_URI = encodeURIComponent(`${window.location.origin}/api/v1/users/discord/callback`);
    const DISCORD_OAUTH_URL = `https://discord.com/api/oauth2/authorize?client_id=${DISCORD_CLIENT_ID}&redirect_uri=${DISCORD_REDIRECT_URI}&response_type=code&scope=identify%20applications.commands&integration_type=1&state=onboarding`;
    window.open(DISCORD_OAUTH_URL, "_blank", "noopener,noreferrer");
  };

  const deviceName = watch("deviceName");

  const handleRegisterDevice = async () => {
    if (!deviceName?.trim()) {
      toast.error("기기 이름을 입력해 주세요.");
      return;
    }
    await subscribe(deviceName);
  };

  const onSubmit = (data: OnboardingForm) => {
    
    if (user && data.notificationEmail === user.email) {
       // ...
    } else {
        if (data.emailEnabled && !verified) {
            toast.error("이메일 인증을 완료해주세요.");
            return;
        }
    }

    if (data.webPushEnabled && !data.deviceName?.trim()) {
      toast.error("알림을 받을 기기 이름을 입력해 주세요.");
      return;
    }

    if (!data.emailEnabled && !data.webPushEnabled && !data.discordEnabled) {
      if (!confirm("모든 알림을 끄시겠습니까? 빈자리가 생겨도 알림을 받을 수 없습니다.")) {
        return;
      }
    }
    
    completeOnboarding({
        ...data,
        discordEnabled: data.discordEnabled // Ensure this is passed
    }, {
      onSuccess: () => {
        router.replace("/");
      },
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (!user) {
    return null;
  }
  
  const isGoogleEmail = user.email === notificationEmail;

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#f8f9fa] p-4 font-sans">
      <motion.main 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-2xl bg-white border border-gray-200 rounded-2xl shadow-lg overflow-hidden relative"
      >
        <div className="px-8 pt-10 pb-6 border-b border-gray-100 bg-white z-20 relative">
          <h1 className="text-2xl font-bold text-gray-900">프로필 설정</h1>
        </div>

        <div className="p-8 min-h-[440px] flex flex-col relative bg-white">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-8 w-full">
            <div className="space-y-1">
              <h2 className="text-xl font-bold text-gray-900">알림 설정하기</h2>
              <p className="text-gray-500 text-sm">원하는 방식으로 알림을 받아보세요.</p>
            </div>

            <div className="space-y-6">
              {/* Discord Section */}
              <div className="bg-[#f3f4f6] rounded-xl overflow-hidden transition-all border border-transparent hover:border-indigo-100">
                <div className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600">
                      <MessageSquare className="w-5 h-5" />
                    </div>
                    <span className="font-bold text-gray-900">디스코드 DM 알림</span>
                  </div>
                  <Switch 
                    id="discordEnabled" 
                    checked={discordEnabled}
                    onCheckedChange={(checked) => setValue("discordEnabled", checked)}
                    disabled={!user.discordId}
                    className="data-[state=checked]:bg-[#56296E]"
                  />
                </div>
                {!user.discordId ? (
                  <div className="px-4 pb-4 pt-0">
                    <Button 
                      type="button"
                      onClick={handleDiscordConnect}
                      className="w-full py-6 bg-[#5865F2] hover:bg-[#4752C4] text-white font-bold shadow-md shadow-[#5865F2]/20 transition-all flex items-center justify-center gap-2 text-sm rounded-xl border-none"
                    >
                      <LinkIcon className="w-4 h-4" />
                      디스코드 계정 연동
                    </Button>
                  </div>
                ) : (
                  <div className="px-4 pb-4 pt-0 flex items-center gap-2 text-green-600 text-xs font-semibold px-2">
                    <CheckCircle className="w-3.5 h-3.5" /> 계정이 연동되었습니다.
                  </div>
                )}
              </div>

              {/* Email Section */}
              <div className="bg-[#f3f4f6] rounded-xl overflow-hidden transition-all border border-transparent hover:border-purple-100">
                <div className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-purple-50 flex items-center justify-center text-[#56296E]">
                      <Mail className="w-5 h-5" />
                    </div>
                    <span className="font-bold text-gray-900">이메일 알림</span>
                  </div>
                  <Switch 
                    id="emailEnabled" 
                    checked={emailEnabled}
                    onCheckedChange={(checked) => setValue("emailEnabled", checked)}
                    className="data-[state=checked]:bg-[#56296E]"
                  />
                </div>
                <div className="px-4 pb-4 pt-0 space-y-3">
                  <div className="flex gap-2">
                    <Input 
                      placeholder="example@email.com" 
                      type="email"
                      readOnly={verified}
                      {...register("notificationEmail", { required: true })}
                      className="flex-1 p-6 bg-white border border-gray-200 focus:border-[#56296E] focus:ring-1 focus:ring-[#56296E] rounded-xl outline-none transition-all text-gray-900 text-sm"
                    />
                    {!isGoogleEmail && !verified && (
                      <Button 
                        type="button"
                        onClick={onSendCode}
                        disabled={sending}
                        className="px-6 h-[50px] bg-white border border-gray-200 hover:bg-gray-50 text-gray-700 font-bold rounded-xl text-sm transition-all whitespace-nowrap"
                      >
                        {sending ? "전송 중" : "인증"}
                      </Button>
                    )}
                  </div>
                  
                  <AnimatePresence>
                    {!verified && emailSent && (
                      <motion.div 
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: "auto" }}
                        exit={{ opacity: 0, height: 0 }}
                        className="flex gap-2 items-center"
                      >
                        <div className="relative flex-1">
                          <Input 
                            placeholder="인증 코드 6자리 입력" 
                            type="text"
                            value={authCode}
                            onChange={(e) => setAuthCode(e.target.value)}
                            maxLength={6}
                            className="w-full p-6 bg-white border border-gray-200 focus:border-[#56296E] focus:ring-1 focus:ring-[#56296E] rounded-xl outline-none transition-all text-gray-900 text-sm"
                          />
                          <div className="absolute right-4 top-1/2 -translate-y-1/2 text-red-500 font-bold text-sm tabular-nums">
                            {formatTime(timeLeft)}
                          </div>
                        </div>
                        <Button 
                          type="button"
                          onClick={onVerifyCode}
                          disabled={verifying || authCode.length !== 6}
                          className="px-6 h-[50px] bg-[#56296E] hover:bg-[#452059] text-white font-bold rounded-xl text-sm transition-all whitespace-nowrap shadow-sm"
                        >
                          확인
                        </Button>
                      </motion.div>
                    )}
                  </AnimatePresence>
                  
                  {isGoogleEmail && (
                    <div className="space-y-1.5 px-1">
                      <div className="flex items-center gap-2 text-green-600 text-xs font-semibold">
                        <CheckCircle className="w-3.5 h-3.5" /> 구글 계정으로 자동 인증되었습니다.
                      </div>
                      <p className="text-gray-500 text-[11px] font-medium leading-relaxed">
                        다른 이메일로 알림을 받고 싶으시다면 주소 수정 후 인증을 진행해 주세요.
                      </p>
                    </div>
                  )}
                  {verified && !isGoogleEmail && (
                    <div className="flex items-center gap-2 text-green-600 text-xs font-semibold px-1">
                      <CheckCircle className="w-3.5 h-3.5" /> 이메일 인증이 완료되었습니다.
                    </div>
                  )}
                </div>
              </div>

              {/* Web Push Section */}
              <div className="bg-[#f3f4f6] rounded-xl overflow-hidden transition-all border border-transparent hover:border-blue-100">
                <div className="flex items-center justify-between p-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center text-blue-600">
                      <Smartphone className="w-5 h-5" />
                    </div>
                    <span className="font-bold text-gray-900">웹 푸시 (현재 기기)</span>
                  </div>
                  <Switch 
                    id="webPushEnabled" 
                    checked={webPushEnabled}
                    onCheckedChange={async (checked) => {
                      setValue("webPushEnabled", checked);
                      if (checked) {
                         const success = await subscribe();
                         if (!success) setValue("webPushEnabled", false);
                      } else {
                          await unsubscribe();
                      }
                    }}
                    disabled={loadingWebPush}
                    className="data-[state=checked]:bg-[#56296E]"
                  />
                </div>
                <div className="px-4 pb-4 pt-0 space-y-3">
                  <div className="space-y-1.5">
                    <Input 
                      placeholder="기기 이름 (예: 내 노트북, 스마트폰)" 
                      type="text"
                      {...register("deviceName", { required: webPushEnabled })}
                      className={`w-full p-6 bg-white border ${errors.deviceName ? 'border-red-500 focus:border-red-500 focus:ring-red-500' : 'border-gray-200 focus:border-[#56296E] focus:ring-[#56296E]'} rounded-xl outline-none transition-all text-gray-900 text-sm`}
                    />
                    {errors.deviceName && (
                      <p className="text-red-500 text-[11px] font-semibold px-1">
                        기기 이름을 입력해 주세요.
                      </p>
                    )}
                  </div>
                  <Button 
                    type="button"
                    onClick={handleRegisterDevice}
                    disabled={loadingWebPush}
                    className="w-full py-6 bg-white border-2 border-[#56296E] text-[#56296E] hover:bg-purple-50 font-bold rounded-xl text-sm transition-all flex items-center justify-center gap-2"
                  >
                    <Smartphone className="w-4 h-4" />
                    현재 기기 등록
                  </Button>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-end pt-8 border-t border-gray-100 mt-8">
              <Button 
                type="submit" 
                disabled={isPending}
                className="w-full sm:w-auto px-12 py-7 bg-[#56296E] hover:bg-[#452059] text-white font-black shadow-lg shadow-[#56296E]/30 transition-all flex items-center justify-center gap-2 text-lg rounded-xl hover:translate-y-[-2px] active:translate-y-0"
              >
                <span>{isPending ? "저장 중..." : "완료"}</span>
                <Check className="w-6 h-6" />
              </Button>
            </div>
            
            <p className="text-[11px] text-center text-gray-400 font-medium leading-relaxed">
              시작 시 서비스 <Link href="/terms" className="underline hover:text-gray-600">이용약관</Link> 및 <Link href="/privacy" className="underline hover:text-gray-600">개인정보 처리방침</Link>에 <br /> 동의하는 것으로 간주됩니다.
            </p>
          </form>
        </div>
      </motion.main>
    </div>
  );
}
