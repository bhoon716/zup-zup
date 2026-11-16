"use client";

import { Loader2 } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import type { User } from "@/shared/types/api";
import { Button } from "@/shared/ui/button";

export function ProfileScreen({ children, title, navigation }: {
  children: (user: User) => ReactNode;
  title: string;
  navigation?: ReactNode;
}) {
  const user = useAuthStore((state) => state.isAuthenticated ? state.user : null);
  const hasCheckedSession = useAuthStore((state) => state.hasCheckedSession);

  return (
    <main className="mx-auto w-full max-w-2xl px-4 pb-12 pt-6 sm:px-6 sm:pt-9">
      {navigation}
      <h1 className="mb-5 text-2xl font-semibold tracking-tight sm:mb-6 sm:text-3xl">{title}</h1>
      {user && children(user)}
      {!user && !hasCheckedSession && (
        <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 aria-hidden="true" className="size-4 animate-spin text-primary" />
          로그인 상태를 확인하는 중입니다.
        </p>
      )}
      {!user && hasCheckedSession && (
        <div className="space-y-3 rounded-xl border bg-card p-4 sm:p-5">
          <p className="text-sm text-muted-foreground">설정을 보려면 로그인하세요.</p>
          <Button asChild>
            <Link href="/login">로그인</Link>
          </Button>
        </div>
      )}
    </main>
  );
}
