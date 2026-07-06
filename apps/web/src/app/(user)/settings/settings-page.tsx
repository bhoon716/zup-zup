"use client";

import { Loader2 } from "lucide-react";
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
    <div className="min-h-screen bg-white">
      <main className="mx-auto max-w-4xl px-6 pb-32 pt-10 sm:px-10 md:pb-40 md:pt-16">
        <div className="mb-12 border-b border-slate-100 pb-8">
          <h1 className="mb-2 text-3xl font-bold tracking-tight text-slate-900">알림 설정</h1>
          <p className="font-medium text-slate-500">빈 좌석 알림을 받을 채널을 설정하고 관리하세요.</p>
        </div>
        <form onSubmit={handleSubmit(onSubmit)}>
          <section className="space-y-12">
            <div className="space-y-6">
              <div className="mb-8 flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-bold text-slate-900">알림 채널 연동</h2>
                  <p className="mt-2 text-sm font-medium text-slate-500">여러 채널을 연동하여 확실하게 알림을 받아보세요.</p>
                </div>
                <p className="text-xs text-slate-400">저장된 설정 기준으로 전송됩니다.</p>
              </div>

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
                isSendingTest={isSendingTest}
                testCooldownSeconds={testCooldownSeconds}
                loadingWebPush={loadingWebPush}
                watch={watch}
                setValue={setValue}
                handleSendTestNotification={handleSendTestNotification}
                handleRegisterDevice={handleRegisterDevice}
                handleDeleteDevice={handleDeleteDevice}
              />
            </div>
          </section>
          <SettingsActionBar isSubmitting={isSubmitting} onCancel={() => router.back()} />
        </form>
      </main>
    </div>
  );
}
