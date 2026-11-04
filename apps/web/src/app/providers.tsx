"use client";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { registerAuthFailureHandler } from "@/shared/api/client";
import { getFirebaseApp } from "@/shared/lib/firebase";
import { resolveAllowedPwaUrl } from "@/shared/lib/pwa-navigation";
import { MutationCache, QueryCache, QueryClient, QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState, Suspense } from "react";
import { Toaster, toast } from "sonner";
import { LoginModal } from "@/widgets/auth/login-modal";
import { reportClientError } from "@/shared/telemetry/client-error";

import { usePathname, useRouter } from "next/navigation";
import { TooltipProvider } from "@/shared/ui/tooltip";

const createQueryClient = () =>
  new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => reportClientError(error, { source: "react-query", operation: "query" }),
    }),
    mutationCache: new MutationCache({
      onError: (error) => reportClientError(error, { source: "react-query", operation: "mutation" }),
    }),
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        refetchOnWindowFocus: false,

        retry: (failureCount, error: unknown) => {
          if (error && typeof error === "object" && "response" in error) {
            const responseError = error as { response?: { status?: number } };
            if (responseError.response?.status === 401) return false;
          }
          return failureCount < 3;
        },
      },
    },
  });

let browserQueryClient: QueryClient | undefined;

export const getAppQueryClient = () => {
  if (typeof window === "undefined") {
    return createQueryClient();
  }

  if (!browserQueryClient) {
    browserQueryClient = createQueryClient();
  }

  return browserQueryClient;
};




function OnboardingGuard({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isLoading = useAuthStore((state) => state.isLoading);
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && isAuthenticated && user && !user.onboardingCompleted) {
      if (pathname !== "/onboarding") {
        router.replace("/onboarding");
      }
    }
  }, [user, isAuthenticated, isLoading, pathname, router]);

  if (!isLoading && isAuthenticated && user && !user.onboardingCompleted && pathname !== "/onboarding") {
    return null;
  }

  return <>{children}</>;
}




function AuthProvider({ children }: { children: React.ReactNode }) {
  const checkSession = useAuthStore((state) => state.checkSession);
  const queryClient = useQueryClient();

  useEffect(() => {
    registerAuthFailureHandler(() => {
      const authState = useAuthStore.getState();
      queryClient.clear();
      authState.logout();
      authState.setLoginModalOpen(true);
    });


    getFirebaseApp();


    void checkSession();

    const revalidateRestoredSession = () => {
      const authState = useAuthStore.getState();
      if (authState.user && !authState.isAuthenticated) {
        void authState.checkSession();
      }
    };
    window.addEventListener("focus", revalidateRestoredSession);
    window.addEventListener("online", revalidateRestoredSession);


    if (typeof window !== "undefined" && "serviceWorker" in navigator) {
      window.addEventListener("load", () => {
        navigator.serviceWorker.register("/sw.js").then(
          () => {},
          (err) => {
            reportClientError(err, { source: "service-worker", operation: "registration" });
          }
        );
      });
    }

    const handleServiceWorkerMessage = (event: MessageEvent) => {
      if (event.data && event.data.type === 'PUSH_NOTIFICATION') {

        const { title, body } = event.data;
        toast.info(title, {
          description: body,
          duration: 5000,
          action: {
            label: '보기',
            onClick: () => {

              // eslint-disable-next-line @next/next/no-location-assign-relative-destination
              window.location.href = resolveAllowedPwaUrl(event.data.url, window.location.origin);
            },
          }
        });
      }
    };

    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);
    }

    return () => {
      window.removeEventListener("focus", revalidateRestoredSession);
      window.removeEventListener("online", revalidateRestoredSession);
      if ('serviceWorker' in navigator) {
        navigator.serviceWorker.removeEventListener('message', handleServiceWorkerMessage);
      }
    };
  }, [checkSession, queryClient]);

  return <OnboardingGuard>{children}</OnboardingGuard>;
}





export default function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(getAppQueryClient);

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
