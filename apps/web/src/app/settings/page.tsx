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
import { Loader2, Mail, Bell, Globe, Smartphone, Save, History, Settings2, CheckCircle, Timer, ArrowLeft } from "lucide-react";
import { getMyProfile, updateSettings } from "@/lib/api/user";
import * as userApi from "@/lib/api/user";
import type { User } from "@/types/api";
import { motion, AnimatePresence } from "framer-motion";
import { useRouter } from "next/navigation";

const settingsSchema = z.object({
  notificationEmail: z.string().email("유효한 이메일 주소를 입력해 주세요.").or(z.literal("")),
  emailEnabled: z.boolean(),
  webPushEnabled: z.boolean(),
  fcmEnabled: z.boolean(),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

export default function SettingsPage() {
  const router = useRouter(); // Initialize router
  
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Email Verification States
  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    trigger, // Added trigger
    formState: { errors }, // removed isDirty constraint for now as verification state matters
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
      fcmEnabled: true,
    },
  });

  const notificationEmail = watch("notificationEmail");
  const emailEnabled = watch("emailEnabled");

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await getMyProfile();
        const userData = response.data;
        setUser(userData);
        
        // Initial values
        const initialEmail = userData.notificationEmail || "";
        
        reset({
          notificationEmail: initialEmail,
          emailEnabled: userData.emailEnabled,
          webPushEnabled: userData.webPushEnabled,
          fcmEnabled: userData.fcmEnabled,
        });
        
        // If current saved email exists and equals what we loaded, it is considered verified (as it was saved previously)
        // However, if user changes it, verified becomes false.
        // We track 'originalEmail' to compare.
      } catch (error) {
        console.error("Failed to fetch profile:", error);
        toast.error("프로필 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [reset]);

  // Reset verification if email changes
  useEffect(() => {
      if (user) {
          const currentInput = notificationEmail;
          const originalEmail = user.notificationEmail || "";
          const googleEmail = user.email;

          // If input matches currently saved email (which is already verified), set verified true
          if (currentInput === originalEmail) {
             // Already verified because it is saved in DB
             setVerified(true);
             setEmailSent(false);
          } else if (currentInput === googleEmail) {
              // Google email is implicitly verified? 
              // Based on backend logic: 
              // if (newEmail != user.getEmail() && newEmail != user.getNotificationEmail()) check verify
              // So if newEmail == user.getEmail(), backend skips verify check.
              // So we can treat it as verified/safe.
              setVerified(true); // Treat as verified for UI behavior
               setEmailSent(false);
          } else {
              // Changed to something else, reset verification
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


  const onSubmit = async (values: SettingsFormValues) => {
    // Validation: If notification email is changed (not same as saved, not same as google), it must be verified.
    if (user) {
        const isOriginal = values.notificationEmail === (user.notificationEmail || "");
        const isGoogle = values.notificationEmail === user.email || (!values.notificationEmail && !user.notificationEmail); 
        // Logic: if notificationEmail is empty string, it defaults to google email in backend logic usually? 
        // Backend: if request.notificationEmail is null/empty -> updateSettings logic uses it.
        // Wait, UserSettingsRequest has notificationEmail.
        // User.updateSettings receives email.
        // If empty string passed, it saves empty string?
        // Let's assume user wants to set a specific email.
        
        if (!isOriginal && !isGoogle) {
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
      // Refresh user data
      const response = await getMyProfile();
      setUser(response.data);
      // Reset form with new data
      reset({
        notificationEmail: response.data.notificationEmail || "",
        emailEnabled: response.data.emailEnabled,
        webPushEnabled: response.data.webPushEnabled,
        fcmEnabled: response.data.fcmEnabled,
      });
      setAuthCode("");
    } catch (error: any) {
      console.error("Failed to update settings:", error);
       // Handle 400 specifically if message provided
      toast.error(error.response?.data?.message || "설정 저장에 실패했습니다.");
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
  // Determine if verification is needed for UI state
  // Verification needed if: email is not empty AND email != saved AND email != google
  const isOriginal = user && notificationEmail === (user.notificationEmail || "");
  const needsVerification = !isOriginal && !isGoogleEmail && notificationEmail;

  return (
    <div className="container max-w-2xl py-10 space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => router.back()} className="rounded-full hover:bg-white/10">
            <ArrowLeft className="h-6 w-6" />
        </Button>
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
              <div className="flex gap-2">
                  <Input
                    id="notificationEmail"
                    placeholder={user?.email || "example@email.com"}
                    {...register("notificationEmail")}
                    className="bg-white/5 border-white/10 focus:ring-primary h-12 text-lg"
                    // If verification is needed and done, maybe mark green?
                    // Basic styling.
                  />
                  
                  {needsVerification && !verified && (
                      <Button 
                        type="button"
                        variant="outline"
                        onClick={onSendCode}
                        disabled={sending || !!errors.notificationEmail || !notificationEmail}
                        className="h-12 px-6"
                      >
                         {sending ? "전송 중" : "인증"}
                      </Button>
                  )}
              </div>
              
              {errors.notificationEmail && (
                <p className="text-sm text-destructive font-medium">{errors.notificationEmail.message}</p>
              )}

              {/* Status messages */}
              {!errors.notificationEmail && isGoogleEmail && (
                  <p className="text-xs text-green-500 flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" /> 구글 계정 이메일 (자동 인증됨)
                  </p>
              )}
               {/* Show verified badge if it IS verified AND it's a custom email (not google, not empty) */}
               {!errors.notificationEmail && verified && !isGoogleEmail && notificationEmail && (
                  <p className="text-xs text-green-500 flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" /> 인증되었습니다
                  </p>
              )}

              <p className="text-xs text-muted-foreground flex items-center gap-1.5 mt-2 px-1">
                <History className="h-3 w-3" />
                입력하지 않으면 로그인한 구글 계정({user?.email})으로 발송됩니다.
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
                                className="bg-white/5 h-12"
                            />
                            <Button type="button" onClick={onVerifyCode} disabled={verifying || authCode.length !== 6} className="h-12">
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
          </CardContent>
          <CardFooter className="relative">
            <Button 
                type="submit" 
                className="w-full h-12 text-lg font-bold shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all duration-300"
                disabled={isSubmitting} // Removing isDirty check to allow saving anytime if verified
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
