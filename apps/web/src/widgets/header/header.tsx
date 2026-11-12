"use client";

import type { MouseEvent } from "react";
import { useLogout } from "@/features/user/hooks/useUser";
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
import { cn } from "@/shared/lib/utils";
import { useHasMounted } from "@/shared/hooks/useHasMounted";

import { HeaderInbox } from "./ui/header-inbox";
import { mobileMenuHeadingClass, mobileMenuRowClass } from "./ui/mobile-menu-styles";
import { NavLinks } from "./ui/nav-links";
import { HeaderUserActions, HeaderMobileUserStatus } from "./ui/user-status";

export function Header() {
  const mobileMenuTriggerId = "header-mobile-menu-trigger";
  const mobileMenuContentId = "header-mobile-menu-content";
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isLoading = useAuthStore((state) => state.isLoading);
  const { mutate: logout, isPending } = useLogout();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);
  const pathname = usePathname();
  const { install, platform } = usePWAInstall();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const hasMounted = useHasMounted();
  const shouldShowSkeleton = hasMounted ? isLoading && !user : true;
  const verifiedUser = isAuthenticated ? user : null;
  const showInbox = hasMounted && !!verifiedUser;
  const shouldShowUserSkeleton = hasMounted
    ? (isLoading && !user) || Boolean(user && !isAuthenticated)
    : true;

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
    <header className="sticky top-0 z-50 w-full border-b bg-card/90 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between gap-4 px-4 sm:px-6">
        <div className="flex items-center gap-4">
          <Link href="/" className="flex shrink-0 items-center gap-2 group transition-all active:scale-95">
            <Image src="/zub-zub-logo.png" alt="로고" width={38} height={38} className="w-10 h-10 object-contain group-hover:scale-110 transition-transform" />
            <span className="font-bold text-xl text-primary tracking-tight">줍줍</span>
          </Link>
          <nav aria-label="주요 메뉴" className="hidden items-center xl:flex 2xl:gap-1">
            <NavLinks 
              isLoading={shouldShowSkeleton}
              isLoggedIn={hasMounted ? !!user : false} 
              isAdmin={hasMounted ? verifiedUser?.role === "ADMIN" : false}
              onGuardedAction={handleGuardedAction} 
            />
          </nav>
        </div>

        <div role="group" aria-label="헤더 액션" className="flex shrink-0 items-center gap-2">
          <Button
            onClick={install}
            variant="outline"
            className="hidden rounded-xl border-primary/20 bg-primary/10 text-primary hover:bg-primary/20 hover:text-primary xl:inline-flex"
          >
            <Download aria-hidden="true" />
            {platform === "ios" ? "공유 → 홈 추가" : "웹앱 설치"}
          </Button>

          <div
            className={cn("items-center xl:flex xl:border-l xl:pl-3", showInbox ? "flex" : "hidden")}
            role="group"
            aria-label="사용자 메뉴"
          >
            <HeaderUserActions
              user={hasMounted && (isAuthenticated || !user) ? verifiedUser : undefined}
              isLoading={shouldShowUserSkeleton}
              isPending={isPending} 
              onLogout={() => logout()} 
              onLoginClick={closeMenu}
            >
              {showInbox && <HeaderInbox key={verifiedUser.id} userId={verifiedUser.id} />}
            </HeaderUserActions>
          </div>

          <div className="flex shrink-0 items-center gap-2 xl:hidden" role="group" aria-label="모바일 메뉴">
            <Sheet open={isMenuOpen} onOpenChange={setIsMenuOpen}>
              <SheetTrigger asChild>
                <Button
                  id={mobileMenuTriggerId}
                  aria-controls={mobileMenuContentId}
                  aria-label="메뉴 열기"
                  variant="ghost"
                  size="icon-lg"
                  className="rounded-lg text-muted-foreground hover:bg-primary/5 hover:text-primary"
                >
                  <Menu aria-hidden="true" className="size-5" />
                </Button>
              </SheetTrigger>
              <SheetContent
                id={mobileMenuContentId}
                side="right"
                aria-describedby={undefined}
                className="flex h-dvh w-[min(280px,100%)] flex-col gap-0 bg-background p-0 sm:w-[350px]"
              >
                <SheetHeader className="h-[72px] shrink-0 justify-center border-b border-border px-6">
                  <SheetTitle className="flex items-center gap-2.5 text-left font-bold text-primary">
                    <Image src="/zub-zub-logo.png" alt="" width={32} height={32} className="size-8 object-contain" />
                    줍줍
                  </SheetTitle>
                </SheetHeader>
                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain px-3 pt-2 pb-[max(0.75rem,env(safe-area-inset-bottom))]">
                  <div className="shrink-0 space-y-1 border-b border-border pb-2">
                    <HeaderMobileUserStatus
                      user={hasMounted && (isAuthenticated || !user) ? verifiedUser : undefined}
                      isLoading={shouldShowUserSkeleton}
                      onLinkClick={closeMenu}
                    />
                    <Button
                      variant="ghost"
                      onClick={() => {
                        install();
                        closeMenu();
                      }}
                      className={cn(mobileMenuRowClass, "text-primary")}
                    >
                      <Download aria-hidden="true" />
                      {platform === "ios" ? "홈 화면에 추가 안내" : "바로가기 설치하기"}
                    </Button>
                  </div>

                  <nav aria-label="모바일 메뉴" className="flex shrink-0 flex-col py-2">
                    <h3 className={mobileMenuHeadingClass}>메뉴</h3>
                    <NavLinks
                      isMobile
                      isLoading={shouldShowSkeleton}
                      isLoggedIn={hasMounted ? !!user : false}
                      isAdmin={hasMounted ? verifiedUser?.role === "ADMIN" : false}
                      onGuardedAction={handleGuardedAction}
                      onLinkClick={closeMenu}
                    />
                  </nav>

                  {user && (
                    <div className="mt-auto shrink-0 border-t border-border pt-2">
                      <Button
                        variant="ghost"
                        onClick={() => {
                          logout();
                          closeMenu();
                        }}
                        disabled={isPending}
                        className={cn(mobileMenuRowClass, "text-muted-foreground hover:bg-destructive/5 hover:text-destructive")}
                      >
                        <LogOut aria-hidden="true" />
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
