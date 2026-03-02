"use client";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { getFirebaseApp } from "@/shared/lib/firebase";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, Suspense } from "react";
import { Toaster, toast } from "sonner";
import { LoginModal } from "@/widgets/auth/login-modal";

import { useUser } from "@/features/user/hooks/useUser";
import { usePathname, useRouter } from "next/navigation";
import { TooltipProvider } from "@/shared/ui/tooltip";

/**
 * 온보딩 완료 여부를 체크하여 미완료 시 온보딩 페이지로 강제 이동시키는 가드 컴포넌트입니다.
 */
function OnboardingGuard({ children }: { children: React.ReactNode }) {
  const { data: user, isLoading } = useUser();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && user && !user.onboardingCompleted) {
      if (pathname !== "/onboarding") {
        router.replace("/onboarding");
      }
    }
  }, [user, isLoading, pathname, router]);

  if (!isLoading && user && !user.onboardingCompleted && pathname !== "/onboarding") {
    return null;
  }

  return <>{children}</>;
}

/**
 * 세션 상태 및 서비스 워커 푸시 알림 이벤트를 처리하는 인증 프로바이더입니다.
 */
function AuthProvider({ children }: { children: React.ReactNode }) {
  const checkSession = useAuthStore((state) => state.checkSession);

  useEffect(() => {
    // Firebase SDK를 앱 시작 시 한 번 초기화한다.
    getFirebaseApp();
    checkSession();

    // 서비스 워커 등록 (PWA 설치 가능성 확보)
    if (typeof window !== "undefined" && "serviceWorker" in navigator) {
      window.addEventListener("load", () => {
        navigator.serviceWorker.register("/sw.js").then(
          () => {},
          (err) => {
            console.error("ServiceWorker registration failed: ", err);
          }
        );
      });
    }

    const handleServiceWorkerMessage = (event: MessageEvent) => {
      if (event.data && event.data.type === 'PUSH_NOTIFICATION') {
        // 서비스 워커 메시지를 인앱 토스트로 연결한다.
        const { title, body } = event.data;
        toast.info(title, {
          description: body,
          duration: 5000,
          action: {
            label: '보기',
            onClick: () => {
              if (event.data.url) window.location.href = event.data.url;
            }
          }
        });
      }
    };

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);
    }

    return () => {
      if ('serviceWorker' in navigator) {
        navigator.serviceWorker.removeEventListener('message', handleServiceWorkerMessage);
      }
    };
  }, [checkSession]);

  return <OnboardingGuard>{children}</OnboardingGuard>;
}


/**
 * React Query, Toaster, Tooltip 등 전역 상태 및 UI 프로바이더들을 통합 관리하는 컴포넌트입니다.
 */
export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            refetchOnWindowFocus: false,
            // 401 에러는 재시도해도 실패할 가능성이 높으므로 즉시 중단한다.
            retry: (failureCount, error: any) => {
              if (error?.response?.status === 401) return false;
              return failureCount < 3;
            },
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster position="top-right" richColors />
        <Suspense fallback={null}>
          <AuthProvider>
            {children}
            <LoginModal />
          </AuthProvider>
        </Suspense>
      </TooltipProvider>
    </QueryClientProvider>
  );
}
