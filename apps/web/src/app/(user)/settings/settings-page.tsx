"use client";

import { Bell, Bookmark, Loader2 } from "lucide-react";
import { SubscriptionList } from "@/features/subscription/components/subscription-list";
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
    <main className="mx-auto w-full max-w-6xl px-4 pb-12 pt-7 text-foreground sm:px-6 sm:pt-10 lg:px-8">
      <header className="mb-6">
        <h1 className="text-2xl font-semibold leading-normal sm:text-3xl">알림 설정</h1>
        <p className="mt-1.5 break-keep text-sm leading-6 text-muted-foreground">빈자리, 문의 답변, 중요 공지 알림을 받을 채널을 설정하세요.</p>
      </header>
      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <form
          aria-labelledby="notification-channels-heading"
          onSubmit={handleSubmit(onSubmit)}
          className="min-w-0 rounded-xl border bg-card"
        >
          <header className="flex items-center gap-2 rounded-t-xl border-b bg-muted/40 px-4 py-3.5 sm:px-5">
            <Bell aria-hidden="true" className="size-4 text-primary" />
            <h2 id="notification-channels-heading" className="text-sm font-semibold">알림 채널</h2>
          </header>
          <div className="divide-y">
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
        {user && (
          <section aria-labelledby="course-subscriptions-heading" className="min-w-0 rounded-xl border bg-card">
            <header className="flex items-center gap-2 rounded-t-xl border-b bg-muted/40 px-4 py-3.5">
              <Bookmark aria-hidden="true" className="size-4 text-primary" />
              <h2 id="course-subscriptions-heading" className="text-sm font-semibold">구독 중인 강의</h2>
            </header>
            <SubscriptionList user={user} />
          </section>
        )}
      </div>
    </main>
  );
}
