"use client";

import { useState } from "react";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { InboxList } from "@/features/notification/components/inbox-list";
import { NotificationList } from "@/features/notification/components/notification-list";
import { SubscriptionList } from "@/features/subscription/components/subscription-list";
import { Button } from "@/shared/ui/button";

export const dynamic = "force-dynamic";

export default function NotificationsPage() {
  const userId = useAuthStore((state) => state.isAuthenticated ? state.user?.id : undefined);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);

  return (
    <div className="min-h-screen bg-slate-50">
      <main className="mx-auto max-w-[1600px] p-4 sm:p-6 md:p-10 lg:p-12">
        <h1 className="mb-5 text-3xl font-bold tracking-tight text-slate-900 sm:mb-8">알림함</h1>
        {userId === undefined ? (
          <p role="status" className="text-sm text-slate-600">
            {hasCheckedSession ? "로그인하면 알림을 확인할 수 있습니다." : "로그인 상태를 확인하는 중입니다."}
          </p>
        ) : (
          <AccountNotifications key={userId} userId={userId} />
        )}
      </main>
    </div>
  );
}

function AccountNotifications({ userId }: { userId: number }) {
  const [view, setView] = useState<"inbox" | "history">("inbox");

  return (
    <div className="flex flex-col items-start gap-6 lg:flex-row lg:gap-8">
      <section aria-label={view === "inbox" ? "알림함" : "발송 이력"} className="w-full min-w-0 flex-1 rounded-2xl border border-slate-100 bg-white p-3 shadow-card sm:rounded-[2rem] sm:p-8">
        <div role="group" aria-label="알림 보기" className="mb-5 flex gap-1 border-b border-slate-100 pb-3">
          <Button type="button" variant={view === "inbox" ? "secondary" : "ghost"} aria-pressed={view === "inbox"} onClick={() => setView("inbox")}>알림함</Button>
          <Button type="button" variant={view === "history" ? "secondary" : "ghost"} aria-pressed={view === "history"} onClick={() => setView("history")}>발송 이력</Button>
        </div>
        {view === "inbox" ? <InboxList userId={userId} /> : <NotificationList />}
      </section>
      <aside className="w-full shrink-0 lg:sticky lg:top-24 lg:w-96">
        <SubscriptionList />
      </aside>
    </div>
  );
}
