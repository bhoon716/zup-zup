"use client";

// Header removed (Global layout usage)
import { NotificationList } from "@/components/features/notification/notification-list";

export const dynamic = 'force-dynamic';

export default function NotificationsPage() {
  return (
    <div className="min-h-screen bg-background">
      <main className="container max-w-4xl py-8 px-4 md:px-6">
        <NotificationList />
      </main>
    </div>
  );
}
