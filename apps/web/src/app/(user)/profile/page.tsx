"use client";

import { Bell, Bookmark, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { useWithdraw } from "@/features/user/hooks/useUser";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/shared/ui/dialog";
import { ProfileScreen } from "./profile-screen";

const settingsLinks = [
  { href: "/settings", label: "알림 설정", icon: Bell },
  { href: "/profile/subscriptions", label: "구독 설정", icon: Bookmark },
];

export default function ProfilePage() {
  const [isWithdrawDialogOpen, setIsWithdrawDialogOpen] = useState(false);
  const { mutate: withdraw, isPending: isWithdrawing, isError: withdrawFailed, reset: resetWithdraw } = useWithdraw();

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
          <section aria-labelledby="account-management" className="mt-8 border-t pt-5">
            <Dialog
              open={isWithdrawDialogOpen}
              onOpenChange={(open) => {
                if (isWithdrawing) return;
                if (open) resetWithdraw();
                setIsWithdrawDialogOpen(open);
              }}
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <h2 id="account-management" className="text-sm font-semibold">계정 관리</h2>
                  <p className="mt-1 text-sm text-muted-foreground">탈퇴한 계정은 다시 사용할 수 없습니다.</p>
                </div>
                <DialogTrigger asChild>
                  <Button type="button" variant="outline" className="text-destructive hover:text-destructive">
                    회원 탈퇴
                  </Button>
                </DialogTrigger>
              </div>
              <DialogContent role="alertdialog" showCloseButton={false} className="sm:max-w-sm">
                <DialogHeader className="text-left">
                  <DialogTitle>회원 탈퇴를 진행할까요?</DialogTitle>
                  <DialogDescription className="break-keep leading-relaxed">
                    탈퇴한 계정은 다시 사용할 수 없고 모든 기기에서 로그아웃됩니다.
                  </DialogDescription>
                </DialogHeader>
                {withdrawFailed && (
                  <p role="alert" className="text-sm text-destructive">
                    탈퇴를 완료하지 못했습니다. 다시 시도해 주세요.
                  </p>
                )}
                <DialogFooter className="flex-row justify-end">
                  <DialogClose asChild>
                    <Button type="button" variant="outline" disabled={isWithdrawing}>취소</Button>
                  </DialogClose>
                  <Button type="button" variant="destructive" disabled={isWithdrawing} onClick={() => withdraw()}>
                    {isWithdrawing ? "탈퇴 처리 중…" : "회원 탈퇴"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </section>
        </>
      )}
    </ProfileScreen>
  );
}
