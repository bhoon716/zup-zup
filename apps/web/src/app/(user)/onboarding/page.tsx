"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useRouter, useSearchParams } from "next/navigation";
import { AppWindow, Bell, Check, Loader2, Mail, MessageSquare } from "lucide-react";
import Link from "next/link";
import { AxiosError } from "axios";
import { toast } from "sonner";
import { z } from "zod";
import * as userApi from "@/features/user/api/user.api";
import { useUser, useCompleteOnboarding } from "@/features/user/hooks/useUser";
import { useWebPush } from "@/features/user/hooks/useWebPush";
import {
  NotificationActions,
  NotificationChannel,
  NotificationDeviceAlias,
  NotificationEmailStatus,
  NotificationSettingsLayout,
  notificationButtonClassName,
  notificationDiscordButtonClassName,
  notificationInputClassName,
} from "@/features/user/ui/notification-settings-layout";
import { trackOnboardingStarted } from "@/shared/analytics/onboarding-events";
import { localizedErrorMessage } from "@/shared/lib/error-message";
import type { OnboardingRequest, User } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";

type OnboardingForm = Required<OnboardingRequest> & { deviceName: string };

const EMAIL_CODE_EXPIRY_SECONDS = 180;
const TEST_COOLDOWN_SECONDS = 10;
const DEVICE_INPUT_ID = "onboarding-device";
const EMAIL_STATUS_ID = "onboarding-email-note";
const emailSchema = z.email();

const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof AxiosError && typeof error.response?.data?.message === "string") {
    return localizedErrorMessage(error.response.data.message, fallbackMessage);
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

const formatTime = (seconds: number) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
};

