"use client";

import { Loader2 } from "lucide-react";
import type { ReactNode } from "react";
import { Dashboard } from "@/widgets/home/dashboard";
import { useDashboardSnapshot } from "@/widgets/home/hooks/useDashboard";
import { useAuthStore } from "@/features/auth/store/useAuthStore";

export function HomeSession({ children, hasSessionCookie }: { children: ReactNode; hasSessionCookie: boolean }) {
  const user = useAuthStore((state) => state.user);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isAuthLoading = useAuthStore(
    (state) => (state.isLoading && hasSessionCookie) || Boolean(state.user && !state.isAuthenticated)
  );
  const verifiedUser = isAuthenticated ? user : null;
  const { data: snapshot, isLoading, isError, refetch } = useDashboardSnapshot({ enabled: isAuthenticated });

  if (isAuthLoading || (verifiedUser && isLoading)) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center bg-[#fafafa] dark:bg-black">
        <div role="status">
          <Loader2 aria-hidden="true" className="w-8 h-8 animate-spin text-primary" />
          <span className="sr-only">메인 화면을 불러오는 중입니다.</span>
        </div>
      </div>
    );
  }

  if (verifiedUser && isError) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center bg-[#fafafa] dark:bg-black px-4">
        <div className="rounded-3xl border border-red-100 bg-white p-8 text-center shadow-sm">
          <p className="text-sm font-bold text-slate-900">메인 화면을 불러오지 못했습니다.</p>
          <p className="mt-2 text-xs text-slate-500">잠시 후 다시 시도해 주세요.</p>
          <button type="button" onClick={() => void refetch()} className="mt-4 inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2 text-sm font-bold text-white">
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  if (verifiedUser && snapshot) return <Dashboard snapshot={snapshot} />;

  return children;
}
