"use client";

import { Loader2, X } from "lucide-react";
import type { ReactNode } from "react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import type { SettingsPageModel } from "./useSettingsPage";

type SettingsActionBarProps = Pick<SettingsPageModel, "isSubmitting"> &
  Partial<Pick<SettingsPageModel, "isSendingTest" | "testCooldownSeconds" | "handleSendTestNotification">> & {
    onCancel: () => void;
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
  id: "discord" | "email" | "web";
  title: string;
  description: string;
  toggleLabel: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
  children: ReactNode;
}) {
  return (
    <section aria-labelledby={`${id}-heading`} className="p-4 sm:px-5">
      <header className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 id={`${id}-heading`} className="font-semibold">{title}</h3>
          <p id={`${id}-description`} className="mt-1 break-keep text-sm text-muted-foreground">
            {description}
          </p>
        </div>
        <Label htmlFor={`${id}-enabled`} className="sr-only">{toggleLabel}</Label>
        <Switch
          id={`${id}-enabled`}
          checked={checked}
          onCheckedChange={onCheckedChange}
          disabled={disabled}
          aria-describedby={`${id}-description`}
          className="mt-0.5 data-[size=default]:h-5 data-[size=default]:w-9 [&_[data-slot=switch-thumb][data-state=checked]]:translate-x-full"
        />
      </header>
      <div className="mt-4 space-y-3">{children}</div>
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
      description="연결한 계정의 DM으로 알림을 받습니다."
      toggleLabel="DM 알림 활성화"
      checked={watch("discordEnabled")}
      onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        {discordId ? (
          <>
            <div className="min-w-0">
              <p className="text-sm font-medium">연동됨</p>
              <p className="mt-0.5 wrap-anywhere text-sm text-muted-foreground">{discordId}</p>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleDiscordUnlink}
              disabled={isUnlinking}
              className="text-destructive hover:text-destructive"
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
          <Label htmlFor="notification-email" className="text-sm">수신 이메일</Label>
          {(isGoogleEmail || verified) && (
            <span className="text-xs text-muted-foreground">인증됨</span>
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
            className="h-10 sm:flex-1"
          />
          {needsVerification && !verified && (
            <Button
              type="button"
              variant="outline"
              onClick={onSendCode}
              disabled={sending || !!emailError || !watch("notificationEmail")}
              className="h-10"
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
          <Label htmlFor="notification-email-code" className="text-sm">인증 코드</Label>
          <div className="flex gap-2">
            <Input
              id="notification-email-code"
              placeholder="인증 코드 6자리"
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
      description="등록한 모든 기기의 브라우저에 알림을 보냅니다."
      toggleLabel="웹 푸시 알림 활성화"
      checked={watch("webPushEnabled")}
      onCheckedChange={(checked) => setValue("webPushEnabled", checked, { shouldDirty: true })}
      disabled={loadingWebPush}
    >
      <div className="space-y-1.5">
        <Label htmlFor="web-push-device-alias" className="text-sm">기기 별칭</Label>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Input
            id="web-push-device-alias"
            value={deviceAlias}
            onChange={(event) => setDeviceAlias(event.target.value)}
            placeholder="예: 내 노트북"
            className="h-10 sm:flex-1"
          />
          <Button
            type="button"
            variant="outline"
            onClick={handleRegisterDevice}
            disabled={loadingWebPush}
            className="h-10"
          >
            {loadingWebPush && <Loader2 aria-hidden="true" className="animate-spin" />}
            {loadingWebPush ? "등록 중…" : "현재 기기 등록"}
          </Button>
        </div>
      </div>
      <div className="border-t pt-3">
        <h4 className="text-sm font-medium">내 계정에 등록된 기기</h4>
        {devices.length === 0 ? (
          <p className="mt-1 text-sm text-muted-foreground">등록된 기기가 없습니다.</p>
        ) : (
          <ul className="mt-1 divide-y">
            {devices.map((device) => {
              const alias = device.alias || "알 수 없는 기기";

              return (
                <li key={device.id} className="flex items-center gap-3 py-2">
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
                    className="text-muted-foreground hover:text-destructive"
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
    <div className="mt-4 flex flex-wrap items-end justify-between gap-x-4 gap-y-3">
      {handleSendTestNotification && (
        <div className="space-y-1.5">
          <p id="notification-test-description" className="text-xs text-muted-foreground">저장된 설정으로 테스트합니다.</p>
          <Button
            type="button"
            variant="outline"
            onClick={handleSendTestNotification}
            disabled={isSendingTest || testCooldownSeconds > 0}
            aria-describedby="notification-test-description"
            className="h-10"
          >
            {isSendingTest && <Loader2 aria-hidden="true" className="animate-spin" />}
            {testLabel}
          </Button>
        </div>
      )}
      <div className="ml-auto flex items-center gap-2">
        <Button type="button" variant="ghost" onClick={onCancel} className="h-10 text-muted-foreground">
          변경 취소
        </Button>
        <Button type="submit" disabled={isSubmitting} className="h-10">
          {isSubmitting && <Loader2 aria-hidden="true" className="animate-spin" />}
          {isSubmitting ? "저장 중…" : "설정 저장하기"}
        </Button>
      </div>
    </div>
  );
}
