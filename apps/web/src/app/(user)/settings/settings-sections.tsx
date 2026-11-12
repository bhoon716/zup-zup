"use client";

import { BellRing, Laptop, Loader2, Mail, MessageSquare, Monitor, Plus, Save, X } from "lucide-react";
import type { ReactNode } from "react";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import type { SettingsPageModel } from "./useSettingsPage";

type SettingsActionBarProps = Pick<SettingsPageModel, "isSubmitting"> &
  Partial<Pick<SettingsPageModel, "isSendingTest" | "testCooldownSeconds" | "handleSendTestNotification">> & {
    onCancel: () => void;
  };

const channelIcons = {
  discord: { Icon: MessageSquare, className: "bg-indigo-100 text-indigo-700 dark:bg-indigo-400/15 dark:text-indigo-300" },
  email: { Icon: Mail, className: "bg-primary/10 text-primary" },
  web: { Icon: Laptop, className: "bg-sky-100 text-sky-800 dark:bg-sky-400/15 dark:text-sky-300" },
};

function NotificationChannel({
  id,
  title,
  description,
  toggleLabel,
  checked,
  onCheckedChange,
  disabled,
  children,
}: {
  id: keyof typeof channelIcons;
  title: string;
  description: string;
  toggleLabel: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  children: ReactNode;
}) {
  const { Icon, className } = channelIcons[id];

  return (
    <section aria-labelledby={`${id}-heading`} className="rounded-xl border bg-card px-4 py-4 sm:px-5">
      <header className="grid grid-cols-[auto_minmax(0,1fr)_auto] gap-x-3 gap-y-1">
        <span aria-hidden="true" className={cn("grid size-10 place-items-center rounded-lg sm:row-span-2", className)}>
          <Icon className="size-5" />
        </span>
        <h3 id={`${id}-heading`} className="self-center font-semibold">{title}</h3>
        <div className="flex items-center gap-2 self-center">
          <Label htmlFor={`${id}-enabled`} className="sr-only">{toggleLabel}</Label>
          <span aria-hidden="true" className="hidden text-xs text-muted-foreground sm:inline">
            {checked ? "켜짐" : "꺼짐"}
          </span>
          <Switch
            id={`${id}-enabled`}
            checked={checked}
            onCheckedChange={onCheckedChange}
            disabled={disabled}
            aria-describedby={`${id}-description`}
            className="data-[size=default]:h-5 data-[size=default]:w-9 [&_[data-slot=switch-thumb][data-state=checked]]:translate-x-full"
          />
        </div>
        <p id={`${id}-description`} className="col-span-3 break-keep text-sm leading-5 text-muted-foreground sm:col-span-2 sm:col-start-2">
          {description}
        </p>
      </header>
      <div className="mt-3.5 space-y-3 border-t pt-3.5 sm:ml-[3.25rem]">{children}</div>
    </section>
  );
}

export function DiscordIntegrationSection({
  user,
  watch,
  setValue,
  isUnlinking,
  handleDiscordConnect,
  handleDiscordUnlink,
}: Pick<
  SettingsPageModel,
  "user" | "watch" | "setValue" | "isUnlinking" | "handleDiscordConnect" | "handleDiscordUnlink"
>) {
  const discordId = user?.discordId;

  return (
    <NotificationChannel
      id="discord"
      title="Discord 연동"
      description="연결한 Discord 계정의 DM으로 알림을 받습니다."
      toggleLabel="DM 알림 활성화"
      checked={watch("discordEnabled")}
      onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
    >
      <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 rounded-lg bg-muted/65 px-3 py-2.5">
        {discordId ? (
          <>
            <div className="min-w-0">
              <p className="text-xs font-medium text-primary">연동됨</p>
              <p className="mt-0.5 wrap-anywhere font-mono text-xs text-muted-foreground">{discordId}</p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDiscordUnlink}
              disabled={isUnlinking}
              className="rounded-md border-input bg-card text-destructive hover:text-destructive"
            >
              {isUnlinking ? "해제 중…" : "연동 해제"}
            </Button>
          </>
        ) : (
          <>
            <p className="text-sm text-muted-foreground">연동된 계정 없음</p>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDiscordConnect}
              className="rounded-md border-primary/20 bg-card text-primary"
            >
              Discord 계정 연결하기
            </Button>
          </>
        )}
      </div>
    </NotificationChannel>
  );
}

