"use client";

import { Loader2 } from "lucide-react";
import { NotificationSettingsLayout } from "@/features/user/ui/notification-settings-layout";
import type { SettingsPageModel } from "./useSettingsPage";
import {
  DiscordIntegrationSection,
  EmailNotificationSection,
  SettingsActionBar,
  WebPushSection,
} from "./settings-sections";

export function SettingsPage({ model }: { model: SettingsPageModel }) {
  const { user, loading, isSubmitting, isDirty, handleSubmit, onSubmit, cancelChanges } = model;

  if (loading) {
    return (
      <div className="flex h-[60vh] items-center justify-center" role="status">
        <Loader2 aria-hidden="true" className="size-8 animate-spin text-primary" />
        <span className="sr-only">알림 설정을 불러오는 중</span>
      </div>
    );
  }

  return (
    <NotificationSettingsLayout description="빈자리, 문의 답변, 중요 공지를 받을 채널을 관리하세요.">
      {!user && (
        <p role="alert" className="mt-4 text-sm text-destructive">
          프로필 정보를 확인한 뒤 다시 시도해 주세요.
        </p>
      )}
      <form aria-label="알림 채널" noValidate onSubmit={handleSubmit(onSubmit)} className="mt-5">
        <fieldset disabled={!user || isSubmitting} className="grid min-w-0 gap-2.5">
          <DiscordIntegrationSection {...model} />
          <EmailNotificationSection {...model} />
          <WebPushSection {...model} />
        </fieldset>
        <SettingsActionBar
          isSubmitting={isSubmitting}
          isDirty={isDirty}
          onCancel={cancelChanges}
          isSendingTest={model.isSendingTest}
          testCooldownSeconds={model.testCooldownSeconds}
          handleSendTestNotification={user ? model.handleSendTestNotification : undefined}
        />
      </form>
    </NotificationSettingsLayout>
  );
}
