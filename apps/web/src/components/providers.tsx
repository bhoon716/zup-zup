"use client";

import { useAuthStore } from "@/store/useAuthStore";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState, Suspense } from "react";
import { Toaster, toast } from "sonner";

import { useUser } from "@/hooks/useUser";
import { usePathname, useRouter } from "next/navigation";

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

function AuthProvider({ children }: { children: React.ReactNode }) {
  const checkSession = useAuthStore((state) => state.checkSession);

  useEffect(() => {
    checkSession();

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

import { LoginModal } from "./shared/login-modal";

export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,
            refetchOnWindowFocus: false,
          },
        },
      })
  );

  return (
    <QueryClientProvider client={queryClient}>
      <Toaster position="top-right" richColors />
      <Suspense fallback={null}>
        <AuthProvider>
          {children}
          <LoginModal />
        </AuthProvider>
      </Suspense>
    </QueryClientProvider>
  );
}
