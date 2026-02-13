"use client";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Chromium, AlertCircle, ExternalLink } from "lucide-react";
import { isInAppBrowser } from "@/lib/utils";
import { useEffect, useState } from "react";

export function LoginCard() {
  const [isInApp, setIsInApp] = useState(false);

  useEffect(() => {
    setIsInApp(isInAppBrowser());
  }, []);

  const handleGoogleLogin = () => {
    if (isInApp) return;
    // Redirect to Backend OAuth2 Endpoint (via Vercel Rewrites)
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
        {isInApp && (
          <div className="bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800/50 rounded-2xl p-4 space-y-2">
            <div className="flex items-center gap-2 text-amber-700 dark:text-amber-400 font-bold text-sm">
              <AlertCircle className="w-4 h-4" />
              <span>로그인 제한 안내</span>
            </div>
            <p className="text-xs text-amber-600/90 dark:text-amber-500/90 leading-relaxed font-medium">
              현재 인앱 브라우저(카카오톡, 인스타그램 등)에서는 Google 로그인이 차단됩니다. 
              상단 또는 하단의 <span className="font-bold underline">점 세 개(⋮)</span> 메뉴를 눌러 
              <span className="font-bold underline text-primary">"다른 브라우저로 열기"</span> 혹은 <span className="font-bold underline">"Chrome/Safari로 열기"</span>를 선택해 주세요.
            </p>
          </div>
        )}
        <Button
          variant="outline"
          disabled={isInApp}
          className={`w-full flex items-center justify-center gap-3 h-14 rounded-2xl text-base font-bold transition-all border-border/60 ${
            isInApp 
              ? "opacity-50 cursor-not-allowed bg-gray-50 text-muted-foreground" 
              : "hover:bg-white hover:shadow-lg active:scale-95"
          }`}
          onClick={handleGoogleLogin}
        >
          {isInApp ? (
            <ExternalLink className="w-5 h-5 flex-shrink-0" />
          ) : (
            <img src="https://www.google.com/favicon.ico" className="w-5 h-5 flex-shrink-0" alt="Google" />
          )}
          {isInApp ? "외부 브라우저로 접속해 주세요" : "Google로 시작하기"}
        </Button>
        <p className="text-[10px] text-center text-muted-foreground leading-relaxed px-4">
          로그인 시 서비스 이용약관 및 <br className="md:hidden" /> 개인정보 처리방침에 동의하게 됩니다.
        </p>
      </CardContent>
    </Card>
  );
}
