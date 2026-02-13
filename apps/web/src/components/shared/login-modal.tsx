"use client";

import { useAuthStore } from "@/store/useAuthStore";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Sparkles, LogIn, AlertCircle, ExternalLink } from "lucide-react";
import Image from "next/image";
import { isInAppBrowser } from "@/lib/utils";
import { useEffect, useState } from "react";

export function LoginModal() {
  const { isLoginModalOpen, setLoginModalOpen } = useAuthStore();
  const [isInApp, setIsInApp] = useState(false);

  useEffect(() => {
    if (isLoginModalOpen) {
      setIsInApp(isInAppBrowser());
    }
  }, [isLoginModalOpen]);

  const handleLogin = () => {
    if (isInApp) return;
    // Redirect to backend OAuth2 endpoint (via Vercel Rewrites)
    window.location.href = `${window.location.origin}/api/oauth2/authorization/google`;
  };

  return (
    <Dialog open={isLoginModalOpen} onOpenChange={setLoginModalOpen}>
      <DialogContent className="sm:max-w-[400px] rounded-[2rem] border-none bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl shadow-2xl">
        <DialogHeader className="flex flex-col items-center justify-center pt-6 pb-2">
          <div className="w-16 h-16 bg-primary/10 rounded-3xl flex items-center justify-center mb-4 animate-bounce-slow">
            <Sparkles className="w-8 h-8 text-primary" />
          </div>
          <DialogTitle className="text-2xl font-black text-center tracking-tight">
            로그인이 필요한 서비스입니다
          </DialogTitle>
          <DialogDescription className="text-center text-muted-foreground font-medium pt-2 leading-relaxed">
            찜하기, 시간표 관리, 실시간 알림 등 <br />
            모든 기능을 이용하려면 로그인이 필요합니다.
          </DialogDescription>
        </DialogHeader>
        
        <div className="flex flex-col gap-3 pb-6 px-2">
          {isInApp && (
            <div className="bg-amber-50 dark:bg-amber-950/20 border border-amber-200 dark:border-amber-800/40 rounded-2xl p-4 mb-2">
              <div className="flex items-center gap-2 text-amber-700 dark:text-amber-400 font-bold text-xs mb-1">
                <AlertCircle className="w-3.5 h-3.5" />
                <span>Google 로그인 제한</span>
              </div>
              <p className="text-[11px] text-amber-600/90 dark:text-amber-500/90 leading-normal font-medium">
                인앱 브라우저에서는 로그인이 불가합니다. <br />
                메뉴에서 <span className="font-bold underline">"다른 브라우저로 열기"</span>를 선택해 주세요.
              </p>
            </div>
          )}
          <Button 
            onClick={handleLogin}
            disabled={isInApp}
            size="lg"
            className={`w-full h-14 rounded-2xl font-bold shadow-sm transition-all flex items-center justify-center gap-3 active:scale-[0.98] ${
              isInApp 
                ? "bg-gray-100 text-muted-foreground border-none cursor-not-allowed opacity-70" 
                : "bg-white text-black border border-border/50 hover:bg-gray-50"
            }`}
          >
            <div className="relative w-5 h-5 flex-shrink-0">
              {isInApp ? (
                <ExternalLink className="w-5 h-5 text-muted-foreground" />
              ) : (
                <Image 
                  src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" 
                  alt="Google" 
                  fill
                  className="object-contain"
                />
              )}
            </div>
            {isInApp ? "외부 브라우저에서 이용" : "Google 계정으로 계속하기"}
          </Button>
          
          <Button 
            variant="ghost" 
            onClick={() => setLoginModalOpen(false)}
            className="w-full h-12 rounded-xl text-muted-foreground font-semibold hover:bg-primary/5 hover:text-primary transition-all"
          >
            다음에 할게요
          </Button>
        </div>

        <div className="absolute top-0 right-0 p-8 -z-10 opacity-30 blur-2xl pointer-events-none">
          <div className="w-32 h-32 bg-primary rounded-full animate-pulse" />
        </div>
      </DialogContent>
    </Dialog>
  );
}
