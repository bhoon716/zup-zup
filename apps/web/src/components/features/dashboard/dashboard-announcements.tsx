"use client";

import { Megaphone, ExternalLink, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import Link from "next/link";

const announcements = [
  {
    id: 1,
    tag: "학사",
    title: "2026학년도 1학기 수강신청 안내 (필독)",
    date: "2026-02-15",
    isNew: true
  },
  {
    id: 2,
    tag: "서비스",
    title: "서버 점검 안내 (02. 20 02:00 ~ 04:00)",
    date: "2026-02-14",
    isNew: false
  },
  {
    id: 3,
    tag: "학사",
    title: "강의계획서 조회 및 장바구니 이용 안내",
    date: "2026-02-12",
    isNew: false
  }
];

/**
 * 서비스 및 학사의 주요 공지사항을 보여주는 컴포넌트입니다.
 */
export function DashboardAnnouncements() {
  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full">
      <div className="flex justify-between items-center mb-5">
        <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-indigo-50 dark:bg-indigo-950/30 flex items-center justify-center">
            <Megaphone className="w-5 h-5 text-indigo-600" />
          </div>
          중요 공지사항
        </h3>
        <Button variant="ghost" size="sm" className="text-xs font-bold text-gray-400 hover:text-primary transition-colors">
          전체보기 <ChevronRight className="w-4 h-4" />
        </Button>
      </div>
 
      <div className="space-y-3">
        {announcements.map((item) => (
          <div 
            key={item.id} 
            className="group flex items-center justify-between p-3.5 rounded-2xl border border-gray-50 dark:border-gray-800/50 hover:border-indigo-100 dark:hover:border-indigo-900/40 hover:bg-indigo-50/30 dark:hover:bg-indigo-900/10 transition-all cursor-pointer"
          >
            <div className="flex items-center gap-4 overflow-hidden">
              <span className="text-[10px] font-black text-indigo-600 bg-indigo-50 dark:bg-indigo-950/50 px-2 py-0.5 rounded-md border border-indigo-100 dark:border-indigo-900/50 shrink-0">
                {item.tag}
              </span>
              <h4 className="text-sm font-bold text-gray-800 dark:text-gray-200 truncate group-hover:text-indigo-600 transition-colors">
                {item.title}
              </h4>
              {item.isNew && (
                <div className="w-1.5 h-1.5 rounded-full bg-red-500 shrink-0" />
              )}
            </div>
            <div className="flex items-center gap-3 shrink-0 ml-4">
              <span className="text-[10px] text-gray-400 font-medium">{item.date}</span>
              <ExternalLink className="w-3.5 h-3.5 text-gray-300 group-hover:text-indigo-400 transition-colors" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
