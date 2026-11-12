"use client";

import { Button } from "@/shared/ui/button";
import Link from "next/link";
import { LogOut } from "lucide-react";
import type { ReactNode } from "react";
import type { User } from "@/shared/types/api";

interface UserProfileProps {
  user: User | null | undefined;
  isLoading: boolean;
  isPending?: boolean;
  onLogout?: () => void;
  onLoginClick?: () => void;
  onLinkClick?: () => void;
}

export function HeaderUserActions({ user, isLoading, isPending, onLogout, onLoginClick, children }: UserProfileProps & { children?: ReactNode }) {
  if (isLoading) return <div className="h-9 w-24 animate-pulse rounded-md bg-muted" />;

  if (!user) {
    return (
      <Button asChild>
        <Link href="/login" onClick={onLoginClick}>
          로그인
        </Link>
      </Button>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <span className="hidden max-w-20 truncate text-right text-sm font-medium text-foreground xl:block 2xl:max-w-28" title={user.name}>{user.name} 님</span>
      {children}
      <Button
        variant="ghost"
        onClick={onLogout}
        disabled={isPending}
        className="hidden text-muted-foreground hover:bg-destructive/5 hover:text-destructive xl:inline-flex"
      >
        <LogOut aria-hidden="true" />
        로그아웃
      </Button>
    </div>
  );
}

export function HeaderMobileUserStatus({ user, isLoading, onLinkClick }: Omit<UserProfileProps, 'isPending' | 'onLogout' | 'onLoginClick'> & { onLinkClick: () => void }) {
  if (isLoading) return <div className="h-10 w-full animate-pulse bg-muted/50 rounded-xl" />;

  if (!user) {
    return (
      <Button asChild className="w-full gap-2 rounded-xl h-11 bg-primary shadow-lg shadow-primary/20">
        <Link href="/login" onClick={onLinkClick}>
          로그인하고 시작하기
        </Link>
      </Button>
    );
  }

  return (
    <div className="bg-accent/30 rounded-2xl p-4 border border-white/5">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 shrink-0 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
          {user.name.charAt(0)}
        </div>
        <div>
          <p className="text-sm font-bold wrap-anywhere">{user.name} 님</p>
          <p className="text-[10px] text-muted-foreground italic">환영합니다!</p>
        </div>
      </div>
    </div>
  );
}
