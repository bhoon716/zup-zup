"use client";

import { useSubscriptions } from "@/hooks/useSubscriptions";
import { SubscriptionCard } from "./subscription-card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { Search, Loader2 } from "lucide-react";

export function SubscriptionList() {
  const { data: subscriptions, isLoading, error } = useSubscriptions();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600 mb-4">구독 목록을 불러오는데 실패했습니다.</p>
        <Button onClick={() => window.location.reload()}>다시 시도</Button>
      </div>
    );
  }

  if (!subscriptions || subscriptions.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="mb-4">
          <Search className="w-16 h-16 mx-auto text-gray-300" />
        </div>
        <h3 className="text-lg font-semibold mb-2">구독 중인 강좌가 없습니다</h3>
        <p className="text-muted-foreground mb-6">
          강좌를 검색하고 구독하여 빈자리 알림을 받아보세요.
        </p>
        <Link href="/search">
          <Button className="gap-2">
            <Search className="w-4 h-4" />
            강좌 검색하기
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-2xl font-bold">내 구독 목록</h2>
        <span className="text-sm text-muted-foreground">
          총 {subscriptions.length}개 구독 중
        </span>
      </div>
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {subscriptions.map((subscription) => (
          <SubscriptionCard key={subscription.id} subscription={subscription} />
        ))}
      </div>
    </div>
  );
}
