"use client";

import { Button } from "@/shared/ui/button";
import Link from "next/link";
import { LogOut } from "lucide-react";
import type { ReactNode } from "react";
import type { User } from "@/shared/types/api";

interface UserProfileProps {
  user: User | null;
  isPending?: boolean;
  onLogout?: () => void;
  onLoginClick?: () => void;
  onLinkClick?: () => void;
}

export function HeaderUserActions({ user, isPending, onLogout, onLoginClick, children }: UserProfileProps & { children?: ReactNode }) {
  if (!user) {
    return (
      <Button asChild className="rounded-xl leading-5">
        <Link href="/login" onClick={onLoginClick}>
          로그인
        </Link>
      </Button>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <span className="hidden max-w-20 truncate text-right text-sm leading-5 font-medium text-foreground xl:block 2xl:max-w-28" title={user.name}>{user.name} 님</span>
      {children}
      <Button
        variant="ghost"
        onClick={onLogout}
        disabled={isPending}
        className="hidden rounded-xl leading-5 text-muted-foreground hover:bg-destructive/5 hover:text-destructive xl:inline-flex"
      >
        <LogOut aria-hidden="true" />
        로그아웃
      </Button>
    </div>
  );
}

export function HeaderMobileUserStatus({ user, onLinkClick }: Omit<UserProfileProps, 'isPending' | 'onLogout' | 'onLoginClick'> & { onLinkClick: () => void }) {
  if (!user) {
    return (
      <Button asChild className="h-10 w-full rounded-lg">
        <Link href="/login" onClick={onLinkClick}>
          로그인하고 시작하기
        </Link>
      </Button>
    );
  }

  return (
    <p className="truncate px-3 py-2 text-base font-semibold text-foreground" title={user.name}>
      {user.name} 님
    </p>
  );
}
