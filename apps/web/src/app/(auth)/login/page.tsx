"use client";

import { LoginCard } from "@/features/auth/components/login-card";

export const dynamic = 'force-dynamic';

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <LoginCard />
    </div>
  );
}
