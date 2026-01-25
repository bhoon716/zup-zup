"use client";

import { useUser, useLogout } from "@/hooks/useUser";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { LogOut, Search, Home, Bell, Settings } from "lucide-react";

export function Header() {
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();

  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="container flex h-16 items-center justify-between">
        <div className="flex items-center gap-6">
          <Link href="/" className="flex items-center gap-2 font-bold text-xl">
            <span className="text-blue-600">Sugang Helper</span>
          </Link>
          <nav className="flex items-center gap-4">
            <Link href="/">
              <Button variant="ghost" size="sm" className="gap-2">
                <Home className="w-4 h-4" />
                대시보드
              </Button>
            </Link>
            <Link href="/search">
              <Button variant="ghost" size="sm" className="gap-2">
                <Search className="w-4 h-4" />
                강좌 검색
              </Button>
            </Link>
            <Link href="/notifications">
              <Button variant="ghost" size="sm" className="gap-2">
                <Bell className="w-4 h-4" />
                알림 내역
              </Button>
            </Link>
            <Link href="/settings">
              <Button variant="ghost" size="sm" className="gap-2">
                <Settings className="w-4 h-4" />
                설정
              </Button>
            </Link>
          </nav>
        </div>

        <div className="flex items-center gap-4">
          {isLoading ? (
            <div className="h-8 w-24 animate-pulse bg-gray-200 rounded" />
          ) : user ? (
            <>
              <span className="text-sm text-muted-foreground">{user.name}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => logout()}
                disabled={isPending}
                className="gap-2"
              >
                <LogOut className="w-4 h-4" />
                로그아웃
              </Button>
            </>
          ) : null}
        </div>
      </div>
    </header>
  );
}
