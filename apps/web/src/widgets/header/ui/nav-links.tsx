"use client";

import type { MouseEvent } from "react";
import Link from "next/link";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";
import { 
  ShieldCheck, Calendar, Search, Bell, Megaphone, LayoutDashboard, ChevronDown, Settings, MessageCircle
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { JBNUSiteLinks } from "./jbnu-site-links";

interface NavLinksProps {
  isMobile?: boolean;
  isAdmin: boolean;
  isLoggedIn: boolean;
  onGuardedAction: (e: MouseEvent) => void;
  onLinkClick?: () => void;
}

/**
 * 메인 내비게이션 링크들을 렌더링하는 컴포넌트입니다.
 */
export function NavLinks({ isMobile = false, isAdmin, isLoggedIn, onGuardedAction, onLinkClick }: NavLinksProps) {
  const handleClick = (e: MouseEvent) => {
    onLinkClick?.();
    onGuardedAction(e);
  };

  return (
    <>
      {isLoggedIn && (
        <Link href="/timetable" onClick={handleClick}>
          <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
            <Calendar className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm font-medium">내 시간표</span>
          </Button>
        </Link>
      )}
      <Link href="/search" onClick={onLinkClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Search className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">강의 검색</span>
        </Button>
      </Link>
      {isLoggedIn && (
        <Link href="/notifications" onClick={handleClick}>
          <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
            <Bell className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm font-medium">알림 / 구독</span>
          </Button>
        </Link>
      )}
      <Link href="/announcements" onClick={onLinkClick}>
        <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
          <Megaphone className="w-[1.1rem] h-[1.1rem]" />
          <span className="text-sm font-medium">공지사항</span>
        </Button>
      </Link>
      {isLoggedIn && (
        <Link href="/feedback" onClick={handleClick}>
          <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
            <MessageCircle className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm font-medium">건의 / 버그</span>
          </Button>
        </Link>
      )}
      {isLoggedIn && (
        <Link href="/settings" onClick={handleClick}>
          <Button variant="ghost" size="sm" className={cn("gap-1.5 rounded-xl px-3 h-9 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors", isMobile && "w-full justify-start h-11 px-4 text-base")}>
            <Settings className="w-[1.1rem] h-[1.1rem]" />
            <span className="text-sm font-medium">설정</span>
          </Button>
        </Link>
      )}

      <JBNUSiteLinks isMobile={isMobile} onLinkClick={onLinkClick} />
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
                <Link href="/admin/announcements" onClick={onLinkClick}>
                  <Button variant="ghost" size="sm" className="w-full justify-start gap-3 h-11 px-4 text-sm font-semibold hover:bg-primary/5 text-primary">
                    <Megaphone className="w-4 h-4" />
                    공지사항 관리
                  </Button>
                </Link>
                <Link href="/admin/feedbacks" onClick={onLinkClick}>
                  <Button variant="ghost" size="sm" className="w-full justify-start gap-3 h-11 px-4 text-sm font-semibold hover:bg-primary/5 text-primary">
                    <MessageCircle className="w-4 h-4" />
                    건의/버그 관리
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
                <Link href="/admin/announcements">
                  <DropdownMenuItem className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                    <Megaphone className="w-4 h-4" />
                    공지사항 관리
                  </DropdownMenuItem>
                </Link>
                <Link href="/admin/feedbacks">
                  <DropdownMenuItem className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                    <MessageCircle className="w-4 h-4" />
                    건의/버그 관리
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

