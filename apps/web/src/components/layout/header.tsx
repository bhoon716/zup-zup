"use client";

import { useUser, useLogout } from "@/hooks/useUser";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { LogOut, Search, Home, Bell, Settings, ShieldCheck, Calendar, Menu, Download } from "lucide-react";
import { usePathname } from "next/navigation";

import { useAuthStore } from "@/store/useAuthStore";
import { 
  Sheet, 
  SheetContent, 
  SheetHeader, 
  SheetTitle, 
  SheetTrigger, 
  SheetClose 
} from "@/components/ui/sheet";
import { usePWAInstall } from "@/hooks/usePWAInstall";

export function Header() {
  const { data: user, isLoading } = useUser();
  const { mutate: logout, isPending } = useLogout();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);
  const pathname = usePathname();
  const { isInstallable, install } = usePWAInstall(); // Added hook

  // Hide header on specific routes
  if (pathname === "/onboarding") {
    return null;
  }

  const handleGuardedAction = (e: React.MouseEvent, href: string) => {
    if (!user) {
      e.preventDefault();
      setLoginModalOpen(true);
    }
  };

  const NavLinks = ({ isMobile = false }: { isMobile?: boolean }) => (
    <>
      <Link href="/timetable" onClick={(e) => handleGuardedAction(e, "/timetable")}>
        <Button variant="ghost" size="sm" className={cn("gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Calendar className="w-4 h-4 text-muted-foreground" />
          <span className="font-medium">내 시간표</span>
        </Button>
      </Link>
      <Link href="/search">
        <Button variant="ghost" size="sm" className={cn("gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Search className="w-4 h-4 text-muted-foreground" />
          <span className="font-medium">강의 검색</span>
        </Button>
      </Link>
      <Link href="/notifications" onClick={(e) => handleGuardedAction(e, "/notifications")}>
        <Button variant="ghost" size="sm" className={cn("gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Bell className="w-4 h-4 text-muted-foreground" />
          <span className="font-medium">알림 내역</span>
        </Button>
      </Link>
      <Link href="/settings" onClick={(e) => handleGuardedAction(e, "/settings")}>
        <Button variant="ghost" size="sm" className={cn("gap-2 rounded-xl px-3 h-9 hover:bg-accent/50 text-foreground", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Settings className="w-4 h-4 text-muted-foreground" />
          <span className="font-medium">설정</span>
        </Button>
      </Link>
      {user?.role === 'ADMIN' && (
        <Link href="/admin">
          <Button variant="ghost" size="sm" className={cn("gap-2 rounded-xl px-3 h-9 text-[#56296e]/70 hover:text-[#56296e] hover:bg-[#56296e]/5", isMobile && "w-full justify-start h-11 px-4 text-base")}>
            <ShieldCheck className="w-4 h-4" />
            <span className="font-medium">관리자</span>
          </Button>
        </Link>
      )}
    </>
  );

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
            <NavLinks />
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

          {/* Mobile Menu */}
          <div className="flex md:hidden items-center gap-2">
            <Sheet>
              <SheetTrigger asChild>
                <Button variant="ghost" size="icon" className="rounded-xl w-10 h-10 hover:bg-accent/50">
                  <Menu className="w-6 h-6" />
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-[280px] sm:w-[350px] p-0 border-l border-white/5 bg-background/95 backdrop-blur-xl">
                <SheetHeader className="p-6 border-b border-white/5">
                  <SheetTitle className="text-left flex items-center gap-2.5">
                    <img src="/jbnu-logo.png" alt="로고" className="w-6 h-6 object-contain" />
                    <span className="bg-gradient-to-r from-[#56296e] to-[#7c4d91] bg-clip-text text-transparent font-bold tracking-tight">수강신청 도우미</span>
                  </SheetTitle>
                </SheetHeader>
                <div className="flex flex-col gap-2 p-4">
                  <div className="mb-4 px-2">
                    {/* ... user logic ... */}
                    {isLoading ? (
                       <div className="h-10 w-full animate-pulse bg-muted/50 rounded-xl" />
                    ) : user ? (
                      <div className="bg-accent/30 rounded-2xl p-4 border border-white/5">
                         {/* ... user specific ... */}
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
                       <Link href="/login" className="block">
                        <Button className="w-full gap-2 rounded-xl h-11 bg-primary shadow-lg shadow-primary/20">
                          로그인하고 시작하기
                        </Button>
                      </Link>
                    )}
                  </div>
                  
                  {isInstallable && (
                    <div className="px-2 mb-2">
                         <Button 
                            onClick={install}
                            className="w-full gap-2 rounded-xl h-11 bg-primary/10 hover:bg-primary/20 text-primary border border-primary/20"
                        >
                          <Download className="w-4 h-4" />
                          앱 설치하기
                        </Button>
                    </div>
                  )}

                  <div className="space-y-1">
                    <p className="px-4 text-[10px] font-bold text-muted-foreground uppercase tracking-widest mb-2">Menu</p>
                    <NavLinks isMobile />
                  </div>

                  {user && (
                    <div className="mt-auto pt-6 border-t border-white/5 p-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => logout()}
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
