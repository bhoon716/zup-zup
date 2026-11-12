"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { AlertCircle, Loader2, RefreshCcw } from "lucide-react";

import { useAdminDashboardSnapshot } from "@/features/admin/hooks/useAdminDashboard";
import {
  useCrawlCourses,
  useCrawlCoursesByTarget,
  useUpdateAdminCrawlTarget,
} from "@/features/admin/hooks/useAdminActions";

import { Button } from "@/shared/ui/button";

import { AdminCrawlTargetPanel } from "@/features/admin/components/admin-crawl-target-panel";
import { AdminSchedulePanel } from "@/features/admin/components/admin-schedule-panel";

export default function AdminSettingsPage() {
  const {
    data: snapshot,
    isLoading: isSnapshotLoading,
    isError: isSnapshotError,
    refetch: refetchSnapshot,
  } = useAdminDashboardSnapshot();

  const { mutate: crawlCourses, isPending: isCrawling } = useCrawlCourses();
  const { mutate: crawlCoursesByTarget, isPending: isCustomCrawling } = useCrawlCoursesByTarget();
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
    <main className="mx-auto max-w-6xl space-y-4 px-4 py-5 sm:space-y-5 sm:px-6 sm:py-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">시스템 제어 및 설정</h1>
        <Button type="button" variant="outline" onClick={handleRefresh}>
          <RefreshCcw className="size-4" />
          새로고침
        </Button>
      </div>

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

      <AdminSchedulePanel upcomingOnly />
    </main>
  );
}
