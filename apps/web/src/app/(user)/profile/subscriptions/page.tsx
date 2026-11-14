"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { SubscriptionList } from "@/features/subscription/components/subscription-list";
import { Button } from "@/shared/ui/button";
import { ProfileScreen } from "../profile-screen";

export default function SubscriptionSettingsPage() {
  return (
    <ProfileScreen
      title="구독 설정"
      navigation={(
        <Button asChild variant="ghost" size="sm" className="-ml-2.5 mb-3 text-muted-foreground">
          <Link href="/profile">
            <ArrowLeft aria-hidden="true" />
            설정으로 돌아가기
          </Link>
        </Button>
      )}
    >
      {(user) => (
        <section aria-labelledby="course-subscriptions-heading" className="rounded-xl border bg-card">
          <h2 id="course-subscriptions-heading" className="sr-only">구독 중인 강의</h2>
          <SubscriptionList key={user.id} user={user} />
        </section>
      )}
    </ProfileScreen>
  );
}
