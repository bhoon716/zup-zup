"use client";

import { Loader2, RefreshCcw, Send } from "lucide-react";
import { Button } from "@/shared/ui/button";

interface AdminOverviewProps {
  serverClock: string;
  onRefresh: () => void;
  onSendTest: () => void;
  isSendingTest: boolean;
}

export function AdminOverview({ serverClock, onRefresh, onSendTest, isSendingTest }: AdminOverviewProps) {
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
        <div className="flex flex-wrap items-center gap-2">
          <Button
            type="button"
            variant="outline"
            className="h-10 rounded-xl px-3"
            onClick={onSendTest}
            disabled={isSendingTest}
          >
            {isSendingTest ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
            자가 알림 테스트
          </Button>
          <Button
            type="button"
            variant="outline"
            className="h-10 rounded-xl px-3"
            onClick={onRefresh}
          >
            <RefreshCcw className="size-4" />
            데이터 새로고침
          </Button>
        </div>
      </div>
    </section>
  );
}
