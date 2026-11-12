"use client";

import { motion } from "framer-motion";
import { AlertCircle, CloudCog, Loader2, RefreshCcw, Users } from "lucide-react";
import { useState } from "react";

import { AdminOverview } from "@/features/admin/components/admin-overview";
import { AdminStatCard } from "@/features/admin/components/admin-stat-card";
import { AdminTrafficChart } from "@/features/admin/components/admin-traffic-chart";
import { useAdminDashboardSnapshot, useAdminNotificationTraffic } from "@/features/admin/hooks/useAdminDashboard";
import { formatRelative, formatTime, getStatusMeta } from "@/features/admin/lib/formatters";
import type { AdminTrafficRange } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

export default function AdminDashboardPage() {
  const [range, setRange] = useState<AdminTrafficRange>("LAST_24_HOURS");
  const traffic = useAdminNotificationTraffic(range);
  const {
    data: snapshot,
    isLoading: isSnapshotLoading,
    isError: isSnapshotError,
    refetch: refetchSnapshot,
  } = useAdminDashboardSnapshot();

  const overview = snapshot?.overview;

  const handleRefresh = () => {
    void refetchSnapshot();
    void traffic.refetch();
  };

  if (isSnapshotLoading) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 min-h-[70dvh]">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
          <p className="text-sm font-medium text-slate-500">대시보드 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (isSnapshotError || !snapshot || !overview) {
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

  const status = getStatusMeta(overview.crawlingStatus);
  const latency = overview.jbnuLatencyMs === null ? "측정 기록 없음" : `${overview.jbnuLatencyMs} ms`;

  return (
    <main>
      <AdminOverview serverClock={formatTime(overview.serverTime)} onRefresh={handleRefresh} />

      <div className="mx-auto max-w-[1920px] space-y-6 px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
        <div className="grid gap-4 md:grid-cols-2 sm:gap-6">
          <AdminStatCard title="크롤러 상태" icon={CloudCog}>
            <div>
              <dt className="text-sm font-medium text-slate-600">최근 크롤링</dt>
              <dd className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-slate-600">
                <span className={`rounded-full px-2.5 py-1 font-semibold ${status.badgeClass}`}>{status.label}</span>
                {formatRelative(overview.lastCrawledAt)}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-600">JBNU 평균 지연시간</dt>
              <dd className="mt-1 text-lg font-bold tabular-nums text-slate-900">{latency}</dd>
            </div>
          </AdminStatCard>

          <AdminStatCard title="사용자 및 빈자리 구독" icon={Users}>
            <div>
              <dt className="text-sm font-medium text-slate-600">누적 사용자</dt>
              <dd className="mt-1 text-lg font-bold tabular-nums text-slate-900">
                {overview.totalUsers.toLocaleString("ko-KR")}명
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-slate-600">활성 빈자리 구독</dt>
              <dd className="mt-1 text-lg font-bold tabular-nums text-slate-900">
                {overview.totalActiveSubscriptions.toLocaleString("ko-KR")}건
              </dd>
            </div>
          </AdminStatCard>
        </div>

        <AdminTrafficChart
          range={range}
          onRangeChange={setRange}
          traffic={traffic.data}
          isLoading={traffic.isLoading}
          isError={traffic.isError}
          onRetry={() => void traffic.refetch()}
        />
      </div>
    </main>
  );
}
