"use client";

import { Header } from "@/components/layout/header";
import { SubscriptionList } from "@/components/features/subscription/subscription-list";
import { DashboardTimetable } from "@/components/features/timetable/dashboard-timetable";
import { DashboardWishlist } from "@/components/features/course/dashboard-wishlist";
import { useUser } from "@/hooks/useUser";
import { Bell, LayoutDashboard } from "lucide-react";

export const dynamic = 'force-dynamic';

export default function HomePage() {
  const { data: user } = useUser();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900/50">
      <Header />
      <main className="container max-w-7xl py-10 px-4 md:px-8">
        <div className="flex flex-col gap-8">
          {/* Dashboard Header */}
          <div className="flex flex-col gap-1.5 animate-in fade-in slide-in-from-top-4 duration-700">
             <div className="flex items-center gap-2 mb-1">
                <div className="p-1.5 rounded-lg bg-primary/10 text-primary">
                   <LayoutDashboard className="w-4 h-4" />
                </div>
                <span className="text-xs font-black uppercase tracking-widest text-primary/60">Overview</span>
             </div>
             <h1 className="text-3xl md:text-4xl font-black tracking-tight bg-gradient-to-br from-foreground to-foreground/70 bg-clip-text text-transparent">
               {user?.name ? `${user.name}님의 대시보드` : '대시보드'}
             </h1>
             <p className="text-sm text-muted-foreground font-medium flex items-center gap-2">
                <span>실시간 강의 구독 및 시간표 현황을 한눈에 관리하세요.</span>
             </p>
          </div>

          {/* Main Grid Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
             {/* Left Column: Primary Timetable (70%) */}
             <div className="lg:col-span-8 space-y-8 animate-in fade-in slide-in-from-left-8 duration-1000">
                <DashboardTimetable />
             </div>

             {/* Right Column: Wishlist & Subscriptions (30%) */}
             <div className="lg:col-span-4 space-y-8 animate-in fade-in slide-in-from-right-8 duration-1000 delay-150">
                {/* Wishlist Summary */}
                <section className="bg-white dark:bg-gray-900 rounded-[2rem] p-6 border border-border/40 shadow-sm hover:shadow-md transition-shadow duration-500">
                   <DashboardWishlist />
                </section>

                {/* Subscriptions Summary */}
                <section className="bg-white dark:bg-gray-900 rounded-[2rem] p-6 border border-border/40 shadow-sm hover:shadow-md transition-shadow duration-500">
                   <div className="flex items-center justify-between px-1 mb-6">
                      <h3 className="text-[13px] font-bold text-foreground/80 flex items-center gap-2">
                         <Bell className="w-3.5 h-3.5 text-indigo-500 fill-indigo-500" />
                         알림 수신 중
                      </h3>
                   </div>
                   <div className="max-h-[500px] overflow-y-auto pr-1 -mr-1 custom-scrollbar">
                      <SubscriptionList />
                   </div>
                </section>
             </div>
          </div>
        </div>
      </main>

      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(0, 0, 0, 0.05);
          border-radius: 10px;
        }
        .dark .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.05);
        }
      `}</style>
    </div>
  );
}
