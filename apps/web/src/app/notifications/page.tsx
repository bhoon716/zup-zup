"use client";

// Header removed (Global layout usage)
import { NotificationList } from "@/components/features/notification/notification-list";

export const dynamic = 'force-dynamic';

export default function NotificationsPage() {
  // For the purpose of demonstrating the title and count,
  // we'll use a placeholder for `data.length`.
  // In a real application, `data` would be fetched or passed down.
  const notificationCount = 0; // Placeholder: Replace with actual count from fetched data or NotificationList props

  return (
    <div className="min-h-screen bg-background">
      
      <main className="container max-w-4xl py-8 px-4 md:px-6">
        <div className="flex items-center justify-between mb-2">
          <div>
            <h2 className="text-xl md:text-2xl font-bold tracking-tight">알림 내역</h2>
            <p className="text-[11px] md:text-sm text-muted-foreground font-medium mt-0.5">
              총 {notificationCount}개의 알림 수신 내역이 있습니다.
            </p>
          </div>
        </div>
        <NotificationList />
      </main>
    </div>
  );
}
