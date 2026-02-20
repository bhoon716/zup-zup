"use client";

import { motion } from "framer-motion";
import { LucideIcon } from "lucide-react";
import { cn } from "@/shared/lib/utils";

interface AdminStatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  icon: LucideIcon;
  color: "primary" | "amber" | "green" | "indigo";
  meta?: {
    label: string;
    badgeClass: string;
  };
  badge?: string;
  index: number;
}

/**
 * 관리자 대시보드 지표 카드 컴포넌트입니다.
 * 각 지표의 현재 수치와 상태 정보를 역동적인 애니메이션과 함께 표시합니다.
 */
export function AdminStatCard({
  label,
  value,
  sub,
  icon: Icon,
  color,
  meta,
  badge,
  index,
}: AdminStatCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 30 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1 }}
      className="group relative overflow-hidden rounded-[2.5rem] border border-slate-100 bg-white p-8 shadow-2xl shadow-slate-200/50 transition-all hover:-translate-y-1 hover:border-primary/20"
    >
      <div className="mb-8 flex items-start justify-between">
        <div
          className={cn(
            "flex h-14 w-14 items-center justify-center rounded-2xl transition-colors",
            color === "primary" ? "bg-primary/10 text-primary" :
            color === "amber" ? "bg-amber-100 text-amber-600" :
            color === "green" ? "bg-green-100 text-green-600" :
            "bg-indigo-100 text-indigo-600"
          )}
        >
          <Icon className="h-7 w-7" />
        </div>
        
        {meta ? (
          <span
            className={cn(
              "inline-flex rounded-xl px-3 py-1 text-xs font-bold ring-1 ring-inset",
              value === "RUNNING" ? "bg-green-50 text-green-600 ring-green-500/20" :
              value === "DEGRADED" ? "bg-amber-50 text-amber-600 ring-amber-500/20" :
              "bg-slate-50 text-slate-500 ring-slate-500/20"
            )}
          >
            {meta.label}
          </span>
        ) : badge ? (
          <span className="rounded-xl bg-primary px-3 py-1 text-[10px] font-black uppercase text-white shadow-lg shadow-primary/20">
            {badge}
          </span>
        ) : null}
      </div>

      <p className="text-sm font-bold text-slate-400 uppercase tracking-widest mb-1">{label}</p>
      <p className="text-4xl font-black text-slate-900 tracking-tight">{value}</p>
      {sub && (
        <p className="mt-4 text-xs font-medium text-slate-500 flex items-center gap-1.5">
          {sub}
        </p>
      )}
      <div className="absolute -bottom-6 -right-6 h-32 w-32 rounded-full bg-slate-50/50 transition-transform group-hover:scale-150" />
    </motion.div>
  );
}
