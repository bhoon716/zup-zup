"use client";

import { Dashboard } from "@/widgets/dashboard/dashboard";
import { HomeLanding } from "@/widgets/dashboard/home-landing";
import { useUser } from "@/features/user/hooks/useUser";
import { Loader2 } from "lucide-react";
import Link from "next/link";

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
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-primary/5 blur-[120px] rounded-full animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] bg-indigo-500/5 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '2s' }} />
      </div>

      {!user ? (
        <HomeLanding />
      ) : (
        <Dashboard />
      )}

      {/* 푸터 섹션 */}
      <footer className="bg-white border-t border-gray-100 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="text-center md:text-left">
              <h5 className="text-lg font-bold text-primary mb-1">JBNU Helper</h5>
              <p className="text-xs text-muted-foreground">전북대학교 수강신청 빈자리 알림 서비스 (비영리)</p>
            </div>
            <div className="flex gap-6 text-xs font-bold text-muted-foreground">
              <Link href="/terms" className="hover:text-primary transition-colors">이용약관</Link>
              <Link href="/privacy" className="hover:text-primary transition-colors">개인정보 처리방침</Link>
              <Link href="#" className="hover:text-primary transition-colors">문의하기</Link>
            </div>
            <p className="text-[11px] text-muted-foreground/60 text-center md:text-right">
              © 2026 JBNU Helper. All rights reserved.
            </p>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-50 text-center">
            <p className="text-[11px] text-muted-foreground/50 leading-relaxed max-w-2xl mx-auto">
              본 서비스는 전북대학교 학생들을 위한 공익적 목적으로 운영되는 비영리 서비스입니다. 제공되는 모든 정보의 출처는 전북대학교 오아시스이며, 수강신청 결과에 대한 최종 책임은 사용자 본인에게 있습니다.
            </p>
          </div>
        </div>
      </footer>

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
