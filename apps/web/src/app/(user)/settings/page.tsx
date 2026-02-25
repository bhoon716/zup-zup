"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Label } from "@/shared/ui/label";
import { Input } from "@/shared/ui/input";
import { Button } from "@/shared/ui/button";
import { toast } from "sonner";
import { Badge } from "@/shared/ui/badge";
import { 
  Loader2, Mail, Bell, Smartphone, Save,
  CheckCircle, MessageSquare, Laptop,
  X, AlertCircle, Monitor
} from "lucide-react";
import { getMyProfile, updateSettings, getDevices, deleteDevice } from "@/features/user/api/user.api";
import * as userApi from "@/features/user/api/user.api";
import { unlinkDiscord } from "@/features/user/api/user.api";
import type { User, UserDeviceResponse } from "@/shared/types/api";
import { AxiosError } from "axios";
import { motion, AnimatePresence } from "framer-motion";
import { useRouter, useSearchParams } from "next/navigation";
import { useWebPush } from "@/features/user/hooks/useWebPush";
import { cn } from "@/shared/lib/utils";

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

/**
 * 알림 설정 페이지 컴포넌트입니다.
 * 실시간 알림 채널(Discord, 이메일, 웹 푸시)을 연동하고 관리합니다.
 */
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
  const [deviceAlias, setDeviceAlias] = useState("");
  const [devices, setDevices] = useState<UserDeviceResponse[]>([]);

  const { subscribe, loading: loadingWebPush } = useWebPush();

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

      // 현재 입력값이 기존 알림 메일 또는 구글 메일이면 재인증 없이 저장 가능
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

  const handleRegisterDevice = async () => {
    if (!deviceAlias.trim()) {
      toast.error("기기 별칭을 입력해 주세요.");
      return;
    }

    try {
      const success = await subscribe(deviceAlias);
      if (success) {
        const deviceRes = await getDevices();
        setDevices(deviceRes.data);
        setDeviceAlias("");
        toast.success("현재 기기가 등록되었습니다.");
      }
    } catch {
      toast.error("기기 등록 중 오류가 발생했습니다.");
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
  }

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
    <div className="min-h-screen bg-white">
      <main className="max-w-4xl mx-auto px-6 sm:px-10 py-10 md:py-16 pb-32">
        <div className="mb-12 border-b border-slate-100 pb-8">
          <h1 className="text-3xl font-bold mb-2 text-slate-900 tracking-tight">알림 설정</h1>
          <p className="text-slate-500 font-medium">빈 좌석 알림을 받을 채널을 설정하고 관리하세요.</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)}>
          <section className="space-y-12">
            <div>
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
                      <Bell className="text-primary w-5 h-5" />
                    </div>
                    알림 채널 연동
                  </h2>
                  <p className="text-sm text-slate-500 mt-2 ml-11.5 font-medium">여러 채널을 연동하여 확실하게 알림을 받아보세요.</p>
                </div>
              </div>

              <div className="space-y-6">
                {/* Discord Section */}
                <div className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30">
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-[#5865F2] rounded-2xl items-center justify-center shadow-lg shadow-[#5865F2]/20 shrink-0 text-white">
                      <MessageSquare className="w-8 h-8" />
                    </div>
                    <div className="flex-1 w-full">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="font-bold text-lg text-slate-900">Discord 연동</h3>
                        {user?.discordId && (
                           <Badge variant="outline" className="bg-indigo-50 text-indigo-600 border-indigo-100 font-bold px-2 py-0.5 text-[10px] rounded-lg">
                             연동됨
                           </Badge>
                        )}
                      </div>
                      <p className="text-sm text-slate-500 mb-5 font-medium">디스코드 봇이 개인 DM으로 알림을 즉시 보내드립니다.</p>
                      
                      {user?.discordId ? (
                        <div className="flex flex-col sm:flex-row gap-3">
                          <div className="flex-1 bg-white border border-slate-200 rounded-xl px-4 py-3 flex items-center justify-between">
                            <span className="text-sm font-mono font-bold text-slate-700">{user.discordId}</span>
                            <CheckCircle className="w-4 h-4 text-emerald-500" />
                          </div>
                          <Button 
                            type="button" 
                            variant="destructive"
                            onClick={handleDiscordUnlink}
                            disabled={isUnlinking}
                            className="rounded-xl px-6 h-12 font-bold shadow-soft"
                          >
                            {isUnlinking ? <Loader2 className="w-4 h-4 animate-spin" /> : "연동 해제"}
                          </Button>
                        </div>
                      ) : (
                        <Button 
                          type="button"
                          onClick={handleDiscordConnect}
                          className="w-full bg-[#5865F2] hover:bg-[#4752C4] text-white h-13 rounded-xl font-bold flex items-center justify-center gap-3 transition-all shadow-lg shadow-[#5865F2]/20 active:scale-[0.98]"
                        >
                          <MessageSquare className="w-6 h-6" />
                          Discord 계정 연결하기
                        </Button>
                      )}

                      <div className="mt-4 flex items-center justify-between border-t border-slate-200/50 pt-4">
                        <Label htmlFor="discord-enabled" className="text-sm font-bold text-slate-600">DM 알림 활성화</Label>
                        <Switch
                          checked={watch("discordEnabled")}
                          onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* Email Section */}
                <div className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30">
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-white rounded-2xl items-center justify-center shadow-sm border border-slate-100 shrink-0">
                      <Mail className="w-7 h-7 text-slate-400" />
                    </div>
                    <div className="flex-1 w-full">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="font-bold text-lg text-slate-900">이메일 알림</h3>
                        {(isGoogleEmail || verified) && (
                          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">인증됨</span>
                        )}
                      </div>
                      <p className="text-sm text-slate-500 mb-5 font-medium">중요 알림을 이메일로 받아보세요. 학교 이메일을 권장합니다.</p>
                      
                      <div className="space-y-3">
                        <div className="relative flex items-center gap-2">
                          <Input 
                            {...register("notificationEmail")}
                            placeholder={user?.email}
                            readOnly={isOriginal || isGoogleEmail}
                            className={cn(
                              "w-full bg-white border-slate-200 rounded-xl px-4 py-6 text-sm focus:ring-2 focus:ring-primary h-12",
                              (isOriginal || isGoogleEmail) && "bg-slate-50 text-slate-400"
                            )}
                          />
                          {needsVerification && !verified && (
                            <Button 
                              type="button"
                              variant="outline"
                              onClick={onSendCode}
                              disabled={sending || !!errors.notificationEmail || !notificationEmail}
                              className="shrink-0 h-12 rounded-xl px-6 bg-white border-slate-200 font-bold text-slate-600 hover:bg-slate-50"
                            >
                              {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : "인증"}
                            </Button>
                          )}
                          {(isGoogleEmail || (verified && !isOriginal)) && (
                            <div className="absolute right-3 bg-slate-100 text-slate-400 px-3 py-1.5 rounded-lg text-[11px] font-bold">
                              인증완료
                            </div>
                          )}
                        </div>

                        <AnimatePresence>
                          {needsVerification && !verified && emailSent && (
                            <motion.div 
                              initial={{ opacity: 0, scale: 0.95 }}
                              animate={{ opacity: 1, scale: 1 }}
                              className="bg-primary/5 p-4 rounded-[1.5rem] border border-primary/10 flex flex-col sm:flex-row gap-3"
                            >
                              <Input 
                                placeholder="인증 코드 6자리"
                                value={authCode}
                                onChange={(e) => setAuthCode(e.target.value)}
                                maxLength={6}
                                className="flex-1 h-12 text-center text-lg font-black tracking-[0.3em] rounded-xl border-primary/20 bg-white"
                              />
                              <Button 
                                type="button" 
                                onClick={onVerifyCode} 
                                disabled={verifying || authCode.length !== 6} 
                                className="h-12 px-8 rounded-xl font-bold bg-primary"
                              >
                                {verifying ? <Loader2 className="w-4 h-4 animate-spin" /> : "확인"}
                              </Button>
                            </motion.div>
                          )}
                        </AnimatePresence>

                        <div className="flex items-center justify-between border-t border-slate-200/50 pt-4">
                          <Label htmlFor="email-enabled" className="text-sm font-bold text-slate-600">이메일 알림 활성화</Label>
                          <Switch
                            checked={watch("emailEnabled")}
                            onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
                          />
                        </div>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-3 font-medium flex items-center gap-1.5">
                        <AlertCircle className="w-3.5 h-3.5" />
                        이메일 변경을 원하시면 관리자에게 문의하세요.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Web Push Section */}
                <div className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30">
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-white rounded-2xl items-center justify-center shadow-sm border border-slate-100 shrink-0">
                      <Laptop className="w-7 h-7 text-primary" />
                    </div>
                    <div className="flex-1 w-full">
                      <h3 className="font-bold text-lg text-slate-900 mb-1">웹 푸시 알림</h3>
                      <p className="text-sm text-slate-500 mb-5 font-medium">현재 사용 중인 기기를 등록하여 브라우저 알림을 받습니다.</p>
                      
                      <div className="bg-white rounded-2xl border border-slate-100 p-4 mb-6">
                        <div className="flex flex-col sm:flex-row gap-3">
                          <Input 
                            value={deviceAlias}
                            onChange={(e) => setDeviceAlias(e.target.value)}
                            placeholder="기기 별칭 (예: 맥북 프로)"
                            className="flex-1 bg-slate-50 border-transparent rounded-xl px-4 h-12 focus:bg-white transition-all shadow-none"
                          />
                          <Button 
                            type="button"
                            onClick={handleRegisterDevice}
                            className="bg-primary hover:bg-primary-hover text-white px-6 h-12 rounded-xl text-sm font-bold shadow-lg shadow-primary/20 transition-all active:scale-[0.95]"
                          >
                            현재 기기 등록
                          </Button>
                        </div>
                      </div>

                      <div className="space-y-3">
                        <h4 className="text-xs font-bold text-slate-400 px-1 uppercase tracking-wider mb-2">등록된 기기 목록</h4>
                        {devices.length === 0 ? (
                           <div className="text-center py-6 bg-white/50 rounded-2xl border border-dashed border-slate-200">
                             <p className="text-sm text-slate-400 font-medium">등록된 기기가 없습니다.</p>
                           </div>
                        ) : (
                          devices.map((device, idx) => (
                            <div key={device.id} className="flex items-center justify-between bg-white border border-slate-100 px-4 py-3.5 rounded-xl shadow-sm hover:shadow-md transition-shadow">
                              <div className="flex items-center gap-3">
                                <div className="p-2 rounded-lg bg-slate-50">
                                  {device.type === 'WEB' ? <Monitor className="w-4 h-4 text-slate-400" /> : <Smartphone className="w-4 h-4 text-slate-400" />}
                                </div>
                                <div className="flex flex-col">
                                  <div className="flex items-center gap-2">
                                     <span className="text-sm font-bold text-slate-700">{device.alias || '알 수 없는 기기'}</span>
                                     {idx === 0 && <span className="text-[9px] bg-primary/10 text-primary px-1.5 py-0.5 rounded font-black tracking-tighter">THIS</span>}
                                  </div>
                                  <span className="text-[10px] text-slate-400 font-medium">{new Date(device.registeredAt).toLocaleDateString()} 등록</span>
                                </div>
                              </div>
                              <button 
                                type="button" 
                                onClick={() => handleDeleteDevice(device.id)}
                                className="w-8 h-8 flex items-center justify-center text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                              >
                                <X className="w-4 h-4" />
                              </button>
                            </div>
                          ))
                        )}
                      </div>

                      <div className="mt-6 flex items-center justify-between border-t border-slate-200/50 pt-5">
                        <div className="space-y-1">
                          <Label htmlFor="web-enabled" className="text-sm font-bold text-slate-600">브라우저 전체 알림</Label>
                          <p className="text-[11px] text-slate-400 font-medium">기기 개별 설정이 아닌 서비스 전체 알림 스위치입니다.</p>
                        </div>
                        <Switch
                          checked={watch("webPushEnabled")}
                          onCheckedChange={async (checked) => {
                            setValue("webPushEnabled", checked, { shouldDirty: true });
                          }}
                          disabled={loadingWebPush}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* Floating Action Bar */}
          <div className="fixed bottom-0 right-0 left-0 bg-white/90 backdrop-blur-md border-t border-slate-100 px-8 py-5 flex items-center justify-center sm:justify-end gap-4 z-40">
            <Button 
              type="button" 
              variant="ghost"
              onClick={() => router.back()}
              className="text-slate-500 hover:text-slate-700 hover:bg-slate-100 font-bold px-8 h-12 rounded-full transition-all text-sm"
            >
              변경 취소
            </Button>
            <Button 
              type="submit"
              disabled={isSubmitting}
              className="bg-primary hover:bg-primary-hover text-white font-black px-12 h-12 rounded-full transition-all shadow-lg shadow-primary/20 hover:shadow-primary/40 active:scale-[0.95] text-sm tracking-tight"
            >
              {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Save className="w-4 h-4 mr-2" />}
              설정 저장하기
            </Button>
          </div>
        </form>
      </main>
    </div>
  );
}

/**
 * 전용 Switch 컴포넌트 (mockup 스타일)
 */
function Switch({ checked, onCheckedChange, disabled }: { checked: boolean, onCheckedChange: (checked: boolean) => void, disabled?: boolean }) {
  return (
    <div 
      className={cn(
        "relative inline-flex h-7 w-13 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-300 ease-in-out focus-visible:outline-hidden",
        checked ? "bg-primary" : "bg-slate-200",
        disabled && "opacity-50 cursor-not-allowed"
      )}
      onClick={() => !disabled && onCheckedChange(!checked)}
    >
      <span
        aria-hidden="true"
        className={cn(
          "pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-soft transition duration-300 ease-in-out",
          checked ? "translate-x-7" : "translate-x-1"
        )}
      />
    </div>
  );
}
