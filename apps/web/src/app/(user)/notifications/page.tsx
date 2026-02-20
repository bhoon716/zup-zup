"use client";

import { NotificationList } from "@/features/notification/components/notification-list";
import { SubscriptionList } from "@/features/subscription/components/subscription-list";
import { BellRing } from "lucide-react";

export const dynamic = 'force-dynamic';

/**
 * 알림 히스토리와 구독 목록을 보여주는 페이지입니다.
 * 왼쪽에는 수신된 알림 내역을, 오른쪽에는 현재 구독 중인 강의 목록을 배치합니다.
 */
export default function NotificationsPage() {
  return (
    <div className="min-h-screen bg-[#f8fafc]">
      <main className="max-w-[1600px] mx-auto p-6 md:p-10 lg:p-12">
        <div className="flex flex-col lg:flex-row gap-8 items-start">
          {/* 왼쪽: 알림 히스토리 섹션 */}
          <div className="flex-1 w-full lg:w-auto">
            <header className="mb-8">
              <h1 className="text-3xl font-bold text-slate-900 mb-2 mt-0 tracking-tight">알림 히스토리 및 구독 목록</h1>
              <p className="text-slate-500 text-sm md:text-base font-medium">원하는 강의의 빈자리를 실시간으로 추적하고 알림 내역을 확인하세요.</p>
            </header>
            
            <div className="bg-white rounded-[2rem] shadow-card border border-slate-100 p-6 sm:p-8 min-h-[600px] flex flex-col">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between mb-6 gap-4">
                <h2 className="text-xl font-bold flex items-center gap-2 text-slate-900">
                  <div className="w-10 h-10 rounded-2xl bg-primary/10 flex items-center justify-center">
                    <BellRing className="text-primary w-5 h-5" />
                  </div>
                  알림 히스토리
                </h2>
              </div>
              
              <NotificationList />
            </div>
          </div>

          {/* 오른쪽: 구독 강의 섹션 (사이드바) */}
          <aside className="w-full lg:w-96 shrink-0 flex flex-col gap-6 lg:sticky lg:top-24">
            <SubscriptionList />
          </aside>
        </div>
      </main>
    </div>
  );
}
