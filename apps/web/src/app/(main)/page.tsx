import { Suspense } from "react";
import { cookies } from "next/headers";
import Link from "next/link";
import { HomeLanding } from "@/widgets/home/home-landing";
import { HomeSession } from "@/widgets/home/home-session";
import { DashboardCountdown } from "@/widgets/home/dashboard-countdown";
import { HomeAnnouncements } from "@/widgets/home/home-announcements";
import { HomeSchedules } from "@/widgets/home/home-schedules";
import type { AnnouncementListItemResponse, PageResponse, ScheduleResponse } from "@/shared/types/api";
import { fetchPublicData } from "@/shared/lib/public-metadata";

const SESSION_COOKIE_NAME = "SESSION";

async function PublicCountdown() {
  const data = await fetchPublicData<ScheduleResponse[]>("/api/v1/schedules/ddays", { cache: "no-store" });
  return <DashboardCountdown initialData={data} />;
}

async function PublicSchedules() {
  const data = await fetchPublicData<ScheduleResponse[]>("/api/v1/schedules", { cache: "no-store" });
  return <HomeSchedules initialData={data} />;
}

async function PublicAnnouncements() {
  const result = await fetchPublicData<PageResponse<AnnouncementListItemResponse>>(
    "/api/v1/announcements?searchType=TITLE_CONTENT&page=0",
    { cache: "no-store" },
  );
  const data = Array.isArray(result?.data.content) ? { data: result.data.content, updatedAt: result.updatedAt } : null;
  return <HomeAnnouncements initialData={data} />;
}

export default async function HomePage() {
  const hasSessionCookie = (await cookies()).has(SESSION_COOKIE_NAME);

  return (
    <div className="min-h-screen bg-[#fafafa] dark:bg-black selection:bg-primary/10 selection:text-primary">
      <div className="fixed inset-0 pointer-events-none overflow-hidden -z-10">
        <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-primary/5 blur-[120px] rounded-full motion-safe:animate-pulse" />
        <div className="absolute bottom-[-10%] right-[-5%] w-[40%] h-[40%] bg-indigo-500/5 blur-[120px] rounded-full motion-safe:animate-pulse [animation-delay:2s]" />
      </div>

      <HomeSession hasSessionCookie={hasSessionCookie}>
        <HomeLanding
          countdown={hasSessionCookie ? <DashboardCountdown /> : <Suspense><PublicCountdown /></Suspense>}
          schedules={hasSessionCookie ? <HomeSchedules /> : <Suspense fallback={<div className="h-56" />}><PublicSchedules /></Suspense>}
          announcements={hasSessionCookie ? <HomeAnnouncements /> : <Suspense fallback={<div className="h-56" />}><PublicAnnouncements /></Suspense>}
        />
      </HomeSession>
      <footer className="bg-white border-t border-gray-100 py-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <div className="text-center md:text-left">
              <p className="text-lg font-bold text-primary mb-1">Zup-zup</p>
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
    </div>
  );
}
