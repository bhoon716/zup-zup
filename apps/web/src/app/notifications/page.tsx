"use client";

// Header removed (Global layout usage)
import { NotificationList } from "@/components/features/notification/notification-list";

export const dynamic = 'force-dynamic';

export default function NotificationsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      
      <main className="container py-8">
        <NotificationList />
      </main>
    </div>
  );
}