function OnboardingContent({ user }: { user: User }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const { mutate: completeOnboarding, isPending } = useCompleteOnboarding(user.id);
  const { subscribe, unsubscribe, loading: loadingWebPush } = useWebPush();
  const loginEmail = user.email;
  const discordConnected = Boolean(user.discordId);

  const [pendingVerificationEmail, setPendingVerificationEmail] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);
  const [timeLeft, setTimeLeft] = useState(0);
  const [registeredDeviceName, setRegisteredDeviceName] = useState("");
  const [deviceNameInvalid, setDeviceNameInvalid] = useState(false);
  const [isSendingTest, setIsSendingTest] = useState(false);
  const [testCooldown, setTestCooldown] = useState(0);
  const [testStatus, setTestStatus] = useState("");
  const { isSubmitting, tryStart, finish } = useSubmitGuard();

  const { control, register, handleSubmit, setValue } = useForm<OnboardingForm>({
    defaultValues: {
      notificationEmail: loginEmail,
      emailEnabled: true,
      webPushEnabled: false,
      discordEnabled: discordConnected,
      deviceName: "",
    },
  });

  const notificationEmail = useWatch({ control, name: "notificationEmail" }).trim();
  const emailEnabled = useWatch({ control, name: "emailEnabled" });
  const webPushEnabled = useWatch({ control, name: "webPushEnabled" });
  const discordEnabled = useWatch({ control, name: "discordEnabled" });
  const deviceName = useWatch({ control, name: "deviceName" });
  const emailVerified = notificationEmail !== "" &&
    (notificationEmail === loginEmail || notificationEmail === verifiedEmail);
  const requestingEmailCode = pendingVerificationEmail !== "" && pendingVerificationEmail === notificationEmail;
  const busy = isPending || isSubmitting || loadingWebPush;

  useEffect(() => {
    trackOnboardingStarted(user.id);
  }, [user.id]);

  useEffect(() => {
    setValue("discordEnabled", discordConnected);
  }, [discordConnected, setValue]);

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
    if (!pendingVerificationEmail) return;

    if (timeLeft === 0) {
      const expiration = window.setTimeout(() => {
        setPendingVerificationEmail("");
        toast.error("인증 시간이 만료되었습니다. 다시 시도해 주세요.");
      }, 0);
      return () => window.clearTimeout(expiration);
    }

    const timer = window.setTimeout(() => setTimeLeft(timeLeft - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [pendingVerificationEmail, timeLeft]);

  useEffect(() => {
    if (testCooldown === 0) return;

    const timer = window.setTimeout(() => setTestCooldown(testCooldown - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [testCooldown]);

  const onSendCode = async () => {
    if (!emailSchema.safeParse(notificationEmail).success) return;

    setSending(true);
    try {
      await userApi.sendVerificationCode({ email: notificationEmail });
      setAuthCode("");
      setPendingVerificationEmail(notificationEmail);
      setTimeLeft(EMAIL_CODE_EXPIRY_SECONDS);
      toast.success("인증 코드가 전송되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 코드 전송 실패"));
    } finally {
      setSending(false);
    }
  };

  const onVerifyCode = async () => {
    if (!requestingEmailCode || authCode.length !== 6 || timeLeft === 0 || verifying || sending) return;

    setVerifying(true);
    try {
      await userApi.verifyEmail({ email: pendingVerificationEmail, code: authCode });
      setVerifiedEmail(pendingVerificationEmail);
      setPendingVerificationEmail("");
      setTestStatus("");
      toast.success("이메일이 인증되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 실패"));
    } finally {
      setVerifying(false);
    }
  };

  const toggleChannel = (channel: "emailEnabled" | "webPushEnabled" | "discordEnabled", checked: boolean) => {
    setTestStatus("");
    setValue(channel, checked);
  };

  const handleDiscordConnect = () => {
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/api/v1/users/discord/authorize?returnPath=/onboarding";
  };

  const requireDeviceName = (value: string) => {
    const alias = value.trim();
    if (alias) return alias;

    setDeviceNameInvalid(true);
    document.getElementById(DEVICE_INPUT_ID)?.focus();
    return null;
  };

  const registerDevice = async (alias: string) => {
    toggleChannel("webPushEnabled", false);
    const registered = await subscribe(alias);
    setRegisteredDeviceName(registered ? alias : "");
    toggleChannel("webPushEnabled", registered);
    return registered;
  };

  const enableWebPush = async () => {
    const alias = requireDeviceName(deviceName);
    if (!alias || !tryStart()) return;

    try {
      await registerDevice(alias);
    } finally {
      finish();
    }
  };

  const disableWebPush = async () => {
    if (!tryStart()) return;

    try {
      if (await unsubscribe()) {
        setRegisteredDeviceName("");
        toggleChannel("webPushEnabled", false);
      }
    } finally {
      finish();
    }
  };

  const sendTestNotification = async () => {
    if (testCooldown > 0 || busy) return;

    const request: OnboardingRequest = {
      notificationEmail: emailVerified ? notificationEmail : loginEmail,
      emailEnabled: emailEnabled && emailVerified,
      webPushEnabled: webPushEnabled && registeredDeviceName !== "",
      discordEnabled: discordEnabled && discordConnected,
    };
    if (!request.emailEnabled && !request.webPushEnabled && !request.discordEnabled) {
      setTestStatus("테스트하려면 연결된 채널을 하나 이상 켜 주세요.");
      return;
    }

    if (!tryStart()) return;
    setIsSendingTest(true);
    setTestStatus("");
    try {
      await userApi.sendOnboardingTestNotification(request);
      setTestStatus(emailEnabled && !emailVerified
        ? "연결된 채널에 테스트 알림을 전송했습니다. 이메일은 인증 후 테스트할 수 있어요."
        : "알림 테스트를 전송했습니다.");
      setTestCooldown(TEST_COOLDOWN_SECONDS);
    } catch (error: unknown) {
      if (error instanceof AxiosError && error.response?.status === 429) {
        setTestCooldown(TEST_COOLDOWN_SECONDS);
      }
      setTestStatus(getErrorMessage(error, "알림 테스트 전송에 실패했습니다."));
    } finally {
      setIsSendingTest(false);
      finish();
    }
  };

  const onSubmit = async (data: OnboardingForm) => {
    if (!tryStart()) return;

    let saving = false;
    try {
      const address = data.notificationEmail.trim();
      const verified = address === loginEmail || address === verifiedEmail;
      if (data.emailEnabled && (!address || !verified)) {
        toast.error("이메일 인증을 완료해주세요.");
        document.getElementById("onboarding-email")?.focus();
        return;
      }

      if (data.webPushEnabled) {
        const alias = requireDeviceName(data.deviceName);
        if (!alias || !await registerDevice(alias)) return;
      }

      if (!data.emailEnabled && !data.webPushEnabled && !data.discordEnabled &&
        !confirm("모든 외부 알림을 끄시겠습니까? 빈자리 알림은 줍줍 알림함에서 확인할 수 있습니다.")) {
        return;
      }

      completeOnboarding(
        {
          notificationEmail: address && verified ? address : loginEmail,
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

  return (
    <NotificationSettingsLayout description="알림을 받을 채널을 선택하세요." standalone>
      <form onSubmit={handleSubmit(onSubmit)} aria-label="알림 채널" noValidate className="mt-5">
        <div className="grid gap-2.5">
          <NotificationChannel
            id="onboarding-discord"
            title="디스코드 DM 알림"
            description="디스코드 개인 메시지로 받아요."
            icon={MessageSquare}
            iconClassName="bg-indigo-50 text-indigo-600"
            toggle={
              <Switch
                aria-label="디스코드 DM 알림"
                checked={discordEnabled}
                onCheckedChange={(checked) => toggleChannel("discordEnabled", checked)}
                disabled={!discordConnected || busy}
              />
            }
          >
            <div className="flex min-h-[38px] items-center justify-end gap-2.5">
              <p aria-live="polite" className={discordConnected ? "text-xs font-semibold text-emerald-700" : "text-xs text-muted-foreground"}>
                {discordConnected ? "연동됨" : "연동 안 됨"}
              </p>
              {!discordConnected && (
                <Button
                  type="button"
                  onClick={handleDiscordConnect}
                  disabled={busy}
                  className={`${notificationButtonClassName} ${notificationDiscordButtonClassName}`}
                >
                  디스코드 계정 연동
                </Button>
              )}
            </div>
          </NotificationChannel>

          <NotificationChannel
            id="onboarding-email-channel"
            title="이메일 알림"
            description="인증한 주소로 알림을 받아요."
            icon={Mail}
            iconClassName="bg-purple-50 text-primary"
            toggle={
              <Switch
                id="emailEnabled"
                aria-label="이메일 알림"
                checked={emailEnabled}
                onCheckedChange={(checked) => toggleChannel("emailEnabled", checked)}
                disabled={busy}
              />
            }
          >
            <div className="flex items-center gap-1.5">
              <div className="relative min-w-0 flex-1">
                <Input
                  id="onboarding-email"
                  aria-label="수신 이메일"
                  aria-describedby={EMAIL_STATUS_ID}
                  placeholder="example@email.com"
                  type="email"
                  autoComplete="email"
                  readOnly={busy || sending || verifying}
                  {...register("notificationEmail", {
                    onChange: () => {
                      setPendingVerificationEmail("");
                      setTestStatus("");
                    },
                  })}
                  className={`${notificationInputClassName} pe-[34px]`}
                />
                <NotificationEmailStatus verified={emailVerified} />
              </div>
              {!emailVerified && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={onSendCode}
                  disabled={busy || sending || verifying || !emailSchema.safeParse(notificationEmail).success}
                  className={notificationButtonClassName}
                >
                  {sending ? "전송 중…" : requestingEmailCode ? "다시 보내기" : "인증하기"}
                </Button>
              )}
            </div>
            <p id={EMAIL_STATUS_ID} className="mt-[5px] px-0.5 text-xs leading-normal text-muted-foreground">
              {emailVerified ? "다른 이메일을 쓰려면 인증해 주세요." : "수신 이메일을 인증해 주세요."}
            </p>

            {!emailVerified && requestingEmailCode && (
              <div className="mt-2 space-y-1.5">
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="onboarding-email-code" className="text-xs">인증 코드</Label>
                  <span className="text-xs tabular-nums text-muted-foreground">{formatTime(timeLeft)}</span>
                </div>
                <div className="flex gap-1.5">
                  <Input
                    id="onboarding-email-code"
                    placeholder="인증 코드 6자리 입력"
                    autoComplete="one-time-code"
                    inputMode="numeric"
                    value={authCode}
                    readOnly={busy || sending || verifying}
                    onChange={(event) => setAuthCode(event.target.value.replace(/[^0-9]/g, ""))}
                    onKeyDown={(event) => {
                      if (event.key !== "Enter" || event.nativeEvent.isComposing) return;
                      event.preventDefault();
                      if (!busy) void onVerifyCode();
                    }}
                    maxLength={6}
                    className={notificationInputClassName}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={onVerifyCode}
                    disabled={busy || sending || verifying || authCode.length !== 6 || timeLeft === 0}
                    className={notificationButtonClassName}
                  >
                    {verifying ? "확인 중…" : "확인"}
                  </Button>
                </div>
              </div>
            )}
          </NotificationChannel>

          <NotificationChannel
            id="onboarding-web-push"
            title="웹 푸시 (현재 기기)"
            description="이 브라우저에서 알림을 받아요."
            icon={AppWindow}
            iconClassName="bg-blue-50 text-blue-600"
            toggle={
              <Switch
                aria-label="웹 푸시 (현재 기기)"
                checked={webPushEnabled}
                onCheckedChange={(checked) => {
                  if (checked) void enableWebPush();
                  else void disableWebPush();
                }}
                disabled={busy}
              />
            }
          >
            <NotificationDeviceAlias
              id={DEVICE_INPUT_ID}
              value={deviceName}
              onChange={(value) => {
                setValue("deviceName", value);
                setDeviceNameInvalid(false);
              }}
              onRegister={enableWebPush}
              invalid={deviceNameInvalid}
              disabled={busy}
            />
            {deviceNameInvalid && (
              <p id={`${DEVICE_INPUT_ID}-error`} role="alert" className="mt-[5px] px-0.5 text-xs text-destructive">
                기기 별칭을 입력해 주세요.
              </p>
            )}
            {registeredDeviceName && (
              <p role="status" className="mt-[5px] wrap-anywhere px-0.5 text-xs text-muted-foreground">
                {registeredDeviceName} 등록 완료
              </p>
            )}
          </NotificationChannel>
        </div>

        <NotificationActions>
          <Button
            type="button"
            variant="outline"
            onClick={sendTestNotification}
            disabled={busy || testCooldown > 0}
            className={notificationButtonClassName}
          >
            {isSendingTest ? <Loader2 aria-hidden="true" className="size-[15px] animate-spin" /> : <Bell aria-hidden="true" className="size-[15px]" />}
            {isSendingTest ? "전송 중…" : testCooldown > 0 ? `알림 테스트 (${testCooldown}초)` : "알림 테스트"}
          </Button>
          <Button
            type="submit"
            disabled={busy || sending || verifying}
            className={`${notificationButtonClassName} min-w-[95px]`}
          >
            {isPending ? <Loader2 aria-hidden="true" className="size-[15px] animate-spin" /> : <Check aria-hidden="true" className="size-[15px]" />}
            {isPending ? "저장 중…" : "설정 완료"}
          </Button>
        </NotificationActions>
        <p role="status" className="mt-2 text-[13px] text-primary empty:hidden">{testStatus}</p>
        <p className="mt-6 text-center text-xs leading-relaxed text-muted-foreground">
          시작 시 서비스 <Link href="/terms" className="underline hover:text-foreground">이용약관</Link> 및{" "}
          <Link href="/privacy" className="underline hover:text-foreground">개인정보 처리방침</Link>에 동의하는 것으로 간주됩니다.
        </p>
      </form>
    </NotificationSettingsLayout>
  );
}

function OnboardingSession() {
  const router = useRouter();
  const { data: user, isLoading } = useUser();

  useEffect(() => {
    if (user?.onboardingCompleted) router.replace("/");
  }, [user?.onboardingCompleted, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="size-8 animate-spin rounded-full border-b-2 border-primary" />
      </div>
    );
  }

  if (!user || user.onboardingCompleted) return null;

  return <OnboardingContent key={user.id} user={user} />;
}

export default function OnboardingPage() {
  return (
    <Suspense fallback={null}>
      <OnboardingSession />
    </Suspense>
  );
}
