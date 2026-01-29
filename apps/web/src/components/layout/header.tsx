"use client";

import { useUser, useLogout } from "@/hooks/useUser";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { LogOut, Search, Home, Bell, Settings, ShieldCheck, Calendar } from "lucide-react";

export function Header() {
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/5 bg-background/60 backdrop-blur-xl supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl tracking-tighter">
            <span className="bg-gradient-to-r from-blue-600 to-indigo-500 bg-clip-text text-transparent">Sugang Helper</span>
          </Link>
          <nav className="hidden md:flex items-center gap-1">
            <Link href="/">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9">
                <Home className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">대시보드</span>
              </Button>
            </Link>
            <Link href="/timetable">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9">
                <Calendar className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">내 시간표</span>
              </Button>
            </Link>
            <Link href="/search">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9">
                <Search className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">강의 검색</span>
              </Button>
            </Link>
            <Link href="/notifications">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9">
                <Bell className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">알림 내역</span>
              </Button>
            </Link>
            <Link href="/settings">
              <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9">
                <Settings className="w-4 h-4 text-muted-foreground" />
                <span className="font-medium">설정</span>
              </Button>
            </Link>
            {user?.role === 'ADMIN' && (
              <Link href="/admin">
                <Button variant="ghost" size="sm" className="gap-2 rounded-xl px-3 h-9 text-blue-500 hover:text-blue-600 hover:bg-blue-500/5">
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
