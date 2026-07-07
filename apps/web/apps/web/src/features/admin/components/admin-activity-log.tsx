"use client";

import { motion } from "framer-motion";
import { Download } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";

interface AdminLog {
  timestamp: string;
  level: string;
  message: string;
  source: string;
}

interface AdminActivityLogProps {
  logs: AdminLog[];
  formatDateTime: (value?: string | null) => string;
  getLogMeta: (level: string) => { label: string; className: string };
}

/**
 * 시스템 활동 로그 테이블 컴포넌트입니다.
 * 실시간으로 발생하는 시스템 이벤트와 오류 정보를 가독성 있게 표시합니다.
 */
export function AdminActivityLog({
  logs,
  formatDateTime,
  getLogMeta,
}: AdminActivityLogProps) {
  return (
    <section className="space-y-8">
      <div className="flex flex-col gap-4 px-1 sm:flex-row sm:items-end sm:justify-between sm:gap-6 sm:px-4">
        <div>
          <h3 className="text-2xl font-black tracking-tight text-slate-900 sm:text-3xl">시스템 활동 로그</h3>
          <p className="text-slate-500 font-medium mt-1">실시간 오류 정보와 시스템 중요 이벤트입니다.</p>
        </div>
        <div className="flex gap-2 sm:gap-4">
          <Button variant="outline" className="h-10 gap-2 rounded-xl border-slate-200 bg-white px-3 text-xs font-bold shadow-sm hover:bg-slate-50 sm:h-14 sm:rounded-2xl sm:px-6 sm:text-sm">
            <Download className="h-5 w-5" />
            <span className="hidden sm:inline">CSV 리포트</span>
            <span className="sm:hidden">CSV</span>
          </Button>
          <Button className="h-10 rounded-xl bg-slate-900 px-4 text-xs font-bold text-white shadow-xl shadow-slate-900/10 hover:bg-slate-800 sm:h-14 sm:rounded-2xl sm:px-8 sm:text-sm">
            전체 기록 보기
          </Button>
        </div>
      </div>

      <div className="overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-2xl shadow-slate-200/50 sm:rounded-[3rem]">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50/50 border-b border-slate-100 text-[10px] font-black uppercase text-slate-400 tracking-[0.15em]">
              <tr>
                <th className="px-10 py-6">발생 시간</th>
                <th className="px-8 py-6">레벨</th>
                <th className="px-8 py-6">메시지</th>
                <th className="px-8 py-6">발생 위치</th>
                <th className="px-10 py-6 text-right">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {logs.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-10 py-20 text-center text-slate-400 font-medium italic">
                    현재 기록된 시스템 로그 데이터가 비어 있습니다.
                  </td>
                </tr>
              ) : (
                logs.map((log, index) => {
                  const logMeta = getLogMeta(log.level);
                  return (
                    <motion.tr
                      key={`${log.timestamp}-${index}`}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ delay: index * 0.05 }}
                      className="group transition-colors hover:bg-slate-50/50"
                    >
                      <td className="whitespace-nowrap px-10 py-6 font-mono text-[11px] font-bold text-slate-400">
                        {formatDateTime(log.timestamp)}
                      </td>
                      <td className="px-8 py-6">
                        <span className={cn("inline-flex rounded-lg px-2.5 py-1 text-[10px] font-black uppercase tracking-wider",
                          log.level === "ERROR" ? "bg-red-50 text-red-600" :
                          log.level === "WARN" ? "bg-amber-50 text-amber-600" : "bg-green-50 text-green-600"
                        )}>
                          {logMeta.label}
                        </span>
                      </td>
                      <td className="px-8 py-6 font-bold text-slate-700 max-w-md truncate">
                        {log.message}
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-2">
                          <span className="h-1.5 w-1.5 rounded-full bg-slate-300" />
                          <span className="font-bold text-[11px] text-slate-400 uppercase tracking-widest">{log.source}</span>
                        </div>
                      </td>
                      <td className="px-10 py-6 text-right">
                        <button type="button" className="rounded-xl border border-slate-200 bg-white px-4 py-2 text-[10px] font-black text-slate-400 transition-all hover:border-primary/30 hover:text-primary hover:shadow-sm">
                          상세 보기
                        </button>
                      </td>
                    </motion.tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </section>
  );
}
