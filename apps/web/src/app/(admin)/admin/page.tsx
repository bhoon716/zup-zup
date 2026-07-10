"use client";

import { motion } from "framer-motion";
import { AlertCircle, BellRing, CloudCog, Gauge, Loader2, RefreshCcw, Users, Settings } from "lucide-react";
import Link from "next/link";

import { useAdminDashboardSnapshot } from "@/features/admin/hooks/useAdminDashboard";
import { Button } from "@/shared/ui/button";

import { AdminStatCard } from "@/features/admin/components/admin-stat-card";
import { AdminTrafficChart } from "@/features/admin/components/admin-traffic-chart";
import { AdminActivityLog } from "@/features/admin/components/admin-activity-log";
import { AdminOverview } from "@/features/admin/components/admin-overview";
import {
  formatDateTime,
  formatNumber,
  formatRelative,
  formatTime,
  getLogMeta,
  getStatusMeta,
} from "@/features/admin/lib/formatters";

/**
 * 관리자 대시보드 페이지 메인 모니터링 컴포넌트입니다.
 * 서비스 전체의 핵심 지표, 알림 발송 실시간 트래픽, 최근 활동 로그를 통합 관제합니다.
 */
export default function AdminDashboardPage() {
  const {
    data: snapshot,
    isLoading: isSnapshotLoading,
    isError: isSnapshotError,
    refetch: refetchSnapshot,
  } = useAdminDashboardSnapshot();

  const overview = snapshot?.overview;

  /**
   * 대시보드 모니터링 데이터를 최신 상태로 갱신합니다.
   */
  const handleRefresh = () => {
    void refetchSnapshot();
  };

  // 로딩 상태 처리
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

  // 에러 발생 혹은 데이터 부재 처리
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

  const statusMeta = getStatusMeta(overview.crawlingStatus || "UNKNOWN");
  const serverClockText = formatTime(overview.serverTime);

  const statItems = [
    { label: "크롤러 상태", value: overview.crawlingStatus || "UNKNOWN", sub: `마지막: ${formatRelative(overview.lastCrawledAt)}`, icon: CloudCog, meta: statusMeta, color: "primary" as const },
    { label: "JBNU 지연시간", value: overview.jbnuLatencyMs === null ? "-" : `${overview.jbnuLatencyMs}ms`, sub: "실시간 연동 준비 중", icon: Gauge, color: "amber" as const },
    { label: "누적 사용자 수", value: formatNumber(overview.totalUsers), sub: "이번 학기 활성 학생", icon: Users, badge: "LIVE", color: "green" as const },
    { label: "현재 가동 중인 알림", value: formatNumber(overview.totalActiveSubscriptions), sub: "수강신청 빈자리 대기", icon: BellRing, badge: "Push", color: "indigo" as const }
  ];

  return (
    <div className="bg-[#f3f4f6] text-slate-900 min-h-[calc(100dvh-4rem)]">
      <div className="sticky top-16 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1920px] items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <span className="font-medium text-slate-800">시스템 현황 관제</span>
          </div>
          <div className="flex items-center gap-2">
            <Link href="/admin/announcements">
              <Button
                type="button"
                variant="outline"
                className="h-8 rounded-lg border-slate-200 bg-slate-50 px-3 text-xs font-medium text-slate-600 hover:bg-slate-100"
              >
                공지사항 관리
              </Button>
            </Link>
            <Link href="/admin/settings">
              <Button
                type="button"
                variant="outline"
                className="h-8 gap-1.5 rounded-lg border-primary/30 bg-primary/10 px-3 text-xs font-semibold text-primary hover:bg-primary/20"
              >
                <Settings className="h-3.5 w-3.5" />
                시스템 제어 & 설정
              </Button>
            </Link>
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

      <main>
        <AdminOverview serverClock={serverClockText} onRefresh={handleRefresh} />

        <div className="mx-auto max-w-[1920px] space-y-8 px-4 py-6 sm:space-y-10 sm:px-6 sm:py-8 lg:space-y-12 lg:px-8">
          {/* 주요 통계 그리드 섹션 */}
          <section className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-4">
            {statItems.map((stat, i) => (
              <AdminStatCard key={stat.label} {...stat} index={i} />
            ))}
          </section>

          {/* 트래픽 시각화 차트 섹션 (가독성 향상을 위해 더 넓게 배치) */}
          <section className="grid grid-cols-1 gap-12 lg:grid-cols-3">
            <div className="lg:col-span-3">
              <AdminTrafficChart traffic={overview.notificationTraffic ?? []} />
            </div>
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
               내부 대시보드 관제
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
