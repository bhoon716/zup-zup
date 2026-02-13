"use client";

import Link from "next/link";
import { ChevronLeft, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function TermsPage() {
  return (
    <div className="min-h-screen bg-[#fafafa] dark:bg-black py-12 px-6">
      <div className="max-w-3xl mx-auto space-y-8">
        <Link href="/">
          <Button variant="ghost" size="sm" className="gap-2 mb-4">
            <ChevronLeft className="w-4 h-4" /> 홈으로 돌아가기
          </Button>
        </Link>
        
        <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] border border-border/40 p-8 md:p-12 shadow-sm space-y-8">
          <header className="space-y-2 border-b border-border/40 pb-6">
            <div className="flex items-center gap-2 text-primary font-black text-xs uppercase tracking-widest mb-2">
               <ShieldAlert className="w-4 h-4" /> Legal
            </div>
            <h1 className="text-3xl font-black tracking-tight">서비스 이용약관 및 면책 조항</h1>
          </header>

          <section className="space-y-4">
            <h2 className="text-xl font-bold text-primary">1. 목적 및 비영리 정책</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              &apos;JBNU Helper&apos;는 전북대학교 학생들을 위한 공익적 목적으로 개발된 <strong>비영리 서비스</strong>입니다. 본 서비스는 어떠한 영리 활동도 수행하지 않으며, 학생들의 수강신청 편의를 돕는 것을 유일한 목적으로 합니다.
            </p>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">2. 법적 면책 조항 (중요)</h2>
            <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/40 rounded-2xl p-6 space-y-3">
              <p className="text-sm text-amber-900 dark:text-amber-200 leading-relaxed font-medium">
                본 서비스는 정보 제공을 보조하는 도구일 뿐이며, 아래 사항에 대해 법적 책임을 지지 않습니다.
              </p>
              <ul className="list-disc list-inside text-sm text-amber-800/80 dark:text-amber-300/80 pl-2 space-y-2">
                <li>학교 공식 서버의 지연, 데이터 오류로 인한 정보 불일치</li>
                <li>알림 지연 혹은 미발송으로 인한 수강신청 실패 결과</li>
                <li>본 서비스 이용 중 발생하는 사용자의 수강신청 결과에 대한 모든 책임</li>
                <li>학교 측의 정책 변경으로 인한 서비스 중단 및 이용 제한</li>
              </ul>
            </div>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">3. 사용자의 의무</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              사용자는 본 서비스를 이용함에 있어 학교의 공식 학칙 및 수강신청 관련 규정을 준수해야 합니다. 서비스에 과도한 부하를 주거나 비정상적인 방법으로 이용하는 행위는 금지됩니다.
            </p>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">4. 데이터 출처</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              본 서비스에서 제공하는 모든 강의 정보 및 여석 데이터의 원 출처는 <strong>전북대학교 오아시스(Oasis)</strong>이며, 모든 저작권 및 권한은 학교 본부에 있습니다.
            </p>
          </section>

          <section className="space-y-4 border-t border-border/40 pt-6">
            <p className="text-xs text-muted-foreground">
              본 서비스를 이용하는 것은 위 조항에 모두 동의하는 것으로 간주됩니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
