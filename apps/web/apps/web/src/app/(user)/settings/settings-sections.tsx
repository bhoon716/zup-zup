"use client";

import { AnimatePresence, motion } from "framer-motion";
import {
  AlertCircle,
  CheckCircle,
  Laptop,
  Mail,
  MessageSquare,
  Monitor,
  Loader2,
  Save,
  Smartphone,
  X,
} from "lucide-react";
import type { ReactNode } from "react";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { cn } from "@/shared/lib/utils";
import type { SettingsPageModel } from "./useSettingsPage";

type SettingsSectionProps = {
  user: SettingsPageModel["user"];
  authCode: SettingsPageModel["authCode"];
  setAuthCode: SettingsPageModel["setAuthCode"];
  verified: SettingsPageModel["verified"];
  emailSent: SettingsPageModel["emailSent"];
  sending: SettingsPageModel["sending"];
  verifying: SettingsPageModel["verifying"];
  isUnlinking: SettingsPageModel["isUnlinking"];
  deviceAlias: SettingsPageModel["deviceAlias"];
  setDeviceAlias: SettingsPageModel["setDeviceAlias"];
  devices: SettingsPageModel["devices"];
  isSendingTest: SettingsPageModel["isSendingTest"];
  testCooldownSeconds: SettingsPageModel["testCooldownSeconds"];
  loadingWebPush: SettingsPageModel["loadingWebPush"];
  register: SettingsPageModel["register"];
  watch: SettingsPageModel["watch"];
  setValue: SettingsPageModel["setValue"];
  errors: SettingsPageModel["errors"];
  onSendCode: SettingsPageModel["onSendCode"];
  onVerifyCode: SettingsPageModel["onVerifyCode"];
  handleSendTestNotification: SettingsPageModel["handleSendTestNotification"];
  handleDiscordConnect: SettingsPageModel["handleDiscordConnect"];
  handleDiscordUnlink: SettingsPageModel["handleDiscordUnlink"];
  handleRegisterDevice: SettingsPageModel["handleRegisterDevice"];
  handleDeleteDevice: SettingsPageModel["handleDeleteDevice"];
  isGoogleEmail: SettingsPageModel["isGoogleEmail"];
  needsVerification: SettingsPageModel["needsVerification"];
};

type SettingsActionBarProps = {
  isSubmitting: SettingsPageModel["isSubmitting"];
  onCancel: () => void;
};

function SectionCard({
  icon,
  title,
  description,
  action,
  children,
}: {
  icon: ReactNode;
  title: ReactNode;
  description: ReactNode;
  action?: ReactNode;
  children: ReactNode;
}) {
  return (
    <motion.div whileHover={{ y: -2 }} className="rounded-[2rem] border border-slate-100 bg-slate-50 p-8 transition-all hover:border-primary/30 hover:shadow-md">
      <div className="flex items-start gap-6">
        {icon}
        <div className="w-full flex-1">
          <div className="mb-1 flex items-center justify-between gap-4">
            <h3 className="text-lg font-bold text-slate-900">{title}</h3>
            {action}
          </div>
          <p className="mb-5 text-sm font-medium text-slate-500">{description}</p>
          {children}
        </div>
      </div>
    </motion.div>
  );
}

