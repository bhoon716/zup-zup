"use client";

import { LoginCard } from "@/components/features/auth/login-card";

export const dynamic = 'force-dynamic';

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800">
      <LoginCard />
    </div>
  );
}
