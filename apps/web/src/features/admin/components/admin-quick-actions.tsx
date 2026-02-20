"use client";

import { motion } from "framer-motion";
import { Database, RefreshCcw, Send, Power, Loader2 } from "lucide-react";

interface AdminQuickActionsProps {
  onRefresh: () => void;
  onCrawl: () => void;
  onSendTest: () => void;
  isCrawling: boolean;
  isSendingTest: boolean;
}

/**
 * 관리자용 퀵 컨트롤 센터 컴포넌트입니다.
 * 통계 새로고침, 데이터 수집, 테스트 알림 등의 즉각적인 제어 기능을 제공합니다.
 */
export function AdminQuickActions({
  onRefresh,
  onCrawl,
  onSendTest,
  isCrawling,
  isSendingTest,
}: AdminQuickActionsProps) {
  const actions = [
    {
      label: "대시보드 새로고침",
      desc: "실시간 통계 즉시 동기화",
      icon: RefreshCcw,
      onClick: onRefresh,
    },
    {
      label: "강의 데이터 재수집",
      desc: "크롤러를 즉시 가동합니다",
      icon: Database,
      onClick: onCrawl,
      loading: isCrawling,
    },
    {
      label: "자가 알림 테스트",
      desc: "설정된 내 채널로 즉시 발송",
      icon: Send,
      onClick: onSendTest,
      loading: isSendingTest,
    },
  ];

  return (
    <motion.div
      initial={{ opacity: 0, x: 30 }}
      animate={{ opacity: 1, x: 0 }}
      className="flex flex-col rounded-[3rem] border border-slate-100 bg-white p-10 shadow-2xl shadow-slate-200/50"
    >
      <h3 className="mb-8 text-2xl font-black text-slate-900 tracking-tight">퀵 컨트롤 센터</h3>
      <div className="flex flex-1 flex-col gap-5">
        {actions.map((action) => (
          <button
            key={action.label}
            type="button"
            className="group relative h-28 w-full overflow-hidden rounded-[2rem] border border-slate-100 bg-slate-50/50 transition-all hover:scale-[1.02] hover:border-primary/30 hover:bg-white hover:shadow-xl hover:shadow-primary/5"
            onClick={action.onClick}
            disabled={action.loading}
          >
            <div className="flex items-center gap-6 px-7">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/50 transition-colors group-hover:bg-primary group-hover:text-white group-hover:ring-primary">
                {action.loading ? <Loader2 className="h-6 w-6 animate-spin" /> : <action.icon className="h-6 w-6" />}
              </div>
              <div className="text-left">
                <p className="text-base font-black text-slate-900 group-hover:text-primary transition-colors">{action.label}</p>
                <p className="text-xs font-medium text-slate-500 group-hover:text-slate-600">{action.desc}</p>
              </div>
            </div>
          </button>
        ))}

        <button
          type="button"
          className="group relative mt-auto h-28 w-full overflow-hidden rounded-[2rem] border border-red-100 bg-red-50/30 transition-all hover:scale-[1.02] hover:border-red-500 hover:bg-red-600 hover:text-white hover:shadow-xl hover:shadow-red-500/10"
        >
          <div className="flex items-center gap-6 px-7">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-red-200/50 transition-colors group-hover:bg-white group-hover:text-red-600 group-hover:ring-white">
              <Power className="h-6 w-6 text-red-600" />
            </div>
            <div className="text-left">
              <p className="text-base font-black text-red-700 group-hover:text-white transition-colors">긴급 시스템 재시작</p>
              <p className="text-xs font-medium text-red-500 group-hover:text-white/80">크롤러 긴급 복구 시스템</p>
            </div>
          </div>
        </button>
      </div>
    </motion.div>
  );
}
