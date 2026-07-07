"use client";

import { motion } from "framer-motion";
import { Database, RefreshCcw, Send, Loader2 } from "lucide-react";

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
      className="flex flex-col rounded-3xl border border-slate-100 bg-white p-6 shadow-2xl shadow-slate-200/50 lg:rounded-[3rem] lg:p-10"
    >
      <h3 className="mb-8 text-2xl font-black text-slate-900 tracking-tight">퀵 컨트롤 센터</h3>
      <div className="flex flex-1 flex-col gap-5">
        {actions.map((action) => (
          <button
            key={action.label}
            type="button"
            className="group relative h-24 w-full overflow-hidden rounded-2xl border border-slate-100 bg-slate-50/50 transition-all hover:scale-[1.01] hover:border-primary/30 hover:bg-white hover:shadow-xl hover:shadow-primary/5 lg:h-28 lg:rounded-[2rem] lg:hover:scale-[1.02]"
            onClick={action.onClick}
            disabled={action.loading}
          >
            <div className="flex items-center gap-4 px-5 lg:gap-6 lg:px-7">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white shadow-sm ring-1 ring-slate-200/50 transition-colors group-hover:bg-primary group-hover:text-white group-hover:ring-primary lg:h-14 lg:w-14">
                {action.loading ? <Loader2 className="h-6 w-6 animate-spin" /> : <action.icon className="h-6 w-6" />}
              </div>
              <div className="text-left">
                <p className="text-sm font-black text-slate-900 transition-colors group-hover:text-primary lg:text-base">{action.label}</p>
                <p className="text-xs font-medium text-slate-500 group-hover:text-slate-600">{action.desc}</p>
              </div>
            </div>
          </button>
        ))}
      </div>
    </motion.div>
  );
}
