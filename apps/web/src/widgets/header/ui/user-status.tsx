"use client";

import { Button } from "@/shared/ui/button";
import Link from "next/link";
import { LogOut } from "lucide-react";
import type { User } from "@/shared/types/api";

interface UserProfileProps {
  user: User | null | undefined;
  isLoading: boolean;
  isPending?: boolean;
  onLogout?: () => void;
  onLoginClick?: () => void;
  onLinkClick?: () => void;
}

/**
 * 데스크톱 환경의 사용자 프로필 및 로그아웃 버튼을 렌더링합니다.
 */
export function HeaderDesktopUser({ user, isLoading, isPending, onLogout, onLoginClick }: UserProfileProps) {
  if (isLoading) return <div className="h-8 w-24 animate-pulse bg-muted/50 rounded-xl" />;
  
  if (!user) {
    return (
      <Link href="/login" onClick={onLoginClick}>
        <Button size="sm" className="bg-primary hover:bg-primary-dark text-white text-sm font-medium py-2 px-5 rounded-lg transition-colors shadow-sm">
          로그인
        </Button>
      </Link>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <div className="hidden sm:flex flex-col items-end mr-1">
        <span className="text-xs font-bold leading-none">{user.name} 님</span>
        <span className="text-[10px] text-muted-foreground mt-1">로그인 됨</span>
      </div>
      <Button
        variant="ghost"
        size="sm"
        onClick={onLogout}
        disabled={isPending}
        className="gap-2 rounded-xl h-9 hover:bg-destructive/5 hover:text-destructive transition-colors"
      >
        <LogOut className="w-4 h-4" />
        로그아웃
      </Button>
    </div>
  );
}

/**
 * 모바일 환경의 사용자 상태(프로필/로그인 유도)를 렌더링합니다.
 */
export function HeaderMobileUserStatus({ user, isLoading, onLinkClick }: Omit<UserProfileProps, 'isPending' | 'onLogout' | 'onLoginClick'> & { onLinkClick: () => void }) {
  if (isLoading) return <div className="h-10 w-full animate-pulse bg-muted/50 rounded-xl" />;

  if (!user) {
    return (
      <Link href="/login" className="block" onClick={onLinkClick}>
        <Button className="w-full gap-2 rounded-xl h-11 bg-primary shadow-lg shadow-primary/20">
          로그인하고 시작하기
        </Button>
      </Link>
    );
  }

  return (
    <div className="bg-accent/30 rounded-2xl p-4 border border-white/5">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
          {user.name.charAt(0)}
        </div>
        <div>
          <p className="font-bold text-sm">{user.name} 님</p>
          <p className="text-[10px] text-muted-foreground italic">환영합니다!</p>
        </div>
      </div>
    </div>
  );
}
