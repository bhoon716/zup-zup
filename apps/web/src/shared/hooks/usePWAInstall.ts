import { useState, useEffect } from 'react';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

/**
 * PWA 설치 기능을 관리하는 커스텀 훅입니다.
 */
export const usePWAInstall = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [isInstallable, setIsInstallable] = useState(false);
  const [platform, setPlatform] = useState<"android" | "ios" | "other" | null>(null);

  useEffect(() => {
    // 플랫폼 감지 및 iOS 여부 확인
    if (typeof window !== "undefined") {
      const userAgent = window.navigator.userAgent.toLowerCase();
      const isStandalone = (window.navigator as any).standalone === true || window.matchMedia("(display-mode: standalone)").matches;

      if (userAgent.includes("iphone") || userAgent.includes("ipad") || userAgent.includes("ipod")) {
        setPlatform("ios");
        // iOS는 standalone이 아니면 항상 "설치 가능(안내 가능)" 상태로 간주
        if (!isStandalone) {
          setIsInstallable(true);
        }
      } else if (userAgent.includes("android")) {
        setPlatform("android");
      } else {
        setPlatform("other");
      }
    }

    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setIsInstallable(true);
    };

    window.addEventListener("beforeinstallprompt", handler);

    return () => {
      window.removeEventListener("beforeinstallprompt", handler);
    };
  }, []);

  /**
   * PWA 설치 프로세스를 실행합니다.
   * 브라우저가 지원하면 자동으로 프롬프트를 띄우고, iOS의 경우 수동 설치 안내를 표시합니다.
   */
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