export function EmailNotificationSection({
  user,
  authCode,
  setAuthCode,
  verified,
  emailSent,
  sending,
  verifying,
  register,
  watch,
  setValue,
  errors,
  onSendCode,
  onVerifyCode,
  isGoogleEmail,
  needsVerification,
}: Pick<
  SettingsPageModel,
  | "user"
  | "authCode"
  | "setAuthCode"
  | "verified"
  | "emailSent"
  | "sending"
  | "verifying"
  | "register"
  | "watch"
  | "setValue"
  | "errors"
  | "onSendCode"
  | "onVerifyCode"
  | "isGoogleEmail"
  | "needsVerification"
>) {
  const emailError = errors.notificationEmail?.message;

  return (
    <NotificationChannel
      id="email"
      title="이메일 알림"
      description="수신 주소를 변경하면 이메일 인증이 필요합니다."
      toggleLabel="이메일 알림 활성화"
      checked={watch("emailEnabled")}
      onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
    >
      <div className="space-y-1.5">
        <div className="flex items-center justify-between gap-2">
          <Label htmlFor="notification-email" className="text-xs text-muted-foreground">수신 이메일</Label>
          {(isGoogleEmail || verified) && (
            <span className="text-xs font-medium text-primary">인증됨</span>
          )}
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Input
            {...register("notificationEmail")}
            id="notification-email"
            autoComplete="email"
            inputMode="email"
            placeholder={user?.email}
            aria-invalid={!!emailError}
            aria-describedby={emailError ? "notification-email-error" : undefined}
            className="h-10 rounded-lg border-input bg-card shadow-none sm:flex-1"
          />
          {needsVerification && !verified && (
            <Button
              type="button"
              variant="outline"
              onClick={onSendCode}
              disabled={sending || !!emailError || !watch("notificationEmail")}
              className="h-10 rounded-lg border-input bg-card"
            >
              {sending ? "전송 중…" : "인증 코드 보내기"}
            </Button>
          )}
        </div>
        {emailError && (
          <p id="notification-email-error" className="text-xs text-destructive">{emailError}</p>
        )}
      </div>
      {needsVerification && !verified && emailSent && (
        <div className="space-y-1.5">
          <Label htmlFor="notification-email-code" className="text-xs text-muted-foreground">인증 코드</Label>
          <div className="flex gap-2">
            <Input
              id="notification-email-code"
              placeholder="인증 코드 6자리"
              autoComplete="one-time-code"
              inputMode="numeric"
              value={authCode}
              onChange={(event) => setAuthCode(event.target.value)}
              maxLength={6}
              className="h-10 flex-1 rounded-lg border-input bg-card shadow-none"
            />
            <Button
              type="button"
              variant="outline"
              onClick={onVerifyCode}
              disabled={verifying || authCode.length !== 6}
              className="h-10 rounded-lg border-input bg-card"
            >
              {verifying ? "확인 중…" : "확인"}
            </Button>
          </div>
        </div>
      )}
    </NotificationChannel>
  );
}

