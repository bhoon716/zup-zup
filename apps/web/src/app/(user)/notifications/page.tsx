"use client";

import { useRef, useState, type KeyboardEvent } from "react";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { InboxList } from "@/features/notification/components/inbox-list";
import { NotificationList } from "@/features/notification/components/notification-list";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";

export const dynamic = "force-dynamic";

export default function NotificationsPage() {
  const userId = useAuthStore((state) => state.isAuthenticated ? state.user?.id : undefined);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);

  return (
    <div className="min-h-screen bg-slate-50">
      <main className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
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

const notificationViews = [
  { id: "inbox", label: "알림함" },
  { id: "history", label: "발송 이력" },
] as const;

type NotificationView = (typeof notificationViews)[number]["id"];

function AccountNotifications({ userId }: { userId: number }) {
  const [view, setView] = useState<NotificationView>("inbox");
  const tabs = useRef(new Map<NotificationView, HTMLButtonElement>());

  const moveTab = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    const count = notificationViews.length;
    let nextIndex: number;
    switch (event.key) {
      case "ArrowLeft":
        nextIndex = (index - 1 + count) % count;
        break;
      case "ArrowRight":
        nextIndex = (index + 1) % count;
        break;
      case "Home":
        nextIndex = 0;
        break;
      case "End":
        nextIndex = count - 1;
        break;
      default:
        return;
    }
    event.preventDefault();
    const next = notificationViews[nextIndex].id;
    setView(next);
    tabs.current.get(next)?.focus();
  };

  const panels = {
    inbox: <InboxList userId={userId} />,
    history: <div className="p-3 sm:p-5"><NotificationList /></div>,
  };

  return (
    <div className="w-full min-w-0 rounded-2xl border border-slate-200 bg-white shadow-card">
      <div role="tablist" aria-label="알림 보기" className="flex flex-wrap gap-1 rounded-t-2xl border-b border-slate-200 px-3 py-2 sm:px-5">
        {notificationViews.map(({ id, label }, index) => (
          <Button
            key={id}
            ref={(node) => {
              if (node) tabs.current.set(id, node);
              else tabs.current.delete(id);
            }}
            id={`notification-tab-${id}`}
            type="button"
            role="tab"
            variant={view === id ? "secondary" : "ghost"}
            aria-selected={view === id}
            aria-controls={`notification-panel-${id}`}
            tabIndex={view === id ? 0 : -1}
            className={cn("rounded-lg px-3 sm:px-4", view === id && "bg-primary/10 font-semibold text-primary hover:bg-primary/15")}
            onClick={() => setView(id)}
            onKeyDown={(event) => moveTab(event, index)}
          >
            {label}
          </Button>
        ))}
      </div>
      {notificationViews.map(({ id }) => (
        <section
          key={id}
          id={`notification-panel-${id}`}
          role="tabpanel"
          aria-labelledby={`notification-tab-${id}`}
          hidden={view !== id}
          tabIndex={0}
        >
          {view === id && panels[id]}
        </section>
      ))}
    </div>
  );
}
