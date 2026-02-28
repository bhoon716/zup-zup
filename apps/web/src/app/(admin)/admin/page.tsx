"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { AlertCircle, BellRing, CloudCog, Gauge, Loader2, RefreshCcw, Users } from "lucide-react";

import { useAdminOverview } from "@/features/admin/hooks/useAdminOverview";
import { useHealth } from "@/features/admin/hooks/useHealth";
import {
  useAdminCrawlTarget,
  useCrawlCourses,
  useCrawlCoursesByTarget,
  useSendTestNotification,
  useUpdateAdminCrawlTarget,
} from "@/features/admin/hooks/useAdminActions";

import { Button } from "@/shared/ui/button";

import { AdminStatCard } from "@/features/admin/components/admin-stat-card";
import { AdminTrafficChart } from "@/features/admin/components/admin-traffic-chart";
import { AdminQuickActions } from "@/features/admin/components/admin-quick-actions";
import { AdminActivityLog } from "@/features/admin/components/admin-activity-log";
import { AdminOverview } from "@/features/admin/components/admin-overview";
import { AdminCrawlTargetPanel } from "@/features/admin/components/admin-crawl-target-panel";
import {
  formatDateTime,
  formatNumber,
  formatRelative,
  formatTime,
  getLogMeta,
  getStatusMeta,
} from "@/features/admin/lib/formatters";

/**
 * 관리자 대시보드 페이지 메인 컴포넌트입니다.
 * 서비스 전체의 지표, 실시간 트래픽, 시스템 로그 및 제어판을 통합적으로 제공합니다.
 */
export default function AdminDashboardPage() {
  const {
    data: overview,
    isLoading: isOverviewLoading,
    isError: isOverviewError,
    refetch: refetchOverview,
  } = useAdminOverview();
  
  const { isLoading: isHealthLoading, refetch: refetchHealth } = useHealth();
  
  const { mutate: crawlCourses, isPending: isCrawling } = useCrawlCourses();
  const { mutate: crawlCoursesByTarget, isPending: isCustomCrawling } = useCrawlCoursesByTarget();
  const { mutate: sendTestNotification, isPending: isSendingTest } = useSendTestNotification();
  const { data: crawlTarget, isLoading: isCrawlTargetLoading } = useAdminCrawlTarget();
  const { mutate: updateCrawlTarget, isPending: isUpdatingCrawlTarget } = useUpdateAdminCrawlTarget();

  const [configuredDraft, setConfiguredDraft] = useState<{ year: string; semester: string } | null>(null);
  const [runDraft, setRunDraft] = useState<{ year: string; semester: string } | null>(null);

  const configuredYear = configuredDraft?.year ?? crawlTarget?.year ?? "";
  const configuredSemester = configuredDraft?.semester ?? crawlTarget?.semester ?? "";
  const runYear = runDraft?.year ?? crawlTarget?.year ?? "";
  const runSemester = runDraft?.semester ?? crawlTarget?.semester ?? "";
  const canSaveConfiguredTarget = /^\d{4}$/.test(configuredYear.trim()) && configuredSemester.trim().length > 0;
  const canRunCustomTarget = /^\d{4}$/.test(runYear.trim()) && runSemester.trim().length > 0;

  /**
   * 모든 대시보드 데이터를 최신 상태로 갱신합니다.
   */
  const handleRefresh = () => {
    void Promise.all([refetchOverview(), refetchHealth()]);
  };

  // 로딩 상태 처리
  if (isOverviewLoading || isHealthLoading) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
          <p className="text-sm font-medium text-slate-500">데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  // 에러 발생 혹은 데이터 부재 처리
  if (isOverviewError || !overview) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 px-6">
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
            관리자 대시보드 데이터를 불러오지 못했습니다.<br />서버 상태를 확인하거나 잠시 후 다시 시도해 주세요.
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

  const statusMeta = getStatusMeta(overview.crawlingStatus || "UNKNOWN");
  const serverClockText = formatTime(overview.serverTime);

  const statItems = [
    { label: "크롤러 상태", value: overview.crawlingStatus || "UNKNOWN", sub: `마지막: ${formatRelative(overview.lastCrawledAt)}`, icon: CloudCog, meta: statusMeta, color: "primary" as const },
    { label: "JBNU 지연시간", value: overview.jbnuLatencyMs === null ? "-" : `${overview.jbnuLatencyMs}ms`, sub: "실시간 연동 준비 중", icon: Gauge, color: "amber" as const },
    { label: "누적 사용자 수", value: formatNumber(overview.totalUsers), sub: "이번 학기 활성 학생", icon: Users, badge: "LIVE", color: "green" as const },
    { label: "현재 가동 중인 알림", value: formatNumber(overview.totalActiveSubscriptions), sub: "수강신청 빈자리 대기", icon: BellRing, badge: "Push", color: "indigo" as const }
  ];

  return (
    <div className="bg-[#f3f4f6] text-slate-900">
      <div className="sticky top-16 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1920px] items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <span className="font-medium text-slate-800">시스템 현황 개요</span>
          </div>
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

      <main>
        <AdminOverview serverClock={serverClockText} onRefresh={handleRefresh} />

        <div className="mx-auto max-w-[1920px] space-y-8 px-4 py-6 sm:space-y-10 sm:px-6 sm:py-8 lg:space-y-12 lg:px-8">
          {/* 주요 통계 그리드 섹션 */}
          <section className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-4">
            {statItems.map((stat, i) => (
              <AdminStatCard key={stat.label} {...stat} index={i} />
            ))}
          </section>

          <section className="grid grid-cols-1 gap-12 lg:grid-cols-3">
            {/* 트래픽 시각화 섹션 */}
            <AdminTrafficChart traffic={overview.notificationTraffic ?? []} />

            {/* 퀵 컨트롤 센터 섹션 */}
            <AdminQuickActions 
              onRefresh={handleRefresh}
              onCrawl={() => crawlCourses()}
              onSendTest={() => sendTestNotification()}
              isCrawling={isCrawling}
              isSendingTest={isSendingTest}
            />
          </section>

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
            isConfiguredTargetLoading={isCrawlTargetLoading}
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

          {/* 시스템 활동 로그 섹션 */}
          <AdminActivityLog 
            logs={overview.recentLogs} 
            formatDateTime={formatDateTime}
            getLogMeta={getLogMeta}
          />

          {/* 푸터 섹션 */}
          <footer className="py-20 text-center">
            <div className="flex items-center justify-center gap-2 text-slate-300 font-black text-[10px] uppercase tracking-[0.3em] mb-4">
               <div className="h-px w-8 bg-slate-100"></div>
               내부 대시보드
               <div className="h-px w-8 bg-slate-100"></div>
            </div>
            <p className="text-xs font-bold text-slate-400">
              © 2026 JBNU 수강신청 도우미 관리자. <br className="sm:hidden" /> 모든 권리 보유. V2.0.0-GOLD
            </p>
          </footer>
        </div>
      </main>
    </div>
  );
}
