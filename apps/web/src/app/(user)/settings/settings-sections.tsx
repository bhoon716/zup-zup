"use client";

import { AppWindow, Bell, Check, Loader2, Mail, MessageSquare, Monitor, X } from "lucide-react";
import { useState } from "react";
import {
  NotificationActions,
  NotificationChannel,
  NotificationDeviceAlias,
  NotificationEmailStatus,
  notificationButtonClassName,
  notificationDiscordButtonClassName,
  notificationInputClassName,
} from "@/features/user/ui/notification-settings-layout";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import type { SettingsPageModel } from "./useSettingsPage";

const noteClassName = "mx-0.5 mt-[5px] wrap-anywhere text-xs leading-normal text-muted-foreground";

export function DiscordIntegrationSection({
  user,
  discordEnabled,
  setValue,
  isUnlinking,
  handleDiscordConnect,
  handleDiscordUnlink,
}: Pick<
  SettingsPageModel,
  "user" | "discordEnabled" | "setValue" | "isUnlinking" | "handleDiscordConnect" | "handleDiscordUnlink"
>) {
  const linked = Boolean(user?.discordId);

  return (
    <NotificationChannel
      id="discord"
      title="디스코드 DM 알림"
      description="디스코드 개인 메시지로 받아요."
      icon={MessageSquare}
      iconClassName="bg-indigo-50 text-indigo-600"
      toggle={
        <Switch
          aria-label="디스코드 DM 알림"
          checked={linked && discordEnabled}
          onCheckedChange={(checked) => setValue("discordEnabled", checked)}
          disabled={!linked || isUnlinking}
        />
      }
    >
      <div className="flex flex-wrap items-center justify-end gap-2.5">
        <p aria-live="polite" className={cn("text-xs", linked ? "font-semibold text-[#067347] dark:text-emerald-400" : "text-muted-foreground")}>
          {linked ? "연동됨" : "연동 안 됨"}
        </p>
        {linked ? (
          <Button
            type="button"
            variant="outline"
            onClick={handleDiscordUnlink}
            disabled={isUnlinking}
            className={notificationButtonClassName}
          >
            {isUnlinking ? "해제 중…" : "연동 해제"}
          </Button>
        ) : (
          <Button
            type="button"
            onClick={handleDiscordConnect}
            className={cn(notificationButtonClassName, notificationDiscordButtonClassName)}
          >
            디스코드 계정 연동
          </Button>
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
  notificationEmail,
  emailEnabled,
  setValue,
  errors,
  onSendCode,
  onVerifyCode,
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
  | "notificationEmail"
  | "emailEnabled"
  | "setValue"
  | "errors"
  | "onSendCode"
  | "onVerifyCode"
>) {
  const emailError = errors.notificationEmail?.message;

  return (
    <NotificationChannel
      id="email"
      title="이메일 알림"
      description="인증한 주소로 알림을 받아요."
      icon={Mail}
      iconClassName="bg-purple-50 text-[#56296e]"
      toggle={
        <Switch
          aria-label="이메일 알림"
          checked={emailEnabled}
          onCheckedChange={(checked) => setValue("emailEnabled", checked)}
        />
      }
    >
      <div className="flex items-center gap-1.5">
        <div className="relative min-w-0 flex-1">
          <Input
            {...register("notificationEmail", { onChange: () => setAuthCode("") })}
            id="notification-email"
            type="email"
            autoComplete="email"
            placeholder={user?.email}
            aria-label="수신 이메일"
            aria-invalid={Boolean(emailError)}
            aria-describedby={emailError ? "notification-email-error" : "notification-email-note"}
            className={cn(notificationInputClassName, "w-full pr-[34px]")}
          />
          <NotificationEmailStatus verified={verified} />
        </div>
        {!verified && (
          <Button
            type="button"
            variant="outline"
            onClick={onSendCode}
            disabled={sending || verifying || !notificationEmail.trim()}
            className={notificationButtonClassName}
          >
            {sending ? "전송 중…" : emailSent ? "다시 보내기" : "인증하기"}
          </Button>
        )}
      </div>
      {emailError ? (
        <p id="notification-email-error" role="alert" className={cn(noteClassName, "text-destructive")}>{emailError}</p>
      ) : (
        <p id="notification-email-note" className={noteClassName}>다른 이메일을 쓰려면 인증해 주세요.</p>
      )}
      {!verified && emailSent && (
        <div className="mt-2.5">
          <Label htmlFor="notification-email-code" className="mb-1.5 text-xs text-muted-foreground">인증 코드</Label>
          <div className="flex items-center gap-1.5">
            <Input
              id="notification-email-code"
              placeholder="인증 코드 6자리"
              autoComplete="one-time-code"
              inputMode="numeric"
              value={authCode}
              onChange={(event) => setAuthCode(event.target.value)}
              maxLength={6}
              className={cn(notificationInputClassName, "flex-1")}
            />
            <Button
              type="button"
              variant="outline"
              onClick={onVerifyCode}
              disabled={sending || verifying || authCode.length !== 6}
              className={notificationButtonClassName}
            >
              {verifying ? "확인 중…" : "확인"}
            </Button>
          </div>
        </div>
      )}
    </NotificationChannel>
  );
}

const deviceAliasId = "web-push-device-alias";

export function WebPushSection({
  deviceAlias,
  setDeviceAlias,
  devices,
  loadingWebPush,
  webPushEnabled,
  setValue,
  handleRegisterDevice,
  handleDeleteDevice,
}: Pick<
  SettingsPageModel,
  | "deviceAlias"
  | "setDeviceAlias"
  | "devices"
  | "loadingWebPush"
  | "webPushEnabled"
  | "setValue"
  | "handleRegisterDevice"
  | "handleDeleteDevice"
>) {
  const hasDevices = devices.length > 0;
  const [aliasRequired, setAliasRequired] = useState(false);

  const registerDevice = () => {
    if (!deviceAlias.trim()) {
      setAliasRequired(true);
      document.getElementById(deviceAliasId)?.focus();
      return;
    }
    void handleRegisterDevice();
  };

  return (
    <NotificationChannel
      id="web"
      title="웹 푸시 알림"
      description="등록된 기기에서 알림을 받아요."
      icon={AppWindow}
      iconClassName="bg-blue-50 text-blue-600"
      toggle={
        <Switch
          aria-label="웹 푸시 알림"
          checked={webPushEnabled}
          onCheckedChange={(checked) => setValue("webPushEnabled", checked)}
          disabled={(!hasDevices && !webPushEnabled) || loadingWebPush}
        />
      }
    >
      <NotificationDeviceAlias
        id={deviceAliasId}
        value={deviceAlias}
        onChange={(value) => {
          setAliasRequired(false);
          setDeviceAlias(value);
        }}
        onRegister={registerDevice}
        buttonLabel={loadingWebPush ? "등록 중…" : "현재 기기 등록"}
        disabled={loadingWebPush}
        invalid={aliasRequired}
      />
      {aliasRequired && (
        <p id={`${deviceAliasId}-error`} role="alert" className={cn(noteClassName, "text-destructive")}>
          기기 별칭을 입력해 주세요.
        </p>
      )}
      <div className="mt-2.5">
        <h3 className="mb-1.5 ml-0.5 text-xs font-semibold text-muted-foreground">등록된 기기</h3>
        {hasDevices ? (
          <ul className="divide-y divide-[#ebedf0] rounded-xl border border-[#e4e5ea] bg-card px-[9px] dark:divide-border dark:border-border">
            {devices.map((device) => {
              const alias = device.alias || "알 수 없는 기기";

              return (
                <li key={device.id} className="grid grid-cols-[16px_minmax(0,1fr)_auto] items-center gap-2 py-1.5 text-muted-foreground">
                  <Monitor aria-hidden="true" className="size-4" />
                  <div className="min-w-0">
                    <p className="wrap-anywhere text-[13px] font-semibold text-[#101828] dark:text-foreground">{alias}</p>
                    <p className="mt-px text-[11px]">{new Date(device.registeredAt).toLocaleDateString("ko-KR")} 등록</p>
                  </div>
                  <Button
                    type="button"
                    aria-label={`${alias} 등록 해제`}
                    variant="ghost"
                    size="icon-sm"
                    onClick={() => handleDeleteDevice(device.id)}
                    className="rounded-lg"
                  >
                    <X aria-hidden="true" />
                  </Button>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className={noteClassName}>등록된 기기가 없어요.</p>
        )}
      </div>
    </NotificationChannel>
  );
}

type SettingsActionBarProps = Pick<SettingsPageModel, "isSubmitting" | "isDirty"> &
  Partial<Pick<SettingsPageModel, "isSendingTest" | "testCooldownSeconds" | "handleSendTestNotification">> & {
    onCancel: () => void;
  };

export function SettingsActionBar({
  isSubmitting,
  isDirty,
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
  const actionsDisabled = !isDirty || isSubmitting;
  const actionButtonClassName = cn(notificationButtonClassName, "max-[420px]:px-2 max-[420px]:has-[>svg]:px-2");

  return (
    <NotificationActions>
      {handleSendTestNotification && (
        <>
          <span id="notification-test-description" className="sr-only">저장된 설정으로 테스트합니다.</span>
          <Button
            type="button"
            variant="outline"
            onClick={handleSendTestNotification}
            disabled={isSendingTest || testCooldownSeconds > 0}
            aria-describedby="notification-test-description"
            className={actionButtonClassName}
          >
            {isSendingTest ? <Loader2 aria-hidden="true" className="size-[15px] animate-spin" /> : <Bell aria-hidden="true" className="size-[15px]" />}
            {testLabel}
          </Button>
        </>
      )}
      <Button
        type="button"
        variant="ghost"
        disabled={actionsDisabled}
        onClick={onCancel}
      className={cn(actionButtonClassName, "text-muted-foreground")}
      >
        변경 취소
      </Button>
      <Button
        type="submit"
        disabled={actionsDisabled}
      className={cn(actionButtonClassName, "min-w-[95px] disabled:opacity-[.68] max-[420px]:min-w-0")}
      >
        {isSubmitting ? <Loader2 aria-hidden="true" className="size-[15px] animate-spin" /> : <Check aria-hidden="true" className="size-[15px]" />}
        {isSubmitting ? "저장 중…" : "변경 저장"}
      </Button>
    </NotificationActions>
  );
}
