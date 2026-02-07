"use client";

import { useUser, useLogout } from "@/hooks/useUser";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { LogOut, Search, Home, Bell, Settings, ShieldCheck, Calendar } from "lucide-react";

import { useAuthStore } from "@/store/useAuthStore";

export function Header() {
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const handleGuardedAction = (e: React.MouseEvent, href: string) => {
    if (!user) {
      e.preventDefault();
      setLoginModalOpen(true);
    }
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/5 bg-background/60 backdrop-blur-xl supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2.5 font-bold text-xl tracking-tighter group transition-all active:scale-95">
            <div className="relative w-8 h-8 flex-shrink-0 group-hover:scale-110 transition-transform duration-300">
              <img 
                src="/jbnu-logo.png" 
                alt="전북대학교 로고" 
                className="w-full h-full object-contain drop-shadow-md"
              />
            </div>
            <span className="bg-gradient-to-r from-[#56296e] to-[#7c4d91] bg-clip-text text-transparent">수강신청 도우미</span>
          </Link>
          <nav className="hidden md:flex items-center gap-1">
            <Link href="/timetable" onClick={(e) => handleGuardedAction(e, "/timetable")}>
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground">
                <Calendar className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">내 시간표</span>
              </Button>
            </Link>
            <Link href="/search">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground">
                <Search className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">강의 검색</span>
              </Button>
            </Link>
            <Link href="/notifications" onClick={(e) => handleGuardedAction(e, "/notifications")}>
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground">
                <Bell className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">알림 내역</span>
              </Button>
            </Link>
            <Link href="/settings" onClick={(e) => handleGuardedAction(e, "/settings")}>
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground">
                <Settings className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">설정</span>
              </Button>
            </Link>
            {user?.role === 'ADMIN' && (
              <Link href="/admin">
                <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 text-[#56296e]/70 hover:text-[#56296e] hover:bg-[#56296e]/5">
                  <ShieldCheck className="w-4 h-4" />
                  <span className="font-medium">관리자</span>
                </Button>
              </Link>
            )}
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {isLoading ? (
            <div className="h-8 w-24 animate-pulse bg-muted/50 rounded-xl" />
          ) : user ? (
            <div className="flex items-center gap-3">
              <div className="hidden sm:flex flex-col items-end mr-1">
                <span className="text-xs font-bold leading-none">{user.name} 님</span>
                <span className="text-[10px] text-muted-foreground mt-1">Logged in</span>
              </div>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => logout()}
                disabled={isPending}
                className="gap-2 rounded-xl h-9 hover:bg-destructive/5 hover:text-destructive transition-colors"
              >
                <LogOut className="w-4 h-4" />
                로그아웃
              </Button>
            </div>
          ) : (
            <Link href="/login">
              <Button size="sm" className="gap-2 rounded-xl px-5 h-9 bg-primary shadow-lg shadow-primary/20 hover:shadow-primary/30 active:scale-95 transition-all">
                로그인
              </Button>
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
