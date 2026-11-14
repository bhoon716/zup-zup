"use client";

import { Bell, Bookmark, ChevronRight, CircleUserRound } from "lucide-react";
import Link from "next/link";
import { Card } from "@/shared/ui/card";
import { ProfileScreen } from "./profile-screen";

const settingsLinks = [
  { href: "/settings", label: "알림 설정", icon: Bell, description: "알림을 받을 채널과 기기를 관리해요." },
  { href: "/profile/subscriptions", label: "구독 설정", icon: Bookmark, description: "빈자리 알림을 구독한 강의를 관리해요." },
];

export default function ProfilePage() {
  return (
    <ProfileScreen title="프로필">
      {(user) => (
        <>
          <Card className="mb-5 flex-row items-center gap-4 p-4 sm:mb-6 sm:p-5">
            <span aria-hidden="true" className="grid size-12 shrink-0 place-items-center rounded-full bg-primary/10 text-primary">
              <CircleUserRound className="size-6" />
            </span>
            <div className="min-w-0 wrap-anywhere">
              <p className="text-base font-semibold">{user.name}</p>
              <p className="mt-0.5 text-sm leading-5 text-muted-foreground">{user.email}</p>
            </div>
          </Card>
          <nav aria-label="프로필 설정" className="rounded-xl border bg-card">
            <ul className="divide-y">
              {settingsLinks.map(({ href, label, icon: Icon, description }) => (
                <li key={href}>
                  <Link
                    href={href}
                    aria-label={label}
                    className="group flex min-h-18 items-center gap-3 rounded-lg px-4 py-3 hover:bg-muted/60 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary sm:gap-4 sm:px-5"
                  >
                    <Icon aria-hidden="true" className="size-5 shrink-0 text-primary" />
                    <span className="min-w-0 flex-1">
                      <span className="block text-sm font-semibold">{label}</span>
                      <span className="mt-1 block break-keep text-sm leading-5 text-muted-foreground">{description}</span>
                    </span>
                    <ChevronRight aria-hidden="true" className="size-4 shrink-0 text-muted-foreground group-hover:text-primary" />
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
        </>
      )}
    </ProfileScreen>
  );
}
