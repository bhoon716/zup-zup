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
import { Sparkles, LogIn } from "lucide-react";
import Image from "next/image";

export function LoginModal() {
  const { isLoginModalOpen, setLoginModalOpen } = useAuthStore();

  const handleLogin = () => {
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
          <Button 
            onClick={handleLogin}
            size="lg"
            className="w-full h-14 rounded-2xl bg-white text-black border border-border/50 hover:bg-gray-50 font-bold shadow-sm transition-all flex items-center justify-center gap-3 active:scale-[0.98]"
          >
            <div className="relative w-5 h-5">
              <Image 
                src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" 
                alt="Google" 
                fill
                className="object-contain"
              />
            </div>
            Google 계정으로 계속하기
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
