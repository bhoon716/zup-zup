"use client";

import { AlertCircle, CloudCog, Loader2, RefreshCcw, Users } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { AdminOverview } from "@/features/admin/components/admin-overview";
import { AdminStatGroup } from "@/features/admin/components/admin-stat-group";
import { AdminTrafficChart } from "@/features/admin/components/admin-traffic-chart";
import { useSendTestNotification } from "@/features/admin/hooks/useAdminActions";
import { useAdminDashboardSnapshot, useAdminNotificationTraffic } from "@/features/admin/hooks/useAdminDashboard";
import { CRAWL_SEMESTER_OPTIONS } from "@/features/admin/lib/crawl-semester-options";
import { formatRelative, formatTime, getStatusMeta } from "@/features/admin/lib/formatters";
import type { AdminTrafficRange } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

export default function AdminDashboardPage() {
  const [range, setRange] = useState<AdminTrafficRange>("LAST_24_HOURS");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const traffic = useAdminNotificationTraffic(range);
  const { mutate: sendTestNotification, isPending: isSendingTest } = useSendTestNotification();
  const {
    data: snapshot,
    isLoading: isSnapshotLoading,
    refetch: refetchSnapshot,
  } = useAdminDashboardSnapshot();

  const handleRefresh = async () => {
    setIsRefreshing(true);
    const options = { throwOnError: true, cancelRefetch: false };
    const [snapshotResult, trafficResult] = await Promise.allSettled([
      refetchSnapshot(options),
      traffic.refetch(options),
    ]);
    setIsRefreshing(false);

    const snapshotFailed = snapshotResult.status === "rejected";
    const trafficFailed = trafficResult.status === "rejected";
    if (snapshotFailed && trafficFailed) {
      toast.error("대시보드 데이터를 새로고침하지 못했습니다.");
    } else if (snapshotFailed) {
      toast.error("시스템 현황을 새로고침하지 못했습니다.");
    } else if (trafficFailed) {
      toast.error("알림 발송량을 새로고침하지 못했습니다.");
    } else {
      toast.success("대시보드 데이터를 새로고침했습니다.");
    }
  };

  if (isSnapshotLoading && !isRefreshing) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 min-h-[70dvh]">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
          <p className="text-sm font-medium text-slate-500">대시보드 데이터를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (!snapshot) {
    return (
      <div className="flex h-full items-center justify-center bg-slate-50 px-6 min-h-[70dvh]">
        <div className="rounded-[2.5rem] border border-red-100 bg-white p-12 text-center shadow-xl shadow-red-500/5 max-w-md w-full">
          <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-red-50 text-red-500">
            <AlertCircle className="h-10 w-10" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 mb-2">연결 오류</h2>
          <p className="text-sm text-slate-500 mb-8 leading-relaxed">
            관리자 대시보드 데이터를 불러오지 못했습니다.<br />서버 상태를 확인하거나 잠시 후 다시 시도해 주세요.
          </p>
          <Button
            onClick={handleRefresh}
            disabled={isRefreshing}
            className="w-full h-14 rounded-2xl bg-primary hover:bg-primary-dark transition-all text-base font-semibold"
          >
            {isRefreshing ? <Loader2 aria-hidden="true" className="mr-2 h-5 w-5 animate-spin" /> : <RefreshCcw aria-hidden="true" className="mr-2 h-5 w-5" />}
            {isRefreshing ? "새로고침 중…" : "다시 시도하기"}
          </Button>
        </div>
      </div>
    );
  }

  const { overview, crawlTarget: { year, semester } } = snapshot;
  const semesterLabel = CRAWL_SEMESTER_OPTIONS.find((option) => option.code === semester)?.label ?? semester;
  const status = getStatusMeta(overview.crawlingStatus);

  return (
    <main>
      <AdminOverview
        serverClock={formatTime(overview.serverTime)}
        onRefresh={handleRefresh}
        isRefreshing={isRefreshing}
        onSendTest={() => sendTestNotification()}
        isSendingTest={isSendingTest}
      />

      <div className="@container mx-auto max-w-[1920px] space-y-4 px-4 py-4 sm:px-6 sm:py-5 lg:px-8">
        <div className="grid divide-y divide-slate-200 rounded-xl border border-slate-200 bg-white @min-[680px]:grid-cols-2 @min-[680px]:divide-x @min-[680px]:divide-y-0">
          <AdminStatGroup title="크롤러 상태" icon={CloudCog}>
            <div>
              <dt className="text-xs font-medium text-slate-600 sm:text-sm">최근 크롤링</dt>
              <dd className="mt-1.5">
                <span className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-sm font-semibold ${status.badgeClass}`}>
                  <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
                  {status.label}
                </span>
                <span className="mt-1 block text-xs text-slate-600">{formatRelative(overview.lastCrawledAt)}</span>
              </dd>
            </div>
          </AdminStatGroup>

          <AdminStatGroup title="사용자 및 빈자리 구독" icon={Users}>
            <div>
              <dt className="text-xs font-medium text-slate-600 sm:text-sm">누적 사용자</dt>
              <dd className="mt-1 text-lg font-semibold tabular-nums text-slate-900">
                {overview.totalUsers.toLocaleString("ko-KR")}명
              </dd>
            </div>
            <div>
              <dt className="text-xs font-medium text-slate-600 sm:text-sm">활성 빈자리 구독</dt>
              <dd className="mt-1">
                <span className="block text-lg font-semibold tabular-nums text-slate-900">
                  {overview.totalActiveSubscriptions.toLocaleString("ko-KR")}건
                </span>
                <span className="block text-xs text-slate-600">{year}학년도 {semesterLabel}</span>
              </dd>
            </div>
          </AdminStatGroup>
        </div>

        <AdminTrafficChart
          range={range}
          onRangeChange={setRange}
          traffic={traffic.data}
          isLoading={traffic.isLoading}
          isError={traffic.isError}
          onRetry={() => void traffic.refetch({ cancelRefetch: false })}
        />
      </div>
    </main>
  );
}
