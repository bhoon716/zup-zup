"use client";

import { Header } from "@/components/layout/header";
import { NotificationSettings } from "@/components/features/settings/notification-settings";

export default function SettingsPage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="container py-8">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-3xl font-bold mb-8">설정</h1>
          <NotificationSettings />
        </div>
      </main>
    </div>
  );
}
