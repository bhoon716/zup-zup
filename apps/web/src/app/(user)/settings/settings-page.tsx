"use client";

import { motion, AnimatePresence } from "framer-motion";
import { 
  Loader2, Mail, Bell, Smartphone, Save,
  CheckCircle, MessageSquare, Laptop,
  X, AlertCircle, Monitor
} from "lucide-react";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Input } from "@/shared/ui/input";
import { cn } from "@/shared/lib/utils";
import type { SettingsPageModel } from "./useSettingsPage";

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
      <motion.main 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.6 }}
        className="max-w-4xl mx-auto px-6 sm:px-10 pt-10 pb-32 md:pt-16 md:pb-40"
      >
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="mb-12 border-b border-slate-100 pb-8"
        >
          <h1 className="text-3xl font-bold mb-2 text-slate-900 tracking-tight">알림 설정</h1>
          <p className="text-slate-500 font-medium">빈 좌석 알림을 받을 채널을 설정하고 관리하세요.</p>
        </motion.div>

        <form onSubmit={handleSubmit(onSubmit)}>
          <section className="space-y-12">
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
            >
              <div className="flex items-center justify-between mb-8">
                <div>
                  <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
                      <Bell className="text-primary w-5 h-5" />
                    </div>
                    알림 채널 연동
                  </h2>
                  <p className="text-sm text-slate-500 mt-2 ml-11.5 font-medium">여러 채널을 연동하여 확실하게 알림을 받아보세요.</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <Button
                    type="button"
                    onClick={handleSendTestNotification}
                    disabled={isSendingTest || testCooldownSeconds > 0}
                    className="h-11 rounded-xl bg-primary px-5 text-sm font-bold text-white shadow-sm hover:bg-primary-hover disabled:bg-slate-300 transition-all active:scale-95"
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
                  <p className="text-xs text-slate-400">저장된 설정 기준으로 전송됩니다.</p>
                </div>
              </div>

              <div className="space-y-6">
                <motion.div 
                  whileHover={{ y: -2 }}
                  className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-[#5865F2] rounded-2xl items-center justify-center shadow-lg shadow-[#5865F2]/20 shrink-0 text-white">
                      <MessageSquare className="w-8 h-8" />
                    </div>
                    <div className="flex-1 w-full">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="font-bold text-lg text-slate-900">Discord 연동</h3>
                        {user?.discordId && (
                           <Badge variant="outline" className="bg-indigo-50 text-indigo-600 border-indigo-100 font-bold px-2 py-0.5 text-[10px] rounded-lg">
                             연동됨
                           </Badge>
                        )}
                      </div>
                      <p className="text-sm text-slate-500 mb-5 font-medium">디스코드 봇이 개인 DM으로 알림을 즉시 보내드립니다.</p>
                      
                      {user?.discordId ? (
                        <div className="flex flex-col sm:flex-row gap-3">
                          <div className="flex-1 bg-white border border-slate-200 rounded-xl px-4 py-3 flex items-center justify-between">
                            <span className="text-sm font-mono font-bold text-slate-700">{user.discordId}</span>
                            <CheckCircle className="w-4 h-4 text-emerald-500" />
                          </div>
                          <Button 
                            type="button" 
                            variant="destructive"
                            onClick={handleDiscordUnlink}
                            disabled={isUnlinking}
                            className="rounded-xl px-6 h-12 font-bold shadow-soft transition-all active:scale-95"
                          >
                            {isUnlinking ? <Loader2 className="w-4 h-4 animate-spin" /> : "연동 해제"}
                          </Button>
                        </div>
                      ) : (
                        <Button 
                          type="button"
                          onClick={handleDiscordConnect}
                          className="w-full bg-[#5865F2] hover:bg-[#4752C4] text-white h-13 rounded-xl font-bold flex items-center justify-center gap-3 transition-all shadow-lg shadow-[#5865F2]/20 active:scale-[0.98]"
                        >
                          <MessageSquare className="w-6 h-6" />
                          Discord 계정 연결하기
                        </Button>
                      )}

                      <div className="mt-4 flex items-center justify-between border-t border-slate-200/50 pt-4">
                        <Label htmlFor="discord-enabled" className="text-sm font-bold text-slate-600">DM 알림 활성화</Label>
                        <Switch
                          checked={watch("discordEnabled")}
                          onCheckedChange={(checked) => setValue("discordEnabled", checked, { shouldDirty: true })}
                        />
                      </div>
                    </div>
                  </div>
                </motion.div>

                <motion.div 
                  whileHover={{ y: -2 }}
                  className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-white rounded-2xl items-center justify-center shadow-sm border border-slate-100 shrink-0">
                      <Mail className="w-7 h-7 text-slate-400" />
                    </div>
                    <div className="flex-1 w-full">
                      <div className="flex items-center justify-between mb-1">
                        <h3 className="font-bold text-lg text-slate-900">이메일 알림</h3>
                        {(isGoogleEmail || verified) && (
                          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">인증됨</span>
                        )}
                      </div>
                      <p className="text-sm text-slate-500 mb-5 font-medium">중요 알림을 이메일로 받아보세요. 네이버 이메일을 권장합니다.</p>
                      
                      <div className="space-y-3">
                        <div className="relative flex items-center gap-2">
                          <Input 
                            {...register("notificationEmail")}
                            placeholder={user?.email}
                            className={cn(
                              "w-full bg-white border-slate-200 rounded-xl px-4 py-6 text-sm focus:ring-2 focus:ring-primary h-12 transition-all",
                              (verified && !needsVerification && watch("notificationEmail")) && "bg-slate-50 text-slate-500 font-semibold"
                            )}
                          />
                          {needsVerification && !verified && (
                            <Button 
                              type="button"
                              variant="outline"
                              onClick={onSendCode}
                              disabled={sending || !!errors.notificationEmail || !watch("notificationEmail")}
                              className="shrink-0 h-12 rounded-xl px-6 bg-white border-slate-200 font-bold text-slate-600 hover:bg-slate-50 transition-all active:scale-95"
                            >
                              {sending ? <Loader2 className="w-4 h-4 animate-spin" /> : "인증"}
                            </Button>
                          )}
                          {verified && watch("notificationEmail") && (
                            <div className="absolute right-3 bg-slate-100/80 text-primary px-3 py-1.5 rounded-lg text-[11px] font-bold border border-primary/10 backdrop-blur-sm">
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
                              className="bg-primary/5 p-4 rounded-[1.5rem] border border-primary/10 flex flex-col sm:flex-row gap-3 overflow-hidden"
                            >
                              <Input 
                                placeholder="인증 코드 6자리"
                                value={authCode}
                                onChange={(e) => setAuthCode(e.target.value)}
                                maxLength={6}
                                className="flex-1 h-12 text-center text-lg font-black tracking-[0.3em] rounded-xl border-primary/20 bg-white"
                              />
                              <Button 
                                type="button" 
                                onClick={onVerifyCode} 
                                disabled={verifying || authCode.length !== 6} 
                                className="h-12 px-8 rounded-xl font-bold bg-primary transition-all active:scale-95"
                              >
                                {verifying ? <Loader2 className="w-4 h-4 animate-spin" /> : "확인"}
                              </Button>
                            </motion.div>
                          )}
                        </AnimatePresence>

                        <div className="flex items-center justify-between border-t border-slate-200/50 pt-4">
                          <Label htmlFor="email-enabled" className="text-sm font-bold text-slate-600">이메일 알림 활성화</Label>
                          <Switch
                            checked={watch("emailEnabled")}
                            onCheckedChange={(checked) => setValue("emailEnabled", checked, { shouldDirty: true })}
                          />
                        </div>
                      </div>
                      <p className="text-[11px] text-slate-400 mt-3 font-medium flex items-center gap-1.5">
                        <AlertCircle className="w-3.5 h-3.5" />
                        주 선택 이메일이 아닌 경우 인증이 필요합니다.
                      </p>
                    </div>
                  </div>
                </motion.div>

                <motion.div 
                  whileHover={{ y: -2 }}
                  className="bg-slate-50 rounded-[2rem] border border-slate-100 p-8 transition-all hover:border-primary/30 hover:shadow-md"
                >
                  <div className="flex items-start gap-6">
                    <div className="hidden sm:flex w-14 h-14 bg-white rounded-2xl items-center justify-center shadow-sm border border-slate-100 shrink-0">
                      <Laptop className="w-7 h-7 text-primary" />
                    </div>
                    <div className="flex-1 w-full">
                      <h3 className="font-bold text-lg text-slate-900 mb-1">웹 푸시 알림</h3>
                      <p className="text-sm text-slate-500 mb-5 font-medium">현재 사용 중인 기기를 등록하여 브라우저 알림을 받습니다.</p>
                      
                      <div className="bg-white rounded-2xl border border-slate-100 p-4 mb-6 transition-all hover:border-primary/20">
                        <div className="flex flex-col sm:flex-row gap-3">
                          <Input 
                            value={deviceAlias}
                            onChange={(e) => setDeviceAlias(e.target.value)}
                            placeholder="기기 별칭 (예: 내 노트북)"
                            className="flex-1 bg-slate-50 border-transparent rounded-xl px-4 h-12 focus:bg-white transition-all shadow-none"
                          />
                          <Button 
                            type="button"
                            onClick={handleRegisterDevice}
                            disabled={loadingWebPush}
                            className="bg-primary hover:bg-primary-hover text-white px-6 h-12 rounded-xl text-sm font-bold shadow-lg shadow-primary/20 transition-all active:scale-[0.95] disabled:bg-slate-300"
                          >
                            {loadingWebPush ? (
                              <>
                                <Loader2 className="w-4 h-4 animate-spin mr-2" />
                                등록 중...
                              </>
                            ) : (
                              "현재 기기 등록"
                            )}
                          </Button>
                        </div>
                      </div>

                      <div className="space-y-3">
                        <h4 className="text-xs font-bold text-slate-400 px-1 uppercase tracking-wider mb-2">등록된 기기 목록</h4>
                        {devices.length === 0 ? (
                           <div className="text-center py-6 bg-white/50 rounded-2xl border border-dashed border-slate-200">
                             <p className="text-sm text-slate-400 font-medium">등록된 기기가 없습니다.</p>
                           </div>
                        ) : (
                          <div className="space-y-2">
                            {devices.map((device, idx) => (
                              <motion.div 
                                layout
                                key={device.id} 
                                className="flex items-center justify-between bg-white border border-slate-100 px-4 py-3.5 rounded-xl shadow-sm hover:shadow-md transition-shadow"
                              >
                                <div className="flex items-center gap-3">
                                  <div className="p-2 rounded-lg bg-slate-50">
                                    {device.type === 'WEB' ? <Monitor className="w-4 h-4 text-slate-400" /> : <Smartphone className="w-4 h-4 text-slate-400" />}
                                  </div>
                                  <div className="flex flex-col">
                                    <div className="flex items-center gap-2">
                                       <span className="text-sm font-bold text-slate-700">{device.alias || '알 수 없는 기기'}</span>
                                       {idx === 0 && <span className="text-[9px] bg-primary/10 text-primary px-1.5 py-0.5 rounded font-black tracking-tighter">THIS</span>}
                                    </div>
                                    <span className="text-[10px] text-slate-400 font-medium">{new Date(device.registeredAt).toLocaleDateString()} 등록</span>
                                  </div>
                                </div>
                                <button 
                                  type="button" 
                                  onClick={() => handleDeleteDevice(device.id)}
                                  className="w-8 h-8 flex items-center justify-center text-slate-300 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                                >
                                  <X className="w-4 h-4" />
                                </button>
                              </motion.div>
                            ))}
                          </div>
                        )}
                      </div>

                      <div className="mt-6 flex items-center justify-between border-t border-slate-200/50 pt-5">
                        <div className="space-y-1">
                          <Label htmlFor="web-enabled" className="text-sm font-bold text-slate-600">브라우저 전체 알림</Label>
                          <p className="text-[11px] text-slate-400 font-medium">기기 개별 설정이 아닌 서비스 전체 알림 스위치입니다.</p>
                        </div>
                        <Switch
                          checked={watch("webPushEnabled")}
                          onCheckedChange={async (checked) => {
                            setValue("webPushEnabled", checked, { shouldDirty: true });
                          }}
                          disabled={loadingWebPush}
                        />
                      </div>
                    </div>
                  </div>
                </motion.div>
              </div>
            </motion.div>
          </section>

          <motion.div 
            initial={{ y: 100 }}
            animate={{ y: 0 }}
            transition={{ duration: 0.5, type: "spring", damping: 20 }}
            className="fixed bottom-0 right-0 left-0 bg-white/90 backdrop-blur-md border-t border-slate-100 px-8 py-5 flex items-center justify-center sm:justify-end gap-4 z-40 shadow-[0_-8px_30px_rgba(0,0,0,0.04)]"
          >
            <Button 
              type="button" 
              variant="ghost"
              onClick={() => router.back()}
              className="text-slate-500 hover:text-slate-700 hover:bg-slate-100 font-bold px-8 h-12 rounded-full transition-all text-sm active:scale-95"
            >
              변경 취소
            </Button>
            <Button 
              type="submit"
              disabled={isSubmitting}
              className="bg-primary hover:bg-primary-hover text-white font-black px-12 h-12 rounded-full transition-all shadow-lg shadow-primary/20 hover:shadow-primary/40 active:scale-[0.95] text-sm tracking-tight"
            >
              {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : <Save className="w-4 h-4 mr-2" />}
              설정 저장하기
            </Button>
          </motion.div>
        </form>
      </motion.main>
    </div>
  );
}

function Switch({ checked, onCheckedChange, disabled }: { checked: boolean, onCheckedChange: (checked: boolean) => void, disabled?: boolean }) {
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
        disabled && "opacity-50 cursor-not-allowed"
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
