"use client";

import { localizedErrorMessage } from "@/shared/lib/error-message";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { AxiosError, CanceledError, isCancel } from "axios";
import { useRouter, useSearchParams } from "next/navigation";
import { useWebPush } from "@/features/user/hooks/useWebPush";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import type { User, UserDeviceResponse } from "@/shared/types/api";
import {
  updateSettings,
  getDevices,
  deleteDevice,
  unlinkDiscord,
  sendVerificationCode,
  verifyEmail,
  sendTestNotification,
} from "@/features/user/api/user.api";
import { reportClientError } from "@/shared/telemetry/client-error";

const settingsSchema = z.object({
  notificationEmail: z.string().trim().pipe(z.email("유효한 이메일 주소를 입력해 주세요.")),
  emailEnabled: z.boolean(),
  webPushEnabled: z.boolean(),
  discordEnabled: z.boolean(),
});

export type SettingsFormValues = z.infer<typeof settingsSchema>;

const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof AxiosError) {
    const responseData: unknown = error.response?.data;
    if (responseData && typeof responseData === "object" && "message" in responseData) {
      return localizedErrorMessage(responseData.message, fallbackMessage);
    }
  }
  return fallbackMessage;
};

function savedSettings(user: User): SettingsFormValues {
  return {
    notificationEmail: user.notificationEmail || user.email,
    emailEnabled: user.emailEnabled,
    webPushEnabled: user.webPushEnabled,
    discordEnabled: user.discordEnabled,
  };
}

function loadSettingsProfile() {
  const auth = useAuthStore.getState();
  return auth.refreshUser(auth.hasCheckedSession ? undefined : { silentAuthFailure: true });
}

