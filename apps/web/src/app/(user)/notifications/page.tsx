"use client";

import { useRef, useState, type KeyboardEvent } from "react";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { InboxList } from "@/features/notification/components/inbox-list";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";

export const dynamic = "force-dynamic";

export default function NotificationsPage() {
  const userId = useAuthStore((state) => state.isAuthenticated ? state.user?.id : undefined);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);

  return (
    <div className="min-h-[calc(100dvh-4rem)] bg-background">
      <main className="font-schedule mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-10">
        <h1 className="mb-5 text-2xl font-semibold text-foreground sm:text-3xl">알림함</h1>
        {userId === undefined ? (
          <p role="status" className="text-sm text-muted-foreground">
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
  { id: "history", label: "빈자리 알림" },
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
    inbox: <InboxList userId={userId} view="INBOX" />,
    history: <InboxList userId={userId} view="SEAT_ALERTS" />,
  };

  return (
    <div className="w-full min-w-0 rounded-xl border bg-card">
      <div role="tablist" aria-label="알림 보기" className="flex flex-wrap gap-4 border-b px-4 sm:gap-6 sm:px-5">
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
            variant="ghost"
            aria-selected={view === id}
            aria-controls={`notification-panel-${id}`}
            tabIndex={view === id ? 0 : -1}
            className={cn(
              "h-12 rounded-none border-b-2 border-transparent px-0.5 text-muted-foreground hover:bg-transparent hover:text-foreground",
              view === id && "border-primary font-semibold text-primary hover:text-primary",
            )}
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
