"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { Button } from "@/shared/ui/button";
import { cn } from "@/shared/lib/utils";
import { 
  ShieldCheck, Calendar, Search, Megaphone, LayoutDashboard, ChevronDown, Settings, MessageCircle, RotateCcw
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { JBNUSiteLinks } from "./jbnu-site-links";
import { mobileMenuHeadingClass, mobileMenuRowClass, mobileMenuSectionClass } from "./mobile-menu-styles";

interface NavLinksProps {
  isMobile?: boolean;
  isAdmin: boolean;
  isLoggedIn: boolean;
  onLinkClick?: () => void;
  mobileShortcutAction?: ReactNode;
}

export function NavLinks({ isMobile = false, isAdmin, isLoggedIn, onLinkClick, mobileShortcutAction }: NavLinksProps) {
  const adminMenuTriggerId = "header-admin-menu-trigger";
  const adminMenuContentId = "header-admin-menu-content";
  const navLinkClass = isMobile
    ? mobileMenuRowClass
    : "gap-1.5 rounded-xl px-3 h-9 leading-5 hover:bg-primary/5 text-gray-600 hover:text-primary transition-colors xl:has-[>svg]:px-2 2xl:has-[>svg]:px-3";
  const adminOnlyClass = cn(isMobile ? mobileMenuSectionClass : "flex items-center", !isAdmin && "hidden");

  return (
    <>
      {isLoggedIn && (
        <Button asChild variant="ghost" size="sm" className={navLinkClass}>
          <Link href="/timetable" onClick={onLinkClick}>
            <Calendar aria-hidden="true" />
            <span className="text-sm font-medium">내 시간표</span>
          </Link>
        </Button>
      )}
      <Button asChild variant="ghost" size="sm" className={navLinkClass}>
        <Link href="/search" onClick={onLinkClick}>
          <Search aria-hidden="true" />
          <span className="text-sm font-medium">강의 검색</span>
        </Link>
      </Button>
      <Button asChild variant="ghost" size="sm" className={navLinkClass}>
        <Link href="/announcements" onClick={onLinkClick}>
          <Megaphone aria-hidden="true" />
          <span className="text-sm font-medium">공지사항</span>
        </Link>
      </Button>
      {isLoggedIn && (
        <Button asChild variant="ghost" size="sm" className={navLinkClass}>
          <Link href="/feedback" onClick={onLinkClick}>
            <MessageCircle aria-hidden="true" />
            <span className="text-sm font-medium">건의 / 버그</span>
          </Link>
        </Button>
      )}

      <JBNUSiteLinks isMobile={isMobile} onLinkClick={onLinkClick} mobileShortcutAction={mobileShortcutAction} />
      <div className={adminOnlyClass} aria-hidden={!isAdmin}>
        {isMobile ? (
          <>
            <h3 className={mobileMenuHeadingClass}>관리 메뉴</h3>
            <div className="flex flex-col gap-0.5">
              <Button asChild variant="ghost" size="sm" className={mobileMenuRowClass}>
                <Link href="/admin" onClick={onLinkClick}>
                  <LayoutDashboard className="w-4 h-4" />
                  대시보드
                </Link>
              </Button>
              <Button asChild variant="ghost" size="sm" className={mobileMenuRowClass}>
                <Link href="/admin/settings" onClick={onLinkClick}>
                  <Settings className="w-4 h-4" />
                  설정 및 제어
                </Link>
              </Button>
              <Button asChild variant="ghost" size="sm" className={mobileMenuRowClass}>
                <Link href="/admin/announcements" onClick={onLinkClick}>
                  <Megaphone className="w-4 h-4" />
                  공지사항 관리
                </Link>
              </Button>
              <Button asChild variant="ghost" size="sm" className={mobileMenuRowClass}>
                <Link href="/admin/feedbacks" onClick={onLinkClick}>
                  <MessageCircle className="w-4 h-4" />
                  건의/버그 관리
                </Link>
              </Button>
              <Button asChild variant="ghost" size="sm" className={mobileMenuRowClass}>
                <Link href="/admin/notification-deliveries" onClick={onLinkClick}>
                  <RotateCcw className="w-4 h-4" />
                  알림 DLQ 관리
                </Link>
              </Button>
            </div>
          </>
        ) : (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button id={adminMenuTriggerId} aria-controls={adminMenuContentId} variant="ghost" size="sm" className="gap-1.5 rounded-xl px-3 h-9 leading-5 text-primary font-bold hover:bg-primary/5 xl:has-[>svg]:px-2 2xl:has-[>svg]:px-3">
                <ShieldCheck aria-hidden="true" />
                <span className="text-sm">관리자</span>
                <ChevronDown className="w-3.5 h-3.5 opacity-50" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent id={adminMenuContentId} aria-labelledby={adminMenuTriggerId} align="end" className="w-56 rounded-2xl border-gray-100 bg-white/95 backdrop-blur-xl p-2 shadow-2xl">
              <DropdownMenuItem asChild className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                <Link href="/admin">
                  <LayoutDashboard className="w-4 h-4" />
                  대시보드
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                <Link href="/admin/settings">
                  <Settings className="w-4 h-4" />
                  설정 및 제어
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                <Link href="/admin/announcements">
                  <Megaphone className="w-4 h-4" />
                  공지사항 관리
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                <Link href="/admin/feedbacks">
                  <MessageCircle className="w-4 h-4" />
                  건의/버그 관리
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-bold text-primary hover:bg-primary/5 cursor-pointer">
                <Link href="/admin/notification-deliveries">
                  <RotateCcw className="w-4 h-4" />
                  알림 DLQ 관리
                </Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        )}
      </div>
    </>
  );
}