function SettingsSwitch({
  checked,
  onCheckedChange,
  disabled,
}: {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
}) {
  const handleToggle = () => {
    if (!disabled) {
      onCheckedChange(!checked);
    }
  };

  return (
    <div
      className={cn(
        "relative inline-flex h-7 w-13 shrink-0 cursor-pointer items-center rounded-full transition-colors duration-300 ease-in-out focus-visible:outline-hidden",
        checked ? "bg-primary" : "bg-slate-200",
        disabled && "cursor-not-allowed opacity-50"
      )}
      onClick={handleToggle}
    >
      <span
        aria-hidden="true"
        className={cn(
          "pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-soft transition duration-300 ease-in-out",
          checked ? "translate-x-7" : "translate-x-1"
        )}
      />
    </div>
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
  SettingsSectionProps,
  "user" | "watch" | "setValue" | "isUnlinking" | "handleDiscordConnect" | "handleDiscordUnlink"
>) {
  return (
    <SectionCard
      icon={
        <div className="hidden h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-[#5865F2] text-white shadow-lg shadow-[#5865F2]/20 sm:flex">
          <MessageSquare className="h-8 w-8" />
        </div>
      }
      title="Discord 연동"
      description="디스코드 봇이 개인 DM으로 알림을 즉시 보내드립니다."
      action={
        user?.discordId ? (
          <Badge variant="outline" className="rounded-lg border-indigo-100 bg-indigo-50 px-2 py-0.5 text-[10px] font-bold text-indigo-600">
            연동됨
          </Badge>
        ) : null
      }
    >
      {user?.discordId ? (
        <div className="flex flex-col gap-3 sm:flex-row">
          <div className="flex flex-1 items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3">
            <span className="font-mono text-sm font-bold text-slate-700">{user.discordId}</span>
            <CheckCircle className="h-4 w-4 text-emerald-500" />
          </div>
          <Button
            type="button"
            variant="destructive"
            onClick={handleDiscordUnlink}
            disabled={isUnlinking}
            className="h-12 rounded-xl px-6 font-bold shadow-soft transition-all active:scale-95"
          >
            {isUnlinking ? <Loader2 className="h-4 w-4 animate-spin" /> : "연동 해제"}
          </Button>
        </div>
      ) : (
        <Button
          type="button"
          onClick={handleDiscordConnect}
          className="flex h-13 w-full items-center justify-center gap-3 rounded-xl bg-[#5865F2] font-bold text-white shadow-lg shadow-[#5865F2]/20 transition-all active:scale-[0.98] hover:bg-[#4752C4]"
        >
          <MessageSquare className="h-6 w-6" />
          Discord 계정 연결하기
        </Button>
      )}

      <div className="mt-4 flex items-center justify-between border-t border-slate-200/50 pt-4">
        <Label htmlFor="discord-enabled" className="text-sm font-bold text-slate-600">
          DM 알림 활성화
        </Label>
        <SettingsSwitch
          checked={watch("discordEnabled")}
          onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
        />
      </div>
    </SectionCard>
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
  SettingsSectionProps,
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
  return (
    <SectionCard
      icon={
        <div className="hidden h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-white shadow-sm sm:flex">
          <Mail className="h-7 w-7 text-slate-400" />
        </div>
      }
      title="이메일 알림"
      description="중요 알림을 이메일로 받아보세요. 네이버 이메일을 권장합니다."
      action={(isGoogleEmail || verified) && <span className="rounded-full bg-emerald-100 px-2.5 py-1 text-[10px] font-bold text-emerald-700">인증됨</span>}
    >
      <div className="space-y-3">
        <div className="relative flex items-center gap-2">
          <Input
            {...register("notificationEmail")}
            placeholder={user?.email}
            className={cn(
              "h-12 w-full rounded-xl border-slate-200 px-4 py-6 text-sm transition-all focus:ring-2 focus:ring-primary",
              verified && !needsVerification && watch("notificationEmail") && "bg-slate-50 font-semibold text-slate-500"
            )}
          />
          {needsVerification && !verified && (
            <Button
              type="button"
              variant="outline"
              onClick={onSendCode}
              disabled={sending || !!errors.notificationEmail || !watch("notificationEmail")}
              className="h-12 shrink-0 rounded-xl border-slate-200 bg-white px-6 font-bold text-slate-600 transition-all active:scale-95 hover:bg-slate-50"
            >
              {sending ? <Loader2 className="h-4 w-4 animate-spin" /> : "인증"}
            </Button>
          )}
          {verified && watch("notificationEmail") && (
            <div className="absolute right-3 rounded-lg border border-primary/10 bg-slate-100/80 px-3 py-1.5 text-[11px] font-bold text-primary backdrop-blur-sm">
              인증완료
            </div>
          )}
        </div>

        <AnimatePresence>
          {needsVerification && !verified && emailSent && (
            <motion.div
              initial={{ opacity: 0, scale: 0.95, height: 0 }}
              animate={{ opacity: 1, scale: 1, height: "auto" }}
              exit={{ opacity: 0, scale: 0.95, height: 0 }}
              className="flex flex-col gap-3 overflow-hidden rounded-[1.5rem] border border-primary/10 bg-primary/5 p-4 sm:flex-row"
            >
              <Input
                placeholder="인증 코드 6자리"
                value={authCode}
                onChange={(e) => setAuthCode(e.target.value)}
                maxLength={6}
                className="h-12 flex-1 rounded-xl border-primary/20 bg-white text-center text-lg font-black tracking-[0.3em]"
              />
              <Button
                type="button"
                onClick={onVerifyCode}
                disabled={verifying || authCode.length !== 6}
                className="h-12 rounded-xl bg-primary px-8 font-bold transition-all active:scale-95"
              >
                {verifying ? <Loader2 className="h-4 w-4 animate-spin" /> : "확인"}
              </Button>
            </motion.div>
          )}
        </AnimatePresence>

        <div className="flex items-center justify-between border-t border-slate-200/50 pt-4">
          <Label htmlFor="email-enabled" className="text-sm font-bold text-slate-600">
            이메일 알림 활성화
          </Label>
          <SettingsSwitch
            checked={watch("emailEnabled")}
            onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
          />
        </div>
      </div>

      <p className="mt-3 flex items-center gap-1.5 text-[11px] font-medium text-slate-400">
        <AlertCircle className="h-3.5 w-3.5" />
        주 선택 이메일이 아닌 경우 인증이 필요합니다.
      </p>
    </SectionCard>
  );
}

export function WebPushSection({
  deviceAlias,
  setDeviceAlias,
  devices,
  isSendingTest,
  testCooldownSeconds,
  loadingWebPush,
  watch,
  setValue,
  handleSendTestNotification,
  handleRegisterDevice,
  handleDeleteDevice,
}: Pick<
  SettingsSectionProps,
  | "deviceAlias"
  | "setDeviceAlias"
  | "devices"
  | "isSendingTest"
  | "testCooldownSeconds"
  | "loadingWebPush"
  | "watch"
  | "setValue"
  | "handleSendTestNotification"
  | "handleRegisterDevice"
  | "handleDeleteDevice"
>) {
  return (
    <SectionCard
      icon={
        <div className="hidden h-14 w-14 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-white shadow-sm sm:flex">
          <Laptop className="h-7 w-7 text-primary" />
        </div>
      }
      title="웹 푸시 알림"
      description="현재 사용 중인 기기를 등록하여 브라우저 알림을 받습니다."
      action={
        <Button
          type="button"
          onClick={handleSendTestNotification}
          disabled={isSendingTest || testCooldownSeconds > 0}
          className="h-11 rounded-xl bg-primary px-5 text-sm font-bold text-white shadow-sm transition-all active:scale-95 hover:bg-primary-hover disabled:bg-slate-300"
        >
          {isSendingTest ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              전송 중...
            </>
          ) : testCooldownSeconds > 0 ? (
            `테스트 쿨타임 ${testCooldownSeconds}s`
          ) : (
            "알림 테스트"
          )}
        </Button>
      }
    >
      <div className="mb-6 rounded-2xl border border-slate-100 bg-white p-4 transition-all hover:border-primary/20">
        <div className="flex flex-col gap-3 sm:flex-row">
          <Input
            value={deviceAlias}
            onChange={(e) => setDeviceAlias(e.target.value)}
            placeholder="기기 별칭 (예: 내 노트북)"
            className="h-12 flex-1 rounded-xl border-transparent bg-slate-50 px-4 shadow-none transition-all focus:bg-white"
          />
          <Button
            type="button"
            onClick={handleRegisterDevice}
            disabled={loadingWebPush}
            className="h-12 rounded-xl bg-primary px-6 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all active:scale-[0.95] disabled:bg-slate-300 hover:bg-primary-hover"
          >
            {loadingWebPush ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                등록 중...
              </>
            ) : (
              "현재 기기 등록"
            )}
          </Button>
        </div>
      </div>

      <div className="space-y-3">
        <h4 className="mb-2 px-1 text-xs font-bold uppercase tracking-wider text-slate-400">등록된 기기 목록</h4>
        {devices.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-200 bg-white/50 py-6 text-center">
            <p className="text-sm font-medium text-slate-400">등록된 기기가 없습니다.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {devices.map((device, idx) => (
              <motion.div
                layout
                key={device.id}
                className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3.5 shadow-sm transition-shadow hover:shadow-md"
              >
                <div className="flex items-center gap-3">
                  <div className="rounded-lg bg-slate-50 p-2">
                    {device.type === "WEB" ? <Monitor className="h-4 w-4 text-slate-400" /> : <Smartphone className="h-4 w-4 text-slate-400" />}
                  </div>
                  <div className="flex flex-col">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-bold text-slate-700">{device.alias || "알 수 없는 기기"}</span>
                      {idx === 0 && <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[9px] font-black tracking-tighter text-primary">THIS</span>}
                    </div>
                    <span className="text-[10px] font-medium text-slate-400">{new Date(device.registeredAt).toLocaleDateString()} 등록</span>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => handleDeleteDevice(device.id)}
                  className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-300 transition-all hover:bg-red-50 hover:text-red-500"
                >
                  <X className="h-4 w-4" />
                </button>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      <div className="mt-6 flex items-center justify-between border-t border-slate-200/50 pt-5">
        <div className="space-y-1">
          <Label htmlFor="web-enabled" className="text-sm font-bold text-slate-600">
            브라우저 전체 알림
          </Label>
          <p className="text-[11px] font-medium text-slate-400">기기 개별 설정이 아닌 서비스 전체 알림 스위치입니다.</p>
        </div>
        <SettingsSwitch
          checked={watch("webPushEnabled")}
          onCheckedChange={(checked) => {
            setValue("webPushEnabled", checked, { shouldDirty: true });
          }}
          disabled={loadingWebPush}
        />
      </div>
    </SectionCard>
  );
}

export function SettingsActionBar({ isSubmitting, onCancel }: SettingsActionBarProps) {
  return (
    <motion.div
      initial={{ y: 100 }}
      animate={{ y: 0 }}
      transition={{ duration: 0.5, type: "spring", damping: 20 }}
      className="fixed bottom-0 left-0 right-0 z-40 flex items-center justify-center gap-4 border-t border-slate-100 bg-white/90 px-8 py-5 shadow-[0_-8px_30px_rgba(0,0,0,0.04)] backdrop-blur-md sm:justify-end"
    >
      <Button
        type="button"
        variant="ghost"
        onClick={onCancel}
        className="h-12 rounded-full px-8 text-sm font-bold text-slate-500 transition-all active:scale-95 hover:bg-slate-100 hover:text-slate-700"
      >
        변경 취소
      </Button>
      <Button
      type="submit"
      disabled={isSubmitting}
      className="h-12 rounded-full bg-primary px-12 text-sm font-black tracking-tight text-white shadow-lg shadow-primary/20 transition-all active:scale-[0.95] hover:bg-primary-hover hover:shadow-primary/40"
    >
        {isSubmitting ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
        설정 저장하기
      </Button>
    </motion.div>
  );
}
