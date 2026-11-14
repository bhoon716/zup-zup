"use client";

import { Bell, Bookmark, ChevronRight } from "lucide-react";
import Link from "next/link";
import { ProfileScreen } from "./profile-screen";

const settingsLinks = [
  { href: "/settings", label: "알림 설정", icon: Bell },
  { href: "/profile/subscriptions", label: "구독 설정", icon: Bookmark },
];

export default function ProfilePage() {
  return (
    <ProfileScreen title="프로필">
      {(user) => (
        <>
          <div className="mb-5 wrap-anywhere sm:mb-6">
            <p className="font-semibold">{user.name}</p>
            <p className="mt-0.5 text-sm text-muted-foreground">{user.email}</p>
          </div>
          <nav aria-label="프로필 설정" className="rounded-xl border bg-card">
            <ul className="divide-y">
              {settingsLinks.map(({ href, label, icon: Icon }) => (
                <li key={href} className="first:[&>a]:rounded-t-xl last:[&>a]:rounded-b-xl">
                  <Link
                    href={href}
                    className="group flex min-h-13 items-center gap-3 px-4 py-3 text-sm font-medium hover:bg-muted/60 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary sm:px-5"
                  >
                    <Icon aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
                    {label}
                    <ChevronRight aria-hidden="true" className="ml-auto size-4 shrink-0 text-muted-foreground group-hover:text-primary" />
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
