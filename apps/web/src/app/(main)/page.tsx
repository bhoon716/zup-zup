"use client";

import { useEffect } from "react";
import { Loader2 } from "lucide-react";
import Link from "next/link";
import { Dashboard } from "@/widgets/home/dashboard";
import { HomeLanding } from "@/widgets/home/home-landing";
import { useDashboardSnapshot } from "@/widgets/home/hooks/useDashboard";
import { useAuthStore } from "@/features/auth/store/useAuthStore";

export default function HomePage() {
  const { data: snapshot, isLoading, isError } = useDashboardSnapshot();
  const setUser = useAuthStore((state) => state.setUser);

  useEffect(() => {
    if (snapshot?.user) {
      setUser(snapshot.user);
    }
  }, [setUser, snapshot]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#fafafa] dark:bg-black">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#fafafa] dark:bg-black px-4">
        <div className="rounded-3xl border border-red-100 bg-white p-8 text-center shadow-sm">
          <p className="text-sm font-bold text-slate-900">메인 화면을 불러오지 못했습니다.</p>
          <p className="mt-2 text-xs text-slate-500">잠시 후 다시 시도해 주세요.</p>
          <Link href="/" className="mt-4 inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2 text-sm font-bold text-white">
            다시 시도
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa] dark:bg-black selection:bg-primary/10 selection:text-primary">
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-primary/5 blur-[120px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] bg-indigo-500/5 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: "2s" }} />
      </div>

      {snapshot?.user ? <Dashboard snapshot={snapshot} /> : <HomeLanding />}

      <footer className="bg-white border-t border-gray-100 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="text-center md:text-left">
              <h5 className="text-lg font-bold text-primary mb-1">Zup-zup</h5>
              <p className="text-xs text-muted-foreground">전북대학교 수강신청 빈자리 알림 서비스 &apos;줍줍&apos; (비영리)</p>
            </div>
            <div className="flex gap-6 text-xs font-bold text-muted-foreground">
              <Link href="/terms" className="hover:text-primary transition-colors">이용약관</Link>
              <Link href="/privacy" className="hover:text-primary transition-colors">개인정보 처리방침</Link>
              <Link href="/feedback" className="hover:text-primary transition-colors">문의 및 건의</Link>
            </div>
            <p className="text-[11px] text-muted-foreground/60 text-center md:text-right">
              © 2026 줍줍. All rights reserved.
            </p>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-50 text-center">
            <p className="text-[11px] text-muted-foreground/50 leading-relaxed max-w-2xl mx-auto">
              본 서비스는 전북대학교 학생들을 위한 공익적 목적으로 운영되는 비영리 서비스입니다. 제공되는 모든 정보의 출처는 전북대학교 오아시스이며, 수강신청 결과에 대한 최종 책임은 사용자 본인에게 있습니다.
            </p>
          </div>
        </div>
      </footer>

      <style>{`
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
