"use client";

import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { Button } from "@/shared/ui/button";

export default function PrivacyPage() {
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
            <h1 className="text-3xl font-black tracking-tight">개인정보 처리방침</h1>
            <p className="text-muted-foreground font-medium text-sm">
              시행일: 2026년 7월 13일
            </p>
          </header>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">1. 개인정보 수집 및 이용 목적</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              &apos;JBNU Helper&apos;는 사용자의 수강신청 빈자리 알림 서비스를 제공하기 위해 최소한의 개인정보를 수집합니다. 수집된 정보는 다음 목적 이외의 용도로는 사용되지 않습니다.
            </p>
            <ul className="list-disc list-inside text-sm text-muted-foreground pl-2 space-y-1">
              <li>사용자 식별 및 서비스 이용 의사 확인</li>
              <li>강의 여석 발생 시 실시간 알림(이메일, 웹푸시, 디스코드) 발송</li>
              <li>서비스 이용 기록 관리 및 부정 이용 방지</li>
            </ul>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">2. 수집하는 개인정보 항목</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              서비스 이용 과정에서 다음과 같은 정보들이 수집될 수 있습니다.
            </p>
            <ul className="list-disc list-inside text-sm text-muted-foreground pl-2 space-y-1">
              <li>필수 항목: 이메일(Google OAuth), 이름</li>
              <li>선택 항목: 디스코드 ID (알림 연동 시)</li>
              <li>자동 수집 항목: 서비스 이용 기록, 접속 로그, 쿠키, 기기 정보(푸시 알림용)</li>
            </ul>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">3. 개인정보의 보유 및 이용 기간</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              회원 탈퇴 시 이름, 로그인·알림 이메일, 디스코드 식별자, 푸시 기기 정보와 인증 정보는 즉시 삭제하거나 익명화합니다. 서비스 안전, 통계 및 감사에 필요한 비식별 이용 이력은 보존할 수 있으며, 건의사항 본문과 첨부파일은 일반 사용자에게 보이지 않도록 논리 삭제 처리합니다. 보존 기간과 최종 파기 기준은 관계 법령 및 개인정보 검토 후 정합니다.
            </p>
          </section>

          <section className="space-y-4">
            <h2 className="text-xl font-bold">4. 개인정보의 파기 절차 및 방법</h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              탈퇴 요청이 완료되면 즉시 로그아웃되고, 기존 인증 정보로 서비스에 다시 접근할 수 없습니다. 같은 Google 계정으로 다시 로그인하는 경우에는 기존 탈퇴 계정을 복구하지 않고 새 계정으로 시작합니다. 보존 대상은 일반 사용자 경로에서 차단하며, 최종 파기가 확정되면 복구할 수 없는 기술적 방법으로 삭제합니다.
            </p>
          </section>

          <section className="space-y-4 border-t border-border/40 pt-6">
            <p className="text-xs text-muted-foreground">
              본 방침에 대한 문의사항은 제작자(qkrqudgns@jbnu.ac.kr)에게 연락 주시기 바랍니다.
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
