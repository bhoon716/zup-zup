"use client";

import { useSubscriptions } from "@/hooks/useSubscriptions";
import { SubscriptionCard } from "./subscription-card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { Search, Loader2 } from "lucide-react";

export function SubscriptionList({ minimal = false }: { minimal?: boolean }) {
  const { data: subscriptions, isLoading, error } = useSubscriptions();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-6">
        <Loader2 className="w-6 h-6 animate-spin text-primary/30" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-6">
        <p className="text-red-600 text-xs mb-2">목록 로드 실패</p>
        <Button size="xs" onClick={() => window.location.reload()}>재시도</Button>
      </div>
    );
  }

  if (!subscriptions || subscriptions.length === 0) {
    return (
      <div className="text-center py-10 px-4 flex flex-col items-center">
        <Search className="w-12 h-12 mx-auto text-gray-200 mb-4" />
        <p className="text-xs text-muted-foreground font-medium leading-relaxed">
          구독 중인 강의가 없습니다.<br />검색에서 알림을 설정하세요.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {!minimal && (
        <div className="flex items-center justify-between px-1 mb-4">
          <div className="flex flex-col gap-1.5">
            <h2 className="text-2xl font-black tracking-tight">내 구독 목록</h2>
            <p className="text-xs text-muted-foreground font-medium">실시간 빈자리 감시 중</p>
          </div>
        </div>
      )}
      <div className={minimal ? "grid gap-3" : "grid gap-4 md:grid-cols-2 lg:grid-cols-3"}>
        {subscriptions.map((subscription) => (
          <SubscriptionCard key={subscription.id} subscription={subscription} />
        ))}
      </div>
    </div>
  );
}
