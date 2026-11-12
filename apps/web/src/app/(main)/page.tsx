import { cookies } from "next/headers";
import Link from "next/link";
import { HomeLanding } from "@/widgets/home/home-landing";
import { HomeSession } from "@/widgets/home/home-session";

const SESSION_COOKIE_NAME = "SESSION";

export default async function HomePage() {
  const hasSessionCookie = (await cookies()).has(SESSION_COOKIE_NAME);

  return (
    <div className="min-h-screen bg-background selection:bg-primary/10 selection:text-primary">
      <HomeSession hasSessionCookie={hasSessionCookie}>
        <HomeLanding />
      </HomeSession>
      <footer className="bg-card border-t py-10">
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
