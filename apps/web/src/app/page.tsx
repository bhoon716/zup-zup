"use client";

// import { Header } from "@/components/layout/header"; // Removed header import
import { DashboardTimetable } from "@/components/features/timetable/dashboard-timetable";
import { DashboardStats } from "@/components/features/dashboard/dashboard-stats";
import { HomeLanding } from "@/components/features/dashboard/home-landing";
import { useUser } from "@/hooks/useUser";
import { Sparkles, ArrowRight, Calendar, Loader2 } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export const dynamic = 'force-dynamic';

export default function HomePage() {
  const { data: user, isLoading } = useUser();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#fafafa] dark:bg-black">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] dark:bg-black selection:bg-primary/10 selection:text-primary">
      {/* Header removed from here and moved to layout.tsx */}
      
      {/* Background Decoration */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-primary/5 blur-[120px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] bg-indigo-500/5 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      <main className="container max-w-7xl py-12 px-6 md:px-10 space-y-12">
        {!user ? (
          <HomeLanding />
        ) : (
          <>
            {/* Hero Section */}
            <section className="relative flex flex-col md:flex-row md:items-end justify-between gap-6 pb-2 border-b border-border/40 animate-in fade-in slide-in-from-bottom-4 duration-700">
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-primary uppercase tracking-[0.2em] text-[10px] font-black">
                  <div className="w-8 h-[1px] bg-primary/30" />
                  <Sparkles className="w-3 h-3" />
                  Welcome to JBNU Helper
                </div>
                <div className="space-y-1">
                  <h1 className="text-4xl md:text-5xl font-black tracking-tight text-foreground">
                    안녕하세요, <span className="bg-gradient-to-r from-primary to-[#7c4d91] bg-clip-text text-transparent">{user.name}</span>님!
                  </h1>
                  <p className="text-muted-foreground font-medium md:text-lg">
                    스마트한 수강신청의 시작, 오늘 진행할 작업을 확인하세요.
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap gap-3">
                 <Link href="/timetable">
                   <Button className="rounded-2xl px-6 h-12 font-bold shadow-lg shadow-primary/20 hover:shadow-primary/30 transition-all hover:-translate-y-0.5 active:translate-y-0 active:scale-95 bg-primary text-white">
                     시간표 관리하기
                   </Button>
                 </Link>
                 <Link href="/search">
                   <Button variant="outline" className="rounded-2xl px-6 h-12 font-bold bg-white/50 backdrop-blur-sm border-border/60 hover:bg-white transition-all hover:-translate-y-0.5 active:translate-y-0 active:scale-95">
                     강의 검색
                   </Button>
                 </Link>
              </div>
            </section>

            {/* Stats Section */}
            <section className="animate-in fade-in slide-in-from-bottom-8 duration-700 delay-100">
               <DashboardStats />
            </section>

            {/* Main Timetable Section - Full Width Expansion */}
            <div className="animate-in fade-in slide-in-from-bottom-12 duration-700 delay-200">
              <section className="group relative rounded-[2.5rem] bg-white dark:bg-gray-900 border border-border/40 shadow-sm hover:shadow-xl hover:border-primary/20 transition-all duration-700 overflow-hidden min-h-[700px]">
                <div className="absolute top-0 right-0 w-96 h-96 bg-primary/5 blur-3xl -mr-48 -mt-48 group-hover:bg-primary/10 transition-colors duration-700" />
                <div className="relative p-8 md:p-10 h-full flex flex-col">
                  <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 rounded-2xl bg-primary/10 text-primary flex items-center justify-center">
                        <Calendar className="w-6 h-6" />
                      </div>
                      <div>
                        <h2 className="text-xl font-black text-foreground">내 대표 시간표</h2>
                        <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider mt-1 opacity-60">Representative Weekly Schedule</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <Link href="/search">
                        <Button variant="outline" size="sm" className="rounded-full px-4 h-9 font-bold text-xs border-border/60 hover:bg-primary/5 hover:text-primary transition-all">
                          강의 추가
                        </Button>
                      </Link>
                      <Link href="/timetable">
                        <Button variant="ghost" size="sm" className="rounded-full px-4 h-9 font-bold text-xs hover:bg-primary/5 hover:text-primary gap-1 transition-all">
                          상세보기 <ArrowRight className="w-3 h-3" />
                        </Button>
                      </Link>
                    </div>
                  </div>
                  
                  <div className="flex-1 bg-[#fcfcfc] dark:bg-black/20 rounded-3xl border border-border/20 p-2 md:p-4 overflow-hidden shadow-inner">
                    <DashboardTimetable />
                  </div>
                </div>
              </section>
            </div>
          </>
        )}
      </main>

      <style jsx global>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 4px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(86, 41, 110, 0.1);
          border-radius: 10px;
        }
        .dark .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(255, 255, 255, 0.05);
        }
      `}</style>
    </div>
  );
}
