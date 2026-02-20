"use client";

import { RefreshCcw } from "lucide-react";
import { Button } from "@/shared/ui/button";

interface AdminOverviewProps {
  serverClock: string;
  onRefresh: () => void;
}

/**
 * 관리자 대시보드의 상단 개요 섹션 컴포넌트입니다.
 * 현재 서버 시간 정보와 수동 새로고침 버튼을 제공합니다.
 */
export function AdminOverview({ serverClock, onRefresh }: AdminOverviewProps) {
  return (
    <section className="bg-white border-b border-slate-100">
      <div className="mx-auto flex max-w-[1920px] flex-col justify-between gap-4 px-4 py-6 sm:gap-6 sm:px-6 sm:py-8 md:flex-row md:items-end lg:px-8">
        <div>
          <div className="flex items-center gap-2 text-primary font-bold text-xs uppercase tracking-widest mb-3">
            <div className="h-1 w-8 bg-primary rounded-full"></div>
            개요
          </div>
          <h1 className="mb-2 text-2xl font-black tracking-tight text-slate-900 sm:text-3xl lg:text-4xl">시스템 현황 개요</h1>
          <p className="text-slate-500 font-medium">
            서버 클럭: <span className="font-mono text-primary font-bold">{serverClock} (KST)</span>
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            className="h-11 gap-2 rounded-xl border-slate-200 bg-white px-4 text-sm font-bold shadow-sm transition-all hover:border-primary/30 hover:bg-primary/5 active:scale-95 sm:h-14 sm:rounded-2xl sm:px-6 sm:text-base"
            onClick={onRefresh}
          >
            <RefreshCcw className="h-5 w-5" />
            데이터 새로고침
          </Button>
        </div>
      </div>
    </section>
  );
}
