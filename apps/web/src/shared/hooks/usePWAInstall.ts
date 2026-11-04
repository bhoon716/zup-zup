import { useState, useEffect } from 'react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}




export const usePWAInstall = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isInstallable, setIsInstallable] = useState(false);
  const [platform, setPlatform] = useState<"android" | "ios" | "other" | null>(null);

  useEffect(() => {
    const isDevelopment = process.env.NODE_ENV === "development";


    let timeoutId: number | undefined;
    if (typeof window !== "undefined") {
      const userAgent = window.navigator.userAgent.toLowerCase();
      const isStandalone = (window.navigator as unknown as { standalone?: boolean }).standalone === true || window.matchMedia("(display-mode: standalone)").matches;


      timeoutId = window.setTimeout(() => {
        if (userAgent.includes("iphone") || userAgent.includes("ipad") || userAgent.includes("ipod")) {
          setPlatform("ios");

          if (!isStandalone) {
            setIsInstallable(true);
          }
        } else if (userAgent.includes("android")) {
          setPlatform("android");
        } else {
          setPlatform("other");
        }
      }, 0);
    }

    if (isDevelopment) {
      return () => {
        if (timeoutId) window.clearTimeout(timeoutId);
      };
    }

    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setIsInstallable(true);
    };

    window.addEventListener("beforeinstallprompt", handler);

    return () => {
      if (timeoutId) window.clearTimeout(timeoutId);
      window.removeEventListener("beforeinstallprompt", handler);
    };
  }, []);





  const install = async () => {
    if (platform === "ios") {
      alert("아이폰/아이패드에서는 Safari 하단 중앙의 '공유' 아이콘(□↑)을 누른 후, 리스트에서 '홈 화면에 추가'를 선택해 주세요.");
      return;
    }

    if (!deferredPrompt) {
      alert("이미 설치되어 있거나 브라우저 설정에서 '홈 화면에 추가'를 선택해 주세요.");
      return;
    }

    deferredPrompt.prompt();
    const result = await deferredPrompt.userChoice;

    if (result.outcome === "accepted") {
      setDeferredPrompt(null);
      setIsInstallable(false);
    }
  };

  return { isInstallable, install, platform };
};
