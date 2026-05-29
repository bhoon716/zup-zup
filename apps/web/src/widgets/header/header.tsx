"use client";

import type { MouseEvent } from "react";
import { useUser, useLogout } from "@/features/user/hooks/useUser";
import { Button } from "@/shared/ui/button";
import Link from "next/link";
import Image from "next/image";
import {
  Menu,
  Download,
  LogOut,
} from "lucide-react";
import { usePathname } from "next/navigation";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useState } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/shared/ui/sheet";
import { usePWAInstall } from "@/shared/hooks/usePWAInstall";
import { useHasMounted } from "@/shared/hooks/useHasMounted";
import { DashboardCountdown } from "@/widgets/home/dashboard-countdown";

import { NavLinks } from "./ui/nav-links";
import { HeaderDesktopUser, HeaderMobileUserStatus } from "./ui/user-status";

/**
 * 애플리케이션의 최상단 공통 헤더 컴포넌트입니다.
 * 서비스 로고, 내비게이션 메뉴, PWA 설치 유도 및 사용자 인증 상태(로그인/로그아웃)를 관리합니다.
 */
export function Header() {
  const mobileMenuTriggerId = "header-mobile-menu-trigger";
  const mobileMenuContentId = "header-mobile-menu-content";
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);
  const pathname = usePathname();
  const { install, platform } = usePWAInstall();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const hasMounted = useHasMounted();

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
            <Image src="/zub-zub-logo.png" alt="로고" width={38} height={38} className="w-10 h-10 object-contain group-hover:scale-110 transition-transform" />
            <span className="font-bold text-xl text-primary tracking-tight">줍줍</span>
          </Link>
          <nav className="hidden md:flex items-center gap-1">
            <NavLinks 
              isLoggedIn={hasMounted ? !!user : false} 
              isAdmin={hasMounted ? user?.role === "ADMIN" : false} 
              onGuardedAction={handleGuardedAction} 
            />
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <DashboardCountdown />

          <Button
            onClick={install}
            variant="outline"
            size="sm"
            className="hidden md:flex gap-2 rounded-xl px-3 h-9 bg-primary/10 hover:bg-primary/20 text-primary border-primary/20"
          >
            <Download className="w-4 h-4" />
            <span className="font-medium">{platform === "ios" ? "공유 → 홈 추가" : "웹앱 설치"}</span>
          </Button>

          <div className="hidden md:flex items-center gap-3">
            <HeaderDesktopUser 
              user={hasMounted ? user : undefined} 
              isLoading={hasMounted ? isLoading : true} 
              isPending={isPending} 
              onLogout={() => logout()} 
              onLoginClick={closeMenu}
            />
          </div>

          <div className="flex md:hidden items-center gap-2">
            <Sheet open={isMenuOpen} onOpenChange={setIsMenuOpen}>
              <SheetTrigger asChild>
                <Button id={mobileMenuTriggerId} aria-controls={mobileMenuContentId} variant="ghost" size="icon" className="rounded-xl w-10 h-10 hover:bg-accent/50">
                  <Menu className="w-6 h-6" />
                </Button>
              </SheetTrigger>
              <SheetContent id={mobileMenuContentId} side="right" className="w-[280px] sm:w-[350px] p-0 border-l border-white/5 bg-background/95 backdrop-blur-xl flex flex-col h-full">
                <SheetHeader className="p-6 border-b border-white/5">
                  <SheetTitle className="text-left flex items-center gap-2.5">
                    <Image src="/zub-zub-logo.png" alt="로고" width={32} height={32} className="w-8 h-8 object-contain" />
                    <span className="bg-linear-to-r from-[#56296e] to-[#7c4d91] bg-clip-text text-transparent font-bold tracking-tight">줍줍</span>
                  </SheetTitle>
                </SheetHeader>
                <div className="flex flex-col gap-2 p-4 flex-1 overflow-y-auto min-h-0">
                  <div className="mb-4 px-2">
                    <HeaderMobileUserStatus 
                      user={hasMounted ? user : undefined} 
                      isLoading={hasMounted ? isLoading : true} 
                      onLinkClick={closeMenu} 
                    />
                  </div>

                    <div className="px-2 mb-2">
                      <Button
                        onClick={() => {
                          install();
                          closeMenu();
                        }}
                        className="w-full gap-2 rounded-xl h-11 bg-primary/10 hover:bg-primary/20 text-primary border border-primary/20"
                      >
                        <Download className="w-4 h-4" />
                        {platform === "ios" ? "홈 화면에 추가 안내" : "바로가기 설치하기"}
                      </Button>
                    </div>

                  <div className="space-y-1">
                    <p className="px-4 text-[10px] font-bold text-muted-foreground uppercase tracking-widest mb-2">메뉴</p>
                    <NavLinks 
                      isMobile 
                      isLoggedIn={hasMounted ? !!user : false} 
                      isAdmin={hasMounted ? user?.role === "ADMIN" : false} 
                      onGuardedAction={handleGuardedAction} 
                      onLinkClick={closeMenu} 
                    />
                  </div>

                  {user && (
                    <div className="mt-auto pt-6 border-t border-white/5 p-2 shrink-0">
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