export function WebPushSection({
  deviceAlias,
  setDeviceAlias,
  devices,
  loadingWebPush,
  watch,
  setValue,
  handleRegisterDevice,
  handleDeleteDevice,
}: Pick<
  SettingsPageModel,
  | "deviceAlias"
  | "setDeviceAlias"
  | "devices"
  | "loadingWebPush"
  | "watch"
  | "setValue"
  | "handleRegisterDevice"
  | "handleDeleteDevice"
>) {
  return (
    <NotificationChannel
      id="web"
      title="웹 푸시 알림"
      description="등록한 모든 기기의 브라우저 알림을 켜거나 끕니다."
      toggleLabel="웹 푸시 알림 활성화"
      checked={watch("webPushEnabled")}
      onCheckedChange={(checked) => setValue("webPushEnabled", checked, { shouldDirty: true })}
      disabled={loadingWebPush}
    >
      <div className="space-y-1.5">
        <Label htmlFor="web-push-device-alias" className="text-xs text-muted-foreground">기기 별칭</Label>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Input
            id="web-push-device-alias"
            value={deviceAlias}
            onChange={(event) => setDeviceAlias(event.target.value)}
            placeholder="예: 내 노트북"
            className="h-10 rounded-lg border-input bg-card shadow-none sm:flex-1"
          />
          <Button
            type="button"
            variant="outline"
            onClick={handleRegisterDevice}
            disabled={loadingWebPush}
            className="h-10 rounded-lg border-input bg-card"
          >
            {loadingWebPush ? <Loader2 aria-hidden="true" className="animate-spin" /> : <Plus aria-hidden="true" />}
            {loadingWebPush ? "등록 중…" : "현재 기기 등록"}
          </Button>
        </div>
      </div>
      <div className="overflow-hidden rounded-lg border">
        <h4 className="bg-muted/65 px-3 py-2 text-xs font-medium text-muted-foreground">내 계정에 등록된 기기</h4>
        {devices.length === 0 ? (
          <p className="border-t px-3 py-3 text-sm text-muted-foreground">등록된 기기가 없습니다.</p>
        ) : (
          <ul className="divide-y border-t">
            {devices.map((device) => {
              const alias = device.alias || "알 수 없는 기기";

              return (
                <li key={device.id} className="flex items-center gap-3 px-3 py-2">
                  <Monitor aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
                  <div className="min-w-0 flex-1">
                    <p className="wrap-anywhere text-sm font-medium">{alias}</p>
                    <p className="text-xs text-muted-foreground">{new Date(device.registeredAt).toLocaleDateString("ko-KR")} 등록</p>
                  </div>
                  <Button
                    type="button"
                    aria-label={`${alias} 삭제`}
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => handleDeleteDevice(device.id)}
                    className="rounded-md text-muted-foreground hover:text-destructive"
                  >
                    <X aria-hidden="true" />
                  </Button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </NotificationChannel>
  );
}

export function SettingsActionBar({
  isSubmitting,
  onCancel,
  isSendingTest = false,
  testCooldownSeconds = 0,
  handleSendTestNotification,
}: SettingsActionBarProps) {
  let testLabel = "알림 테스트";
  if (isSendingTest) {
    testLabel = "전송 중…";
  } else if (testCooldownSeconds > 0) {
    testLabel = `다시 테스트까지 ${testCooldownSeconds}초`;
  }

  return (
    <div className="mt-3 flex flex-col gap-3 rounded-xl border bg-card px-4 py-4 sm:flex-row sm:items-end sm:justify-between sm:px-5">
      {handleSendTestNotification && (
        <div className="space-y-1.5">
          <p id="notification-test-description" className="text-xs text-muted-foreground">저장된 설정으로 테스트합니다.</p>
          <Button
            type="button"
            variant="outline"
            onClick={handleSendTestNotification}
            disabled={isSendingTest || testCooldownSeconds > 0}
            aria-describedby="notification-test-description"
            className="h-10 w-full rounded-lg border-input bg-card sm:w-auto"
          >
            {isSendingTest ? <Loader2 aria-hidden="true" className="animate-spin" /> : <BellRing aria-hidden="true" />}
            {testLabel}
          </Button>
        </div>
      )}
      <div className="flex items-center justify-end gap-2 sm:ml-auto">
        <Button type="button" variant="ghost" onClick={onCancel} className="h-10 rounded-lg text-muted-foreground">
          변경 취소
        </Button>
        <Button type="submit" disabled={isSubmitting} className="h-10 rounded-lg font-semibold">
          {isSubmitting ? <Loader2 aria-hidden="true" className="animate-spin" /> : <Save aria-hidden="true" />}
          {isSubmitting ? "저장 중…" : "설정 저장하기"}
        </Button>
      </div>
    </div>
  );
}
