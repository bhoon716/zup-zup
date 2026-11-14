"use client";

import { Button } from "@/shared/ui/button";
import Link from "next/link";
import { LogOut, UserRound } from "lucide-react";
import type { ReactNode } from "react";

interface HeaderUserActionsProps {
  isLoggedIn: boolean;
  isPending: boolean;
  onLogout: () => void;
  onLoginClick: () => void;
  children?: ReactNode;
}

export function HeaderUserActions({ isLoggedIn, isPending, onLogout, onLoginClick, children }: HeaderUserActionsProps) {
  if (!isLoggedIn) {
    return (
      <Button asChild className="rounded-xl leading-5">
        <Link href="/login" onClick={onLoginClick}>
          로그인
        </Link>
      </Button>
    );
  }

  return (
    <div className="flex items-center gap-2 xl:gap-3">
      <Button asChild variant="ghost" size="icon-lg" className="rounded-[8px] text-muted-foreground hover:bg-primary/10 hover:text-primary xl:size-9">
        <Link href="/profile" aria-label="설정" title="설정">
          <UserRound aria-hidden="true" className="size-5 xl:size-4" />
        </Link>
      </Button>
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
