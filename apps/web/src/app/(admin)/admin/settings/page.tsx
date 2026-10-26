"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { AlertCircle, Loader2, RefreshCcw, Settings, ArrowLeft } from "lucide-react";
import Link from "next/link";

import { useAdminDashboardSnapshot } from "@/features/admin/hooks/useAdminDashboard";
import {
  useCrawlCourses,
  useCrawlCoursesByTarget,
  useSendTestNotification,
  useUpdateAdminCrawlTarget,
} from "@/features/admin/hooks/useAdminActions";

import { Button } from "@/shared/ui/button";

import { AdminQuickActions } from "@/features/admin/components/admin-quick-actions";
import { AdminCrawlTargetPanel } from "@/features/admin/components/admin-crawl-target-panel";
import { AdminSchedulePanel } from "@/features/admin/components/admin-schedule-panel";

/**
 * 관리자 제어 및 설정 관리 페이지 컴포넌트입니다.
 * 수강신청 일정, 크롤러 동작 조건, 알림 테스트 등을 통합 제어합니다.
 */
export default function AdminSettingsPage() {
  const {
    data: snapshot,
    isLoading: isSnapshotLoading,
    isError: isSnapshotError,
    refetch: refetchSnapshot,
  } = useAdminDashboardSnapshot();

  const { mutate: crawlCourses, isPending: isCrawling } = useCrawlCourses();
  const { mutate: crawlCoursesByTarget, isPending: isCustomCrawling } = useCrawlCoursesByTarget();
  const { mutate: sendTestNotification, isPending: isSendingTest } = useSendTestNotification();
  const { mutate: updateCrawlTarget, isPending: isUpdatingCrawlTarget } = useUpdateAdminCrawlTarget();

  const [configuredDraft, setConfiguredDraft] = useState<{ year: string; semester: string } | null>(null);
  const [runDraft, setRunDraft] = useState<{ year: string; semester: string } | null>(null);

  const crawlTarget = snapshot?.crawlTarget;

  const configuredYear = configuredDraft?.year ?? crawlTarget?.year ?? "";
  const configuredSemester = configuredDraft?.semester ?? crawlTarget?.semester ?? "";
  const runYear = runDraft?.year ?? crawlTarget?.year ?? "";
  const runSemester = runDraft?.semester ?? crawlTarget?.semester ?? "";
  const canSaveConfiguredTarget = /^\d{4}$/.test(configuredYear.trim()) && configuredSemester.trim().length > 0;
  const canRunCustomTarget = /^\d{4}$/.test(runYear.trim()) && runSemester.trim().length > 0;

  const handleRefresh = () => {
    void refetchSnapshot();
  };

  // 로딩 상태 처리
  if (isSnapshotLoading) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 min-h-[70dvh]">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
          <p className="text-sm font-medium text-slate-500">설정 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  // 에러 발생 혹은 데이터 부재 처리
  if (isSnapshotError || !snapshot || !crawlTarget) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 px-6 min-h-[70dvh]">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-[2.5rem] border border-red-100 bg-white p-12 text-center shadow-xl shadow-red-500/5 max-w-md w-full"
        >
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-red-50 text-red-500">
            <AlertCircle className="h-10 w-10" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 mb-2">연결 오류</h2>
          <p className="text-sm text-slate-500 mb-8 leading-relaxed">
            설정 데이터를 불러오지 못했습니다.<br />서버 상태를 확인하거나 잠시 후 다시 시도해 주세요.
          </p>
          <Button
            onClick={handleRefresh}
            className="w-full h-14 rounded-2xl bg-primary hover:bg-primary-dark transition-all text-base font-semibold"
          >
            <RefreshCcw className="mr-2 h-5 w-5" />
            다시 시도하기
          </Button>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="bg-[#f3f4f6] text-slate-900 min-h-[calc(100dvh-4rem)]">
      <div className="sticky top-16 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1920px] items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <Link href="/admin" className="flex items-center gap-1.5 font-semibold text-slate-600 hover:text-slate-900 transition-colors">
              <ArrowLeft className="h-4 w-4" />
              대시보드 돌아가기
            </Link>
            <span className="text-slate-300">|</span>
            <span className="font-medium text-slate-800">시스템 제어 & 설정</span>
          </div>
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              className="h-8 gap-1 rounded-lg border-slate-200 bg-slate-50 px-3 text-xs font-medium text-slate-600 hover:bg-slate-100"
              onClick={handleRefresh}
            >
              <RefreshCcw className="h-3.5 w-3.5" />
              새로고침
            </Button>
          </div>
        </div>
      </div>

      <main className="mx-auto max-w-[1920px] space-y-8 px-4 py-6 sm:space-y-10 sm:px-6 sm:py-8 lg:space-y-12 lg:px-8">
        <motion.div 
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex items-center gap-3 border-b border-slate-200/60 pb-5"
        >
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-50 text-indigo-500 shadow-sm">
            <Settings className="h-6 w-6" />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">시스템 제어 센터</h1>
            <p className="text-xs text-slate-500 font-medium">크롤러 동기화 및 수강신청/종강 디데이 일정을 동적으로 구성합니다.</p>
          </div>
        </motion.div>

        <section className="grid grid-cols-1 gap-12 lg:grid-cols-3">
          {/* 퀵 컨트롤 센터 섹션 (크롤링 즉시 실행 & 테스트 알림) */}
          <AdminQuickActions 
            onRefresh={handleRefresh}
            onCrawl={() => crawlCourses()}
            onSendTest={() => sendTestNotification()}
            isCrawling={isCrawling}
            isSendingTest={isSendingTest}
          />

          {/* 크롤링 대상 학기 설정 섹션 */}
          <div className="lg:col-span-2">
            <AdminCrawlTargetPanel
              configuredYear={configuredYear}
              configuredSemester={configuredSemester}
              onConfiguredYearChange={(value) => {
                setConfiguredDraft((prev) => ({
                  year: value,
                  semester: prev?.semester ?? crawlTarget?.semester ?? "",
                }));
              }}
              onConfiguredSemesterChange={(value) => {
                setConfiguredDraft((prev) => ({
                  year: prev?.year ?? crawlTarget?.year ?? "",
                  semester: value,
                }));
              }}
              onSaveConfiguredTarget={() => {
                updateCrawlTarget({
                  year: configuredYear.trim(),
                  semester: configuredSemester.trim(),
                });
              }}
              onRunConfiguredTarget={() => crawlCourses()}
              isConfiguredTargetLoading={isSnapshotLoading}
              isSavingConfiguredTarget={isUpdatingCrawlTarget}
              isRunningConfiguredTarget={isCrawling}
              canSaveConfiguredTarget={canSaveConfiguredTarget}
              runYear={runYear}
              runSemester={runSemester}
              onRunYearChange={(value) => {
                setRunDraft((prev) => ({
                  year: value,
                  semester: prev?.semester ?? crawlTarget?.semester ?? "",
                }));
              }}
              onRunSemesterChange={(value) => {
                setRunDraft((prev) => ({
                  year: prev?.year ?? crawlTarget?.year ?? "",
                  semester: value,
                }));
              }}
              onRunCustomTarget={() => {
                crawlCoursesByTarget({
                  year: runYear.trim(),
                  semester: runSemester.trim(),
                });
              }}
              isRunningCustomTarget={isCustomCrawling}
              canRunCustomTarget={canRunCustomTarget}
            />
          </div>
        </section>

        {/* 주요 D-Day 학사 일정 관리 섹션 */}
        <AdminSchedulePanel />
      </main>
    </div>
  );
}
