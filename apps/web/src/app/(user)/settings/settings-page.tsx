"use client";

import { ArrowLeft, Bell, Loader2 } from "lucide-react";
import Link from "next/link";
import { Button } from "@/shared/ui/button";
import type { SettingsPageModel } from "./useSettingsPage";
import {
  DiscordIntegrationSection,
  EmailNotificationSection,
  SettingsActionBar,
  WebPushSection,
} from "./settings-sections";

export function SettingsPage({ model }: { model: SettingsPageModel }) {
  const {
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
    needsVerification,
    router,
  } = model;

  if (loading) {
    return (
      <div className="flex h-[60vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-4 pb-12 pt-6 text-foreground sm:px-6 sm:pt-9">
      <Button asChild variant="ghost" size="sm" className="-ml-2.5 mb-3 text-muted-foreground">
        <Link href="/profile">
          <ArrowLeft aria-hidden="true" />
          프로필로 돌아가기
        </Link>
      </Button>
      <header className="mb-7 flex items-start gap-4 rounded-xl border bg-primary/5 px-4 py-5 sm:items-center sm:gap-5 sm:px-6 sm:py-6">
        <span aria-hidden="true" className="grid size-11 shrink-0 place-items-center rounded-lg bg-primary text-primary-foreground sm:size-13">
          <Bell className="size-5 sm:size-6" />
        </span>
        <div>
          <h1 className="text-2xl leading-8 font-semibold sm:text-3xl sm:leading-10">알림 설정</h1>
          <p className="mt-1 break-keep text-sm leading-6 text-muted-foreground">빈자리, 문의 답변, 중요 공지 알림을 받을 채널을 설정하세요.</p>
        </div>
      </header>
      <form
        aria-labelledby="notification-channels-heading"
        onSubmit={handleSubmit(onSubmit)}
        className="min-w-0"
      >
        <h2 id="notification-channels-heading" className="mb-3 text-lg leading-7 font-semibold">알림 채널</h2>
        <div className="space-y-3">
          <DiscordIntegrationSection
            user={user}
            watch={watch}
            setValue={setValue}
            isUnlinking={isUnlinking}
            handleDiscordConnect={handleDiscordConnect}
            handleDiscordUnlink={handleDiscordUnlink}
          />
          <EmailNotificationSection
            user={user}
            authCode={authCode}
            setAuthCode={setAuthCode}
            verified={verified}
            emailSent={emailSent}
            sending={sending}
            verifying={verifying}
            register={register}
            watch={watch}
            setValue={setValue}
            errors={errors}
            onSendCode={onSendCode}
            onVerifyCode={onVerifyCode}
            isGoogleEmail={isGoogleEmail}
            needsVerification={needsVerification}
          />
          <WebPushSection
            deviceAlias={deviceAlias}
            setDeviceAlias={setDeviceAlias}
            devices={devices}
            loadingWebPush={loadingWebPush}
            watch={watch}
            setValue={setValue}
            handleRegisterDevice={handleRegisterDevice}
            handleDeleteDevice={handleDeleteDevice}
          />
        </div>
        <SettingsActionBar
          isSubmitting={isSubmitting}
          onCancel={() => router.back()}
          isSendingTest={isSendingTest}
          testCooldownSeconds={testCooldownSeconds}
          handleSendTestNotification={handleSendTestNotification}
        />
      </form>
    </main>
  );
}
