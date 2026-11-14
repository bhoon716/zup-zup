"use client";

import { localizedErrorMessage } from "@/shared/lib/error-message";
import { Suspense, useEffect, useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useRouter, useSearchParams } from "next/navigation";
import { useUser, useCompleteOnboarding } from "@/features/user/hooks/useUser";
import { useWebPush } from "@/features/user/hooks/useWebPush";
import * as userApi from "@/features/user/api/user.api";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Switch } from "@/shared/ui/switch";
import { Check, Loader2, Smartphone } from "lucide-react";
import { Label } from "@/shared/ui/label";
import { toast } from "sonner";
import Link from "next/link";
import { AxiosError } from "axios";
import { trackOnboardingStarted } from "@/shared/analytics/onboarding-events";

interface OnboardingForm {
  notificationEmail: string;
  emailEnabled: boolean;
  webPushEnabled: boolean;
  discordEnabled: boolean;
  deviceName: string;
}

const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof AxiosError) {
    const responseData = error.response?.data as { message?: string } | undefined;
    return localizedErrorMessage(responseData?.message, fallbackMessage);
  }
  return fallbackMessage;
};

const useSubmitGuard = () => {
  const inFlightRef = useRef(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const tryStart = () => {
    if (inFlightRef.current) return false;

    inFlightRef.current = true;
    setIsSubmitting(true);
    return true;
  };

  const finish = () => {
    inFlightRef.current = false;
    setIsSubmitting(false);
  };

  return { isSubmitting, tryStart, finish };
};

function OnboardingContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const { data: user, isLoading } = useUser();
  const { mutate: completeOnboarding, isPending } = useCompleteOnboarding(user?.id);
  const { subscribe, unsubscribe, loading: loadingWebPush } = useWebPush();

  const [emailSent, setEmailSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);
  const [timeLeft, setTimeLeft] = useState(180);
  const { isSubmitting, tryStart, finish } = useSubmitGuard();

  const { control, register, handleSubmit, setValue, formState: { errors }, trigger } = useForm<OnboardingForm>({
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: false,
      discordEnabled: false,
    },
  });

  const notificationEmail = useWatch({ control, name: "notificationEmail" });
  const emailEnabled = useWatch({ control, name: "emailEnabled" });
  const webPushEnabled = useWatch({ control, name: "webPushEnabled" });
  const discordEnabled = useWatch({ control, name: "discordEnabled" });
  const deviceName = useWatch({ control, name: "deviceName" });

  useEffect(() => {
    if (!isLoading && user && !user.onboardingCompleted) {
      trackOnboardingStarted(user.id);
    }
  }, [isLoading, user]);

  useEffect(() => {
    if (user) {
      if (user.onboardingCompleted) {

        router.replace("/");
      } else {
        if (!notificationEmail && !verified) {
          setValue("notificationEmail", user.email);
        }
        if (user.discordId) {
          setValue("discordEnabled", true);
        }
      }
    }
  }, [user, router, setValue, notificationEmail, verified]);

  useEffect(() => {
    if (discordStatus === "success") {
      toast.success("디스코드 연동이 성공적으로 완료되었습니다.");
      router.replace("/onboarding");
    } else if (discordStatus === "error") {
      toast.error("디스코드 연동 중 오류가 발생했습니다.");
      router.replace("/onboarding");
    }
  }, [discordStatus, router]);

  useEffect(() => {
    let timer: NodeJS.Timeout;
    let expirationTimer: NodeJS.Timeout;
    if (emailSent && timeLeft > 0 && !verified) {

      timer = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0) {
      expirationTimer = setTimeout(() => {
        setEmailSent(false);
        toast.error("인증 시간이 만료되었습니다. 다시 시도해주세요.");
      }, 0);
    }
    return () => {
      clearInterval(timer);
      clearTimeout(expirationTimer);
    };
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
      setTimeLeft(180);
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

    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/api/v1/users/discord/authorize?returnPath=/onboarding";
  };

  const updateWebPush = async (enabled: boolean, customAlias?: string) => {
    if (!tryStart()) return;

    setValue("webPushEnabled", false);
    try {
      if (enabled) {
        const registered = await subscribe(customAlias);
        setValue("webPushEnabled", registered);
      } else {
        await unsubscribe();
      }
    } finally {
      finish();
    }
  };

  const handleRegisterDevice = async () => {
    if (!deviceName?.trim()) {
      toast.error("기기 이름을 입력해 주세요.");
      return;
    }
    await updateWebPush(true, deviceName.trim());
  };

  const onSubmit = async (data: OnboardingForm) => {
    if (!tryStart()) return;

    let saving = false;
    try {
      if (!(user && data.notificationEmail === user.email)) {

        if (data.emailEnabled && !verified) {
          toast.error("이메일 인증을 완료해주세요.");
          return;
        }
      }

      if (data.webPushEnabled && !data.deviceName?.trim()) {
        toast.error("알림을 받을 기기 이름을 입력해 주세요.");
        return;
      }

      if (data.webPushEnabled) {
        setValue("webPushEnabled", false);
        const registered = await subscribe(data.deviceName);
        if (!registered) return;
        setValue("webPushEnabled", true);
      }

      if (!data.emailEnabled && !data.webPushEnabled && !data.discordEnabled) {
        if (!confirm("모든 외부 알림을 끄시겠습니까? 빈자리 알림은 줍줍 알림함에서 확인할 수 있습니다.")) {
          return;
        }
      }

      completeOnboarding(
        {
          notificationEmail: data.notificationEmail,
          emailEnabled: data.emailEnabled,
          webPushEnabled: data.webPushEnabled,
          discordEnabled: data.discordEnabled,
        },
        {
          onSuccess: () => {
            router.replace("/");
          },
          onSettled: finish,
        }
      );
      saving = true;
    } finally {
      if (!saving) finish();
    }
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
    <main className="mx-auto w-full max-w-2xl px-4 pt-6 pb-12 text-foreground sm:px-6 sm:pt-9">
      <header className="mb-5 sm:mb-6">
        <h1 className="text-2xl font-semibold tracking-tight sm:text-3xl">알림 설정</h1>
        <p className="mt-2 break-keep text-sm text-muted-foreground">
          빈자리, 문의 답변, 중요 공지 알림을 받을 채널을 선택하세요.
        </p>
      </header>

      <form onSubmit={handleSubmit(onSubmit)} aria-label="알림 채널">
        <div className="divide-y rounded-xl border bg-card">
          <section aria-labelledby="onboarding-discord-heading" className="p-4 sm:px-5">
            <header className="flex items-start justify-between gap-3">
              <div>
                <h2 id="onboarding-discord-heading" className="font-semibold">Discord 연동</h2>
                <p className="mt-1 text-sm text-muted-foreground">연결한 계정의 DM으로 알림을 받습니다.</p>
              </div>
              <Switch
                aria-label="디스코드 DM 알림"
                checked={discordEnabled}
                onCheckedChange={(checked) => setValue("discordEnabled", checked)}
                disabled={!user.discordId}
                className="mt-0.5"
              />
            </header>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-2">
              {user.discordId ? (
                <p className="text-sm text-muted-foreground">연동됨</p>
              ) : (
                <>
                  <p className="text-sm text-muted-foreground">연동된 계정 없음</p>
                  <Button type="button" variant="outline" size="sm" onClick={handleDiscordConnect}>
                    Discord 계정 연결하기
                  </Button>
                </>
              )}
            </div>
          </section>

          <section aria-labelledby="onboarding-email-heading" className="p-4 sm:px-5">
            <header className="flex items-start justify-between gap-3">
              <div>
                <h2 id="onboarding-email-heading" className="font-semibold">이메일 알림</h2>
                <p className="mt-1 text-sm text-muted-foreground">수신 주소를 변경하면 이메일 인증이 필요합니다.</p>
              </div>
              <Switch
                id="emailEnabled"
                aria-label="이메일 알림 활성화"
                checked={emailEnabled}
                onCheckedChange={(checked) => setValue("emailEnabled", checked)}
                className="mt-0.5"
              />
            </header>
            <div className="mt-4 space-y-3">
              <div className="space-y-1.5">
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="onboarding-email">수신 이메일</Label>
                  {(isGoogleEmail || verified) && <span className="text-xs text-muted-foreground">인증됨</span>}
                </div>
                <div className="flex flex-col gap-2 sm:flex-row">
                  <Input
                    id="onboarding-email"
                    placeholder="example@email.com"
                    type="email"
                    autoComplete="email"
                    readOnly={verified}
                    aria-invalid={!!errors.notificationEmail}
                    aria-describedby={errors.notificationEmail ? "onboarding-email-error" : undefined}
                    {...register("notificationEmail", { required: true })}
                    className="h-10 flex-1"
                  />
                  {!isGoogleEmail && !verified && (
                    <Button type="button" variant="outline" onClick={onSendCode} disabled={sending} className="h-10">
                      {sending ? "전송 중…" : "인증 코드 보내기"}
                    </Button>
                  )}
                </div>
                {errors.notificationEmail && (
                  <p id="onboarding-email-error" className="text-xs text-destructive">수신 이메일을 입력해 주세요.</p>
                )}
              </div>

              {!verified && emailSent && (
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between gap-2">
                    <Label htmlFor="onboarding-email-code">인증 코드</Label>
                    <span className="text-xs text-muted-foreground tabular-nums">{formatTime(timeLeft)}</span>
                  </div>
                  <div className="flex gap-2">
                    <Input
                      id="onboarding-email-code"
                      placeholder="인증 코드 6자리 입력"
                      autoComplete="one-time-code"
                      inputMode="numeric"
                      value={authCode}
                      onChange={(event) => setAuthCode(event.target.value)}
                      maxLength={6}
                      className="h-10 flex-1"
                    />
                    <Button
                      type="button"
                      variant="outline"
                      onClick={onVerifyCode}
                      disabled={verifying || authCode.length !== 6}
                      className="h-10"
                    >
                      {verifying ? "확인 중…" : "확인"}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section aria-labelledby="onboarding-web-push-heading" className="p-4 sm:px-5">
            <header className="flex items-start justify-between gap-3">
              <div>
                <h2 id="onboarding-web-push-heading" className="font-semibold">웹 푸시 (현재 기기)</h2>
                <p className="mt-1 break-keep text-sm text-muted-foreground">
                  등록한 기기의 브라우저로 알림을 받습니다. 기기 등록은 선택 사항입니다.
                </p>
              </div>
              <Switch
                aria-labelledby="onboarding-web-push-heading"
                checked={webPushEnabled}
                onCheckedChange={(checked) => {
                  void updateWebPush(checked, deviceName?.trim() || undefined);
                }}
                disabled={isPending || isSubmitting || loadingWebPush}
                className="mt-0.5"
              />
            </header>
            <div className="mt-4 space-y-1.5">
              <Label htmlFor="onboarding-device">기기 이름</Label>
              <div className="flex flex-col gap-2 sm:flex-row">
                <Input
                  id="onboarding-device"
                  placeholder="기기 이름 (예: 내 노트북, 스마트폰)"
                  aria-invalid={!!errors.deviceName}
                  aria-describedby={errors.deviceName ? "onboarding-device-error" : undefined}
                  {...register("deviceName", { required: webPushEnabled })}
                  className="h-10 flex-1"
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleRegisterDevice}
                  disabled={isPending || isSubmitting || loadingWebPush}
                  className="h-10"
                >
                  <Smartphone aria-hidden="true" />
                  현재 기기 등록
                </Button>
              </div>
              {errors.deviceName && (
                <p id="onboarding-device-error" className="text-xs text-destructive">기기 이름을 입력해 주세요.</p>
              )}
            </div>
          </section>
        </div>

        <div className="mt-4 flex justify-end">
          <Button type="submit" disabled={isPending || isSubmitting || loadingWebPush} className="w-full sm:w-auto">
            {isPending ? <Loader2 aria-hidden="true" className="animate-spin" /> : <Check aria-hidden="true" />}
            {isPending ? "저장 중…" : "완료"}
          </Button>
        </div>
        <p className="mt-3 text-right text-xs leading-relaxed text-muted-foreground">
          시작 시 <Link href="/terms" className="underline underline-offset-2 hover:text-foreground">이용약관</Link> 및{" "}
          <Link href="/privacy" className="underline underline-offset-2 hover:text-foreground">개인정보 처리방침</Link>에 동의하게 됩니다.
        </p>
      </form>
    </main>
  );
}

export default function OnboardingPage() {
  return (
    <Suspense fallback={null}>
      <OnboardingContent />
    </Suspense>
  );
}
