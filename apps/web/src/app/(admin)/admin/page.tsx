"use client";

import { motion } from "framer-motion";
import { AlertCircle, BellRing, CloudCog, Gauge, Loader2, RefreshCcw, Users } from "lucide-react";

import { useAdminOverview } from "@/features/admin/hooks/useAdminOverview";
import { useHealth } from "@/features/admin/hooks/useHealth";
import { useCrawlCourses, useSendTestNotification } from "@/features/admin/hooks/useAdminActions";

import { Button } from "@/shared/ui/button";

import { AdminStatCard } from "@/features/admin/components/admin-stat-card";
import { AdminTrafficChart } from "@/features/admin/components/admin-traffic-chart";
import { AdminQuickActions } from "@/features/admin/components/admin-quick-actions";
import { AdminActivityLog } from "@/features/admin/components/admin-activity-log";
import { AdminOverview } from "@/features/admin/components/admin-overview";
import { 
  formatNumber, 
  formatDateTime, 
  formatTime, 
  formatRelative, 
  getStatusMeta, 
  getLogMeta 
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
  
  const {
    data: healthData,
    isLoading: isHealthLoading,
    refetch: refetchHealth,
  } = useHealth();
  
  const { mutate: crawlCourses, isPending: isCrawling } = useCrawlCourses();
  const { mutate: sendTestNotification, isPending: isSendingTest } = useSendTestNotification();

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
    <div className="flex min-h-screen flex-col bg-slate-50 text-slate-900">
      <main className="flex-1">
        <AdminOverview serverClock={serverClockText} onRefresh={handleRefresh} />

        <div className="container mx-auto space-y-12 p-6 lg:p-12">
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
               Internal Dashboard
               <div className="h-px w-8 bg-slate-100"></div>
            </div>
            <p className="text-xs font-bold text-slate-400">
              © 2026 JBNU 수강신청 도우미 관리자. <br className="sm:hidden" /> All rights reserved. V2.0.0-GOLD
            </p>
          </footer>
        </div>
      </main>
    </div>
  );
}
