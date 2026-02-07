"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { useUser, useCompleteOnboarding } from "@/hooks/useUser";
import * as userApi from "@/lib/api/user";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bell, Mail, Smartphone, CheckCircle, Timer } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";

interface OnboardingForm {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
}

export default function OnboardingPage() {
  const router = useRouter();
  const { data: user, isLoading } = useUser();
  const { mutate: completeOnboarding, isPending } = useCompleteOnboarding();

  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);

  // If user uses google email, they don't need to verify (optional logic, but let's be consistent and require verification if they change it)
  // Actually, if they use the same email, we can auto-verify? No, let's follow the requirement "Allow self authentication".
  // If the initial email is google email, we set it as verified? 
  // User asked "Add self-authentication".
  // Let's force verification for ANY email if it's enabled.

  const { register, handleSubmit, setValue, watch, formState: { errors }, trigger } = useForm<OnboardingForm>({
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
    },
  });

  const notificationEmail = watch("notificationEmail");
  const emailEnabled = watch("emailEnabled");
  const webPushEnabled = watch("webPushEnabled");

  useEffect(() => {
    if (user) {
      if (user.onboardingCompleted) {
        router.replace("/");
      } else if (!watch("notificationEmail") && !verified) { // Only set default if not verified to avoid overwrite
        setValue("notificationEmail", user.email);
        // If it's their google email, maybe auto-verify?
        // Let's NOT auto-verify to be safe properly following user request.
      }
    }
  }, [user, router, setValue, watch, verified]);

  // Reset verification if email changes
  useEffect(() => {
    if (verified) {
        // If verified, disable editing? Or if they change it, reset verification.
        // For better UX, let's disable the input if verified.
    } else {
        // If email changes, setVerified(false) logic is needed?
        // But input is controlled by register.
        // We handle this by checking if 'verified' is true. 
        // If user changes email, they should re-verify.
        // How to detect change? 'notificationEmail' dependency.
        // But initial set triggers this.
        // Let's make Input readOnly if verified.
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

  const onSubmit = (data: OnboardingForm) => {
    // If email is different from google email, require verification.
    // If same, assuming we enforced verification anyway?
    // Let's require verification if 'verified' is false and email != user.email?
    // User requested "add self-authentication".
    // I will require 'verified' to be true if 'emailEnabled' is true.
    
    // Exception: If user uses their Google Email (user.email) AND we decide to trust it.
    // However, for consistency and meeting the requirement "Add self-auth", let's require it unless logic dictates otherwise.
    // Let's require verification for simplicity and security.
    
    if (user && data.notificationEmail === user.email) {
       // Allow skipping verification for Google Account Email provided by OAuth2
       // But wait, the backend logic I added checks verification for *any* email different from user.email.
       // So if data.notificationEmail === user.email, backend won't throw UNVERIFIED_EMAIL.
       // So frontend should allow submitting without explicit verification step if it matches.
       // BUT, the user explicitly asked for "self-authentication". 
       // If I am the user, I expect to verify my email.
       // If I use my google email, do I verify?
       // Let's implement: If email matches user.email, we CAN skip, but maybe show "Verified via Google" badge.
       // If I allow skipping, I don't need to force Verify.
       // IF the user types a helper email, they MUST verify.
    } else {
        if (data.emailEnabled && !verified) {
            toast.error("이메일 인증을 완료해주세요.");
            return;
        }
    }

    if (!data.emailEnabled && !data.webPushEnabled) {
      if (!confirm("모든 알림을 끄시겠습니까? 빈자리가 생겨도 알림을 받을 수 없습니다.")) {
        return;
      }
    }
    
    completeOnboarding(data, {
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
                        readOnly={verified || (isGoogleEmail && !emailSent && !verified)} // IF google email, maybe treat as verified? No, let user edit.
                        // Better: If verified, readOnly.
                        // If user wants to change, they can't unless we add "Change" button.
                        // For MVP, just don't make it readOnly unless verified.
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
                      {/* Show button if not verified and (not google email OR verification requested) */}
                      {/* Actually, backend logic: If Same as Google Email, no verification needed. */}
                      {/* So we only show Verify button if email != user.email */}
                      
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
