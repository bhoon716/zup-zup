"use client";

import { motion } from "framer-motion";

interface TrafficPoint {
  label: string;
  count: number;
}

interface AdminTrafficChartProps {
  traffic: TrafficPoint[];
}

/**
 * 관리자 대시보드 알림 트래픽 시각화 컴포넌트입니다.
 * 최근 24시간 동안의 발송 패턴을 역동적인 막대 그래프로 표시합니다.
 */
export function AdminTrafficChart({ traffic }: AdminTrafficChartProps) {
  const maxTraffic = Math.max(...traffic.map((point) => point.count), 1);

  return (
    <motion.div
      initial={{ opacity: 0, x: -30 }}
      animate={{ opacity: 1, x: 0 }}
      className="lg:col-span-2 rounded-[3rem] border border-slate-100 bg-white p-10 shadow-2xl shadow-slate-200/50 overflow-hidden"
    >
      <div className="mb-10 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-2xl font-black text-slate-900 tracking-tight">알림 트래픽 분석</h3>
          <p className="text-slate-500 font-medium mt-1">최근 24시간 동안의 발송 패턴입니다.</p>
        </div>
        <div className="flex h-12 items-center gap-1 rounded-2xl bg-slate-50 p-1">
          <button className="h-full rounded-xl bg-white px-5 text-sm font-bold text-primary shadow-sm">24H</button>
          <button className="h-full rounded-xl px-5 text-sm font-bold text-slate-500 hover:text-primary transition-colors">7D</button>
        </div>
      </div>

      <div className="relative h-80 w-full group/chart pt-8">
        <div className="absolute inset-0 flex flex-col justify-between py-1">
          {[1, 2, 3, 4, 5].map((_, idx) => (
            <div key={idx} className="h-px w-full border-t border-dashed border-slate-100" />
          ))}
        </div>
        <div className="relative z-10 flex h-full items-end gap-2 px-2">
          {traffic.map((point, idx) => {
            const ratio = point.count / maxTraffic;
            const heightPercent = Math.max(4, Math.round(ratio * 100));
            return (
              <div key={`${point.label}-${idx}`} className="group/bar relative flex-1 h-full flex items-end">
                <motion.div
                  initial={{ height: 0 }}
                  animate={{ height: `${heightPercent}%` }}
                  transition={{ delay: idx * 0.02, duration: 0.5, ease: "easeOut" }}
                  className="w-full rounded-2xl bg-linear-to-t from-primary/80 to-primary transition-all group-hover/bar:from-primary group-hover/bar:to-primary-dark group-hover/bar:scale-x-110 shadow-lg shadow-primary/5"
                />
                <div className="absolute -top-10 left-1/2 -translate-x-1/2 rounded-lg bg-slate-900 px-2 py-1 text-[10px] font-bold text-white opacity-0 group-hover/bar:opacity-100 transition-opacity whitespace-nowrap z-20">
                  {point.label} · {point.count}건
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="mt-8 flex justify-between px-2 font-mono text-[10px] font-bold text-slate-400 uppercase tracking-widest">
        <span>{traffic[0]?.label ?? "-"}</span>
        <span>{traffic[Math.floor(traffic.length * 0.5)]?.label ?? "-"}</span>
        <span>{traffic[traffic.length - 1]?.label ?? "-"}</span>
      </div>
    </motion.div>
  );
}
