"use client";

import Link from "next/link";
import { ChevronRight, LayoutDashboard } from "lucide-react";
import { AdminAnnouncementPanel } from "@/features/admin/components/admin-announcement-panel";
import { Button } from "@/shared/ui/button";

export default function AdminAnnouncementsPage() {
  return (
    <div className="bg-[#f3f4f6] text-slate-900">
      <div className="sticky top-16 z-10 border-b border-slate-200 bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-[1920px] items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <span className="font-medium text-slate-800">공지사항 관리</span>
            <ChevronRight className="h-4 w-4 text-slate-300" />
            <span>관리자 전용</span>
          </div>
          <Link href="/admin">
            <Button type="button" variant="outline" className="h-8 gap-1 rounded-lg px-3 text-xs font-medium">
              <LayoutDashboard className="h-3.5 w-3.5" />
              대시보드로
            </Button>
          </Link>
        </div>
      </div>

      <main className="mx-auto max-w-[1920px] px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
        <AdminAnnouncementPanel />
      </main>
    </div>
  );
}
