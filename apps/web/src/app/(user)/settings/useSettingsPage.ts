"use client";

import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { toast } from "sonner";
import { AxiosError } from "axios";
import { useRouter, useSearchParams } from "next/navigation";
import { useWebPush } from "@/features/user/hooks/useWebPush";
import type { User, UserDeviceResponse } from "@/shared/types/api";
import {
  getMyProfile,
  updateSettings,
  getDevices,
  deleteDevice,
  unlinkDiscord,
  sendVerificationCode,
  verifyEmail,
  sendTestNotification,
} from "@/features/user/api/user.api";

const settingsSchema = z.object({
  notificationEmail: z.string().email("유효한 이메일 주소를 입력해 주세요.").or(z.literal("")),
  emailEnabled: z.boolean(),
  webPushEnabled: z.boolean(),
  fcmEnabled: z.boolean(),
  discordEnabled: z.boolean(),
});

export type SettingsFormValues = z.infer<typeof settingsSchema>;

/**
 * API 에러 객체에서 메시지를 추출하거나 기본 메시지를 반환합니다.
 */
const getErrorMessage = (error: unknown, fallbackMessage: string) => {
  if (error instanceof AxiosError) {
    const responseData = error.response?.data as { message?: string } | undefined;
    return responseData?.message || fallbackMessage;
  }
  return fallbackMessage;
};

export const useSettingsPage = () => {
  const router = useRouter();
  const searchParams = useSearchParams();
  const discordStatus = searchParams.get("discord");

  const [user, setUser] = useState<User | null>(null);
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
    watch,
    setValue,
    trigger,
    control,
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

  const notificationEmail = useWatch({
    control,
    name: "notificationEmail",
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
    const fetchProfile = async () => {
      try {
        const [profileRes, deviceRes] = await Promise.all([
          getMyProfile(),
          getDevices(),
        ]);
        const userData = profileRes.data;
        setUser(userData);

        const initialEmail = userData.notificationEmail || "";

        reset({
          notificationEmail: initialEmail,
          emailEnabled: userData.emailEnabled,
          webPushEnabled: userData.webPushEnabled,
          fcmEnabled: userData.fcmEnabled,
          discordEnabled: userData.discordEnabled,
        });

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
    if (testCooldownSeconds <= 0) {
      return;
    }

    const timer = window.setInterval(() => {
      setTestCooldownSeconds((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [testCooldownSeconds]);

  const onSendCode = async () => {
    const valid = await trigger("notificationEmail");
    if (!valid || !notificationEmail) return;

    setSending(true);
    try {
      await sendVerificationCode({ email: notificationEmail });
      setPendingVerificationEmail(notificationEmail);
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
      await verifyEmail({ email: notificationEmail, code: authCode });
      setVerifiedEmail(notificationEmail);
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
      const response = await sendTestNotification();
      toast.success(response.message || "알림 테스트를 전송했습니다.");
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
    // Discord OAuth starts on the backend and must perform a full document navigation.
    // eslint-disable-next-line @next/next/no-location-assign-relative-destination
    window.location.href = "/api/v1/users/discord/authorize?returnPath=/settings";
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
        setValue("webPushEnabled", true, { shouldDirty: true });
        toast.success("현재 기기가 등록되었습니다.");
      }
    } catch (error: unknown) {
      console.error("Failed to register device:", error);
      toast.error(getErrorMessage(error, "기기 등록 중 오류가 발생했습니다. 메세지: " + (error instanceof Error ? error.message : "알 수 없음")));
    }
  };

  const handleDeleteDevice = async (id: number) => {
    if (!confirm("이 기기를 삭제하시겠습니까? 더 이상 알림을 받을 수 없습니다.")) return;

    try {
      await deleteDevice(id);
      setDevices((prev) => prev.filter((device) => device.id !== id));
      toast.success("기기가 삭제되었습니다.");
    } catch (error: unknown) {
      toast.error(getErrorMessage(error, "기기 삭제 실패"));
    }
  };

  const onSubmit = async (values: SettingsFormValues) => {
    if (user) {
      const isOriginal = values.notificationEmail === (user.notificationEmail || "");
      const isGoogle = values.notificationEmail === user.email || (!values.notificationEmail && !user.notificationEmail);
      const isVerified = isOriginal || isGoogle || verifiedEmail === values.notificationEmail;

      if (!isOriginal && !isGoogle) {
        if (values.emailEnabled && !isVerified) {
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

  const isGoogleEmail = user?.email === notificationEmail;
  const isOriginal = user && notificationEmail === (user.notificationEmail || "");
  const needsVerification = !isOriginal && !isGoogleEmail && notificationEmail;
  const verified = !!notificationEmail && (isOriginal || isGoogleEmail || verifiedEmail === notificationEmail);
  const emailSent = pendingVerificationEmail === notificationEmail && !verified;

  return {
    router,
    user,
    loading,
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
    reset,
    watch,
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
    isGoogleEmail,
    isOriginal,
    needsVerification,
  };
};

export type SettingsPageModel = ReturnType<typeof useSettingsPage>;
