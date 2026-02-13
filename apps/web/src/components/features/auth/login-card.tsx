"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Chromium } from "lucide-react";

export function LoginCard() {
  const handleGoogleLogin = () => {
    // Redirect to Backend OAuth2 Endpoint (via Vercel Rewrites)
    // /api Prefix를 붙여야 next.config.ts의 rewrites 설정에 따라 백엔드로 프록시됨
    window.location.href = `${window.location.origin}/api/oauth2/authorization/google`;
  };

  return (
    <Card className="w-full max-w-[320px] sm:max-w-md mx-auto shadow-2xl border-none bg-white/70 dark:bg-gray-900/70 backdrop-blur-xl rounded-[2rem] overflow-hidden">
      <CardHeader className="text-center space-y-3 p-6 md:p-8">
        <div className="w-16 h-16 bg-primary/10 rounded-2xl flex items-center justify-center mx-auto mb-2">
           <Chromium className="w-8 h-8 text-primary" />
        </div>
        <CardTitle className="text-2xl md:text-3xl font-black tracking-tight text-foreground">
          반갑습니다!
        </CardTitle>
        <CardDescription className="text-xs md:text-sm font-medium leading-relaxed">
          전북대학교 수강신청 빈자리 알림 서비스 <br className="hidden md:block" /> 
          <span className="text-primary font-bold">JBNU Helper</span>입니다.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6 p-6 md:p-8 pt-0">
        <Button
          variant="outline"
          className="w-full flex items-center justify-center gap-3 h-14 rounded-2xl text-base font-bold transition-all hover:bg-white border-border/60 hover:shadow-lg active:scale-95"
          onClick={handleGoogleLogin}
        >
          <img src="https://www.google.com/favicon.ico" className="w-5 h-5 flex-shrink-0" alt="Google" />
          Google로 시작하기
        </Button>
        <p className="text-[10px] text-center text-muted-foreground leading-relaxed px-4">
          로그인 시 서비스 이용약관 및 <br className="md:hidden" /> 개인정보 처리방침에 동의하게 됩니다.
        </p>
      </CardContent>
    </Card>
  );
}
