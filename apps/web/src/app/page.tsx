"use client";

import { Header } from "@/components/layout/header";
import { SubscriptionList } from "@/components/features/subscription/subscription-list";

export const dynamic = 'force-dynamic';

export default function HomePage() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="container py-10">
        <SubscriptionList />
      </main>
    </div>
  );
}
