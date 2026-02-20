"use client";

import type { MouseEvent } from "react";
import { useUser, useLogout } from "@/features/user/hooks/useUser";
import { Button } from "@/shared/ui/button";
import Link from "next/link";
import Image from "next/image";
import { cn } from "@/shared/lib/utils";
import { 
  LogOut, Search, Bell, Settings, ShieldCheck, Calendar, 
  Menu, Download, GraduationCap, LayoutDashboard, ChevronDown 
} from "lucide-react";
import { usePathname } from "next/navigation";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/shared/ui/sheet";
import { usePWAInstall } from "@/shared/hooks/usePWAInstall";

interface NavLinksProps {
  isMobile?: boolean;
  isAdmin: boolean;
  onGuardedAction: (e: MouseEvent) => void;
  onLinkClick?: () => void;
}

/**
 * 메인 내비게이션 링크들을 렌더링하는 컴포넌트입니다.
 * 데스크톱과 모바일 환경에 적합한 레이아웃을 제공하며, 권한에 따른 관리자 메뉴를 포함합니다.
 */
function NavLinks({ isMobile = false, isAdmin, onGuardedAction, onLinkClick }: NavLinksProps) {
  const handleClick = (e: MouseEvent) => {
    onLinkClick?.();
    onGuardedAction(e);
  };

  return (
    <>
      <Link href="/timetable" onClick={handleClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Calendar className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">내 시간표</span>
        </Button>
      </Link>
      <Link href="/search" onClick={onLinkClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Search className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">강의 검색</span>
        </Button>
      </Link>
      <Link href="/notifications" onClick={handleClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Bell className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">알림 / 구독</span>
        </Button>
      </Link>
      <Link href="/settings" onClick={handleClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Settings className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">설정</span>
        </Button>
      </Link>
      {isAdmin && (
        <div className={cn(isMobile ? "mt-4 space-y-1 pt-4 border-t border-gray-100" : "flex items-center")}>
          {isMobile ? (
            <>
              <p className="px-4 py-2 text-[10px] font-bold text-gray-400 uppercase tracking-widest flex items-center gap-2">
                <ShieldCheck className="w-3.5 h-3.5" />
                관리 메뉴
              </p>
              <div className="flex flex-col gap-0.5 pl-3">
                <Link href="/admin" onClick={onLinkClick}>
                  <Button variant="ghost" size="sm" className="w-full justify-start gap-3 h-11 px-4 text-sm font-semibold hover:bg-primary/5 text-primary">
                    <LayoutDashboard className="w-4 h-4" />
                    대시보드
                  </Button>
                </Link>
              </div>
            </>
          ) : (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="gap-1.5 rounded-xl px-3 h-9 text-primary font-bold hover:bg-primary/5">
                  <ShieldCheck className="w-[1.1rem] h-[1.1rem]" />
                  <span className="text-sm">관리자</span>
                  <ChevronDown className="w-3.5 h-3.5 opacity-50" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56 rounded-2xl border-gray-100 bg-white/95 backdrop-blur-xl p-2 shadow-2xl">
                <DropdownMenuLabel className="px-3 py-2 text-[10px] font-bold text-primary uppercase tracking-widest">시스템 관리자</DropdownMenuLabel>
                <DropdownMenuSeparator className="bg-gray-100" />
                <Link href="/admin">
                  <DropdownMenuItem className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                    <LayoutDashboard className="w-4 h-4" />
                    대시보드
                  </DropdownMenuItem>
                </Link>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      )}
    </>
  );
}

/**
 * 애플리케이션의 최상단 공통 헤더 컴포넌트입니다.
 * 서비스 로고, 내비게이션 메뉴, PWA 설치 유도 및 사용자 인증 상태(로그인/로그아웃)를 관리합니다.
 */
export function Header() {
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);
  const pathname = usePathname();
  const { isInstallable, install } = usePWAInstall();
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  if (pathname === "/onboarding") {
    return null;
  }

  const handleGuardedAction = (e: MouseEvent) => {
    if (!user) {
      e.preventDefault();
      setLoginModalOpen(true);
    }
  };

  const closeMenu = () => setIsMenuOpen(false);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-gray-100 bg-white/90 backdrop-blur-md">
      <div className="container flex h-16 items-center justify-between">
        <div className="flex items-center gap-8">
          <Link href="/" className="flex items-center gap-2 group transition-all active:scale-95">
            <GraduationCap className="text-primary w-8 h-8 group-hover:scale-110 transition-transform" />
            <span className="font-bold text-xl text-primary tracking-tight">전북대 수강신청 도우미</span>
          </Link>
          <nav className="hidden md:flex items-center gap-1">
            <NavLinks isAdmin={user?.role === "ADMIN"} onGuardedAction={handleGuardedAction} />
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {isInstallable && (
            <Button
              onClick={install}
              variant="outline"
              size="sm"
              className="hidden md:flex gap-2 rounded-xl px-3 h-9 bg-primary/10 hover:bg-primary/20 text-primary border-primary/20"
            >
              <Download className="w-4 h-4" />
              <span className="font-medium">앱 설치</span>
            </Button>
          )}

          <div className="hidden md:flex items-center gap-3">
            {isLoading ? (
              <div className="h-8 w-24 animate-pulse bg-muted/50 rounded-xl" />
            ) : user ? (
              <div className="flex items-center gap-3">
                <div className="hidden sm:flex flex-col items-end mr-1">
                  <span className="text-xs font-bold leading-none">{user.name} 님</span>
                  <span className="text-[10px] text-muted-foreground mt-1">로그인 됨</span>
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
                <Button size="sm" className="bg-primary hover:bg-primary-dark text-white text-sm font-medium py-2 px-5 rounded-lg transition-colors shadow-sm">
                  로그인
                </Button>
              </Link>
            )}
          </div>

          <div className="flex md:hidden items-center gap-2">
            <Sheet open={isMenuOpen} onOpenChange={setIsMenuOpen}>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="rounded-xl w-10 h-10 hover:bg-accent/50">
                  <Menu className="w-6 h-6" />
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-[280px] sm:w-[350px] p-0 border-l border-white/5 bg-background/95 backdrop-blur-xl">
                <SheetHeader className="p-6 border-b border-white/5">
                  <SheetTitle className="text-left flex items-center gap-2.5">
                    <Image src="/jbnu-logo.png" alt="로고" width={24} height={24} className="w-6 h-6 object-contain" />
                    <span className="bg-linear-to-r from-[#56296e] to-[#7c4d91] bg-clip-text text-transparent font-bold tracking-tight">수강신청 도우미</span>
                  </SheetTitle>
                </SheetHeader>
                <div className="flex flex-col gap-2 p-4">
                  <div className="mb-4 px-2">
                    {isLoading ? (
                      <div className="h-10 w-full animate-pulse bg-muted/50 rounded-xl" />
                    ) : user ? (
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
                    ) : (
                      <Link href="/login" className="block" onClick={closeMenu}>
                        <Button className="w-full gap-2 rounded-xl h-11 bg-primary shadow-lg shadow-primary/20">
                          로그인하고 시작하기
                        </Button>
                      </Link>
                    )}
                  </div>

                  {isInstallable && (
                    <div className="px-2 mb-2">
                      <Button
                        onClick={() => {
                          install();
                          closeMenu();
                        }}
                        className="w-full gap-2 rounded-xl h-11 bg-primary/10 hover:bg-primary/20 text-primary border border-primary/20"
                      >
                        <Download className="w-4 h-4" />
                        바로가기 설치하기
                      </Button>
                    </div>
                  )}

                  <div className="space-y-1">
                    <p className="px-4 text-[10px] font-bold text-muted-foreground uppercase tracking-widest mb-2">메뉴</p>
                    <NavLinks isMobile isAdmin={user?.role === "ADMIN"} onGuardedAction={handleGuardedAction} onLinkClick={closeMenu} />
                  </div>

                  {user && (
                    <div className="mt-auto pt-6 border-t border-white/5 p-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          logout();
                          closeMenu();
                        }}
                        disabled={isPending}
                        className="w-full gap-3 rounded-xl h-11 justify-start px-4 hover:bg-destructive/5 hover:text-destructive text-muted-foreground transition-colors"
                      >
                        <LogOut className="w-4 h-4" />
                        로그아웃
                      </Button>
                    </div>
                  )}
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </div>
    </header>
  );
}