export const useSettingsPage = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const authUser = useAuthStore((state) => state.isAuthenticated ? state.user : null);
  const refreshUser = useAuthStore((state) => state.refreshUser);
  const [readyUserId, setReadyUserId] = useState<number | null>(null);
  const user = authUser?.id === readyUserId ? authUser : null;
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingVerificationEmail, setPendingVerificationEmail] = useState("");
  const [verifiedEmail, setVerifiedEmail] = useState("");
  const [authCode, setAuthCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [sending, setSending] = useState(false);
  const [isUnlinking, setIsUnlinking] = useState(false);
  const [deviceAlias, setDeviceAlias] = useState("");
  const [devices, setDevices] = useState<UserDeviceResponse[]>([]);
  const [isSendingTest, setIsSendingTest] = useState(false);
  const [testCooldownSeconds, setTestCooldownSeconds] = useState(0);

  const { subscribe, loading: loadingWebPush } = useWebPush();

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    setError,
    trigger,
    control,
    formState: { errors },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      notificationEmail: "",
      emailEnabled: true,
      webPushEnabled: true,
      discordEnabled: false,
    },
  });

  const [notificationEmail, emailEnabled, webPushEnabled, discordEnabled] = useWatch({
    control,
    name: ["notificationEmail", "emailEnabled", "webPushEnabled", "discordEnabled"],
  });

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
    let active = true;
    const fetchProfile = async () => {
      try {
        const [profileRes, deviceRes] = await Promise.all([
          loadSettingsProfile().then(profile => ({ profile, version: useAuthStore.getState().sessionVersion })),
          getDevices(),
        ]);
        if (!active) return;
        if (profileRes.version !== useAuthStore.getState().sessionVersion) throw new CanceledError();
        const userData = profileRes.profile;
        setReadyUserId(userData.id);

        reset(savedSettings(userData));
        setDevices(deviceRes);
      } catch (error) {
        if (!active || isCancel(error)) return;
        reportClientError(error, { source: "settings", operation: "fetch-profile" });
        toast.error("프로필 정보를 불러오는데 실패했습니다.");
      } finally {
        if (active) setLoading(false);
      }
    };

    void fetchProfile();
    return () => { active = false; };
  }, [reset]);

  useEffect(() => {
    if (testCooldownSeconds <= 0) {
      return;
    }

    const timer = window.setInterval(() => {
      setTestCooldownSeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [testCooldownSeconds]);

  const onSendCode = async () => {
    const email = notificationEmail.trim();
    if (sending || !email || !await trigger("notificationEmail")) return;

    setSending(true);
    try {
      await sendVerificationCode({ email });
      setPendingVerificationEmail(email);
      setAuthCode("");
      toast.success("인증 코드가 전송되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 코드 전송 실패"));
    } finally {
      setSending(false);
    }
  };

  const onVerifyCode = async () => {
    const email = notificationEmail.trim();
    if (verifying || authCode.length !== 6 || pendingVerificationEmail !== email) return;
    setVerifying(true);
    try {
      await verifyEmail({ email, code: authCode });
      setVerifiedEmail(email);
      setPendingVerificationEmail("");
      toast.success("이메일이 인증되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "인증 실패"));
    } finally {
      setVerifying(false);
    }
  };

  const handleSendTestNotification = async () => {
    if (testCooldownSeconds > 0) {
      toast.error(`알림 테스트는 ${testCooldownSeconds}초 후 다시 시도할 수 있습니다.`);
      return;
    }

    setIsSendingTest(true);
    try {
      await sendTestNotification();
      toast.success("알림 테스트를 전송했습니다.");
      setTestCooldownSeconds(10);
    } catch (error: unknown) {
      if (error instanceof AxiosError && error.response?.status === 429) {
        setTestCooldownSeconds(10);
      }
      toast.error(getErrorMessage(error, "알림 테스트 전송에 실패했습니다."));
    } finally {
      setIsSendingTest(false);
    }
  };

  const handleDiscordConnect = () => {
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/api/v1/users/discord/authorize?returnPath=/settings";
  };

  const handleDiscordUnlink = async () => {
    if (!confirm("디스코드 연동을 해제하시겠습니까?")) return;

    setIsUnlinking(true);
    const version = useAuthStore.getState().sessionVersion;
    try {
      await unlinkDiscord();
      if (version !== useAuthStore.getState().sessionVersion) return;
      toast.success("디스코드 연동이 해제되었습니다.");
      await refreshUser();
      if (version !== useAuthStore.getState().sessionVersion) return;
      setValue("discordEnabled", false);
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "연동 해제 실패"));
    } finally {
      setIsUnlinking(false);
    }
  };

  const handleRegisterDevice = async () => {
    const alias = deviceAlias.trim();
    if (!alias || loadingWebPush) return;

    try {
      const success = await subscribe(alias);
      if (success) {
        const deviceRes = await getDevices();
        setDevices(deviceRes);
        setDeviceAlias("");
        setValue("webPushEnabled", true);
        toast.success("현재 기기가 등록되었습니다.");
      }
    } catch (error: unknown) {
      reportClientError(error, { source: "settings", operation: "register-device" });
      toast.error(getErrorMessage(error, "기기 등록 중 오류가 발생했습니다."));
    }
  };

  const handleDeleteDevice = async (id: number) => {
    if (!confirm("이 기기를 삭제하시겠습니까? 이 기기에서는 웹 푸시 알림을 받을 수 없습니다.")) return;

    try {
      await deleteDevice(id);
      setDevices((prev) => prev.filter((device) => device.id !== id));
      toast.success("기기가 삭제되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "기기 삭제 실패"));
    }
  };

  const onSubmit = async (values: SettingsFormValues) => {
    if (!user) {
      toast.error("프로필 정보를 확인한 뒤 다시 시도해 주세요.");
      return;
    }
    const isOriginal = values.notificationEmail === user.notificationEmail;
    const isGoogle = values.notificationEmail === user.email;
    const isVerified = isOriginal || isGoogle || verifiedEmail === values.notificationEmail;

    if (!isVerified) {
      setError("notificationEmail", { message: "수신 이메일을 먼저 인증해 주세요." }, { shouldFocus: true });
      return;
    }

    setIsSubmitting(true);
    const version = useAuthStore.getState().sessionVersion;
    try {
      const response = await updateSettings(values);
      if (version !== useAuthStore.getState().sessionVersion) return;
      useAuthStore.getState().setUser(response);
      toast.success("알림 설정이 저장되었습니다.");
      reset(savedSettings(response));
      setPendingVerificationEmail("");
      setAuthCode("");
    } catch (error: unknown) {
      reportClientError(error, { source: "settings", operation: "update" });
      toast.error(getErrorMessage(error, "설정 저장에 실패했습니다."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const email = notificationEmail.trim();
  const verified = Boolean(email && (user?.notificationEmail === email || user?.email === email || verifiedEmail === email));
  const emailSent = Boolean(email && pendingVerificationEmail === email && !verified);
  const saved = user && savedSettings(user);
  const isDirty = Boolean(saved && (
    email !== saved.notificationEmail
    || emailEnabled !== saved.emailEnabled
    || webPushEnabled !== saved.webPushEnabled
    || discordEnabled !== saved.discordEnabled
  ));

  const cancelChanges = () => {
    if (!saved) return;
    reset(saved);
    setPendingVerificationEmail("");
    setAuthCode("");
  };

  return {
    user,
    loading,
    isDirty,
    isSubmitting,
    emailSent,
    verified,
    authCode,
    setAuthCode,
    verifying,
    sending,
    isUnlinking,
    deviceAlias,
    setDeviceAlias,
    devices,
    isSendingTest,
    testCooldownSeconds,
    loadingWebPush,
    register,
    handleSubmit,
    notificationEmail,
    emailEnabled,
    webPushEnabled,
    discordEnabled,
    setValue,
    errors,
    onSendCode,
    onVerifyCode,
    handleSendTestNotification,
    handleDiscordConnect,
    handleDiscordUnlink,
    handleRegisterDevice,
    handleDeleteDevice,
    onSubmit,
    cancelChanges,
  };
};

export type SettingsPageModel = ReturnType<typeof useSettingsPage>;
