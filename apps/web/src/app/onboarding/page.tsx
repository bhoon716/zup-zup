"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter, useSearchParams } from "next/navigation";
import { useUser, useCompleteOnboarding } from "@/hooks/useUser";
import * as userApi from "@/lib/api/user";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bell, Mail, Smartphone, CheckCircle, Timer, MessageSquare } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";

interface OnboardingForm {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  discordEnabled: boolean;
}

export default function OnboardingPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const { data: user, isLoading } = useUser();
  const { mutate: completeOnboarding, isPending } = useCompleteOnboarding();

  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);

  // Discord Config
  const DISCORD_CLIENT_ID = "1470147038564847719"; 
  const DISCORD_REDIRECT_URI = encodeURIComponent("http://localhost:8080/api/v1/users/discord/callback");
  // Add state=onboarding to redirect back here
  const DISCORD_OAUTH_URL = `https://discord.com/api/oauth2/authorize?client_id=${DISCORD_CLIENT_ID}&redirect_uri=${DISCORD_REDIRECT_URI}&response_type=code&scope=identify%20applications.commands&integration_type=1&state=onboarding`;

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

  // Reset verification if email changes
  useEffect(() => {
    if (verified) {
        // ...
    } else {
        // ...
    }
  }, [notificationEmail]);


  const onSendCode = async () => {
    const valid = await trigger("notificationEmail");
    if (!valid) return;

    setSending(true);
    try {
      await userApi.sendVerificationCode({ email: notificationEmail });
      setEmailSent(true);
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
    // Save current form state? 
    // Since we redirect, state might be lost.
    // Ideally we save to sessionStorage. 
    // For now, let's assume user fills email first, then connects discord.
    // When they come back, email input might be lost.
    // We can auto-fill google email again.
    window.location.href = DISCORD_OAUTH_URL;
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
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md"
      >
        <Card className="border-border/50 bg-card/50 backdrop-blur-xl shadow-xl">
          <CardHeader className="text-center space-y-2">
            <div className="mx-auto w-12 h-12 bg-primary/10 rounded-2xl flex items-center justify-center mb-2">
              <Bell className="w-6 h-6 text-primary" />
            </div>
            <CardTitle className="text-2xl font-bold">환영합니다! 👋</CardTitle>
            <CardDescription className="text-base">
              빈자리 알림을 받기 위한 설정을 완료해주세요.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="notificationEmail">알림 받을 이메일</Label>
                  <div className="flex gap-2">
                      <Input 
                        id="notificationEmail" 
                        type="email" 
                        placeholder="example@jbnu.ac.kr"
                        readOnly={verified || (isGoogleEmail && !emailSent && !verified)} 
                        {...register("notificationEmail", { 
                          required: "이메일은 필수입니다.",
                          pattern: {
                            value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                            message: "올바른 이메일 형식이 아닙니다."
                          },
                          onChange: () => {
                              if (verified) setVerified(false);
                              if (emailSent) setEmailSent(false);
                          }
                        })}
                        className={verified ? "bg-muted text-muted-foreground" : ""}
                      />
                      
                      {!isGoogleEmail && !verified && (
                          <Button 
                            type="button" 
                            variant="outline" 
                            onClick={onSendCode} 
                            disabled={sending || !!errors.notificationEmail || !notificationEmail}
                          >
                            {sending ? "전송 중" : "인증"}
                          </Button>
                      )}
                  </div>
                  
                  {errors.notificationEmail && (
                    <p className="text-xs text-destructive">{errors.notificationEmail.message}</p>
                  )}
                  
                  {!errors.notificationEmail && isGoogleEmail && (
                      <p className="text-xs text-green-600 flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" /> 구글 계정 이메일(자동 인증됨)
                      </p>
                  )}
                   {!errors.notificationEmail && verified && (
                      <p className="text-xs text-green-600 flex items-center gap-1">
                          <CheckCircle className="w-3 h-3" /> 인증되었습니다
                      </p>
                  )}
                  
                  <p className="text-xs text-muted-foreground">
                    * 주로 사용하는 이메일을 입력해주세요.
                  </p>

                  <AnimatePresence>
                    {!verified && emailSent && (
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
                                />
                                <Button type="button" onClick={onVerifyCode} disabled={verifying || authCode.length !== 6}>
                                    확인
                                </Button>
                            </div>
                            <p className="text-xs text-muted-foreground flex items-center gap-1">
                                <Timer className="w-3 h-3" /> 인증 코드가 발송되었습니다. (5분 내 입력)
                            </p>
                        </motion.div>
                    )}
                  </AnimatePresence>
                </div>

                <div className="space-y-4 pt-4 border-t border-border/50">
                  <Label>알림 수신 채널</Label>
                  
                  
                  <div className="flex items-center justify-between p-3 rounded-xl border border-border/50 bg-background/50 hover:bg-accent/50 transition-colors">
                    <div className="flex items-center gap-3">
                      <div className={`p-2 rounded-lg ${emailEnabled ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'}`}>
                        <Mail className="w-4 h-4" />
                      </div>
                      <div className="space-y-0.5">
                        <Label htmlFor="emailEnabled" className="text-sm font-medium cursor-pointer">이메일 알림</Label>
                        <p className="text-xs text-muted-foreground">메일로 알림을 받습니다.</p>
                      </div>
                    </div>
                    <Switch 
                      id="emailEnabled" 
                      checked={emailEnabled}
                      onCheckedChange={(checked) => setValue("emailEnabled", checked)}
                    />
                  </div>

                  <div className="flex items-center justify-between p-3 rounded-xl border border-border/50 bg-background/50 hover:bg-accent/50 transition-colors">
                    <div className="flex items-center gap-3">
                      <div className={`p-2 rounded-lg ${webPushEnabled ? 'bg-primary/10 text-primary' : 'bg-muted text-muted-foreground'}`}>
                        <Smartphone className="w-4 h-4" />
                      </div>
                      <div className="space-y-0.5">
                        <Label htmlFor="webPushEnabled" className="text-sm font-medium cursor-pointer">웹 푸시 알림</Label>
                        <p className="text-xs text-muted-foreground">브라우저/모바일 푸시를 받습니다.</p>
                      </div>
                    </div>
                    <Switch 
                      id="webPushEnabled" 
                      checked={webPushEnabled}
                      onCheckedChange={(checked) => setValue("webPushEnabled", checked)}
                    />
                  </div>

                  <div className="flex items-center justify-between p-3 rounded-xl border border-border/50 bg-background/50 hover:bg-accent/50 transition-colors">
                    <div className="flex items-center gap-3">
                      <div className={`p-2 rounded-lg ${discordEnabled ? 'bg-indigo-500/10 text-indigo-500' : 'bg-muted text-muted-foreground'}`}>
                        <MessageSquare className="w-4 h-4" />
                      </div>
                      <div className="space-y-0.5">
                        <Label htmlFor="discordEnabled" className="text-sm font-medium cursor-pointer">디스코드 알림</Label>
                        <p className="text-xs text-muted-foreground">디스코드 DM으로 알림을 받습니다.</p>
                      </div>
                    </div>
                    <Switch 
                      id="discordEnabled" 
                      checked={discordEnabled}
                      onCheckedChange={(checked) => setValue("discordEnabled", checked)}
                      disabled={!user.discordId} // Disable if not linked
                    />
                  </div>
                  
                  {/* Discord Connect UI */}
                  {!user.discordId && (
                      <div className="p-3 bg-indigo-50 dark:bg-indigo-950/20 rounded-xl border border-indigo-100 dark:border-indigo-900/30">
                          <div className="flex items-center justify-between">
                              <div className="space-y-1">
                                  <p className="text-xs font-semibold text-indigo-700 dark:text-indigo-300">디스코드 미연동</p>
                                  <p className="text-[10px] text-muted-foreground">연동하면 DM 알림을 켤 수 있습니다.</p>
                              </div>
                              <Button 
                                type="button" 
                                size="sm" 
                                variant="outline"
                                onClick={handleDiscordConnect}
                                className="h-8 text-xs bg-indigo-600 text-white hover:bg-indigo-700 border-none"
                              >
                                연동하기
                              </Button>
                          </div>
                      </div>
                  )}

                  {user.discordId && (
                      <div className="p-3 bg-green-50 dark:bg-green-950/20 rounded-xl border border-green-100 dark:border-green-900/30">
                           <div className="flex items-center gap-2">
                               <CheckCircle className="w-4 h-4 text-green-600" />
                               <p className="text-xs font-medium text-green-700 dark:text-green-300">
                                   디스코드 계정이 연동되었습니다.
                               </p>
                           </div>
                      </div>
                  )}

                </div>
              </div>

              <Button type="submit" className="w-full h-11 text-base font-medium rounded-xl" disabled={isPending}>
                {isPending ? "저장 중..." : "설정 완료하고 시작하기"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}
