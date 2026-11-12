"use client";

import { format } from "date-fns";
import { ArrowRight, Bell } from "lucide-react";
import Link from "next/link";
import { useId } from "react";
import type { InboxSelection } from "@/features/notification/api/inbox.api";
import { useHasUnreadInbox, useInbox } from "@/features/notification/hooks/useInbox";
import { getInboxItemTitle, inboxTypeLabels } from "@/features/notification/lib/inbox-display";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";

const previewSelection = { page: 0, read: "ALL", type: "ALL", keyword: "", sort: "NEWEST" } satisfies InboxSelection;
const previewLimit = 5;

export function HeaderInbox({ userId }: { userId: number }) {
  const { data: hasUnread } = useHasUnreadInbox(userId);
  const unreadDescriptionId = useId();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          aria-label="알림함"
          aria-describedby={hasUnread ? unreadDescriptionId : undefined}
          title="알림함"
          className="rounded-[8px] bg-primary/5 text-muted-foreground hover:bg-primary/10 hover:text-primary data-[state=open]:bg-primary/15 data-[state=open]:text-primary xl:size-9"
        >
          <span className="relative">
            <Bell aria-hidden="true" className="size-5 xl:size-4" />
            {hasUnread && (
              <span id={unreadDescriptionId} className="absolute -top-0.5 -right-0.5 size-2 rounded-full bg-destructive ring-2 ring-card">
                <span className="sr-only">읽지 않은 알림이 있습니다.</span>
              </span>
            )}
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" sideOffset={10} collisionPadding={12} className="w-80 max-w-[calc(100vw-1.5rem)] rounded-sm p-0 shadow-lg">
        <div className="flex items-center justify-between gap-2 border-b py-1.5 pr-2 pl-4">
          <DropdownMenuLabel className="p-0 font-semibold text-foreground">
            최근 알림
          </DropdownMenuLabel>
          <DropdownMenuItem asChild className="h-9 cursor-pointer font-medium text-primary hover:bg-accent focus:text-primary">
            <Link href="/notifications">
              모두 보기
              <ArrowRight aria-hidden="true" className="size-4 text-primary" />
            </Link>
          </DropdownMenuItem>
        </div>
        <RecentInboxItems userId={userId} />
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function RecentInboxItems({ userId }: { userId: number }) {
  const inbox = useInbox(userId, previewSelection);
  const statusClass = "px-4 py-5 text-center text-sm text-muted-foreground";

  if (inbox.isError) {
    return <p role="alert" className={statusClass}>알림을 불러오지 못했습니다.</p>;
  }

  if (inbox.isPending) {
    return <p role="status" className={statusClass}>알림을 불러오는 중입니다.</p>;
  }

  const items = inbox.data.content.slice(0, previewLimit);
  if (items.length === 0) {
    return <p className={statusClass}>받은 알림이 없습니다.</p>;
  }

  return (
    <ul className="divide-y divide-border">
      {items.map((item) => (
        <li key={item.id} className="min-w-0 px-4 py-2.5">
          <p className="truncate text-sm font-medium text-foreground">{getInboxItemTitle(item)}</p>
          <p className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-muted-foreground">
            <span>{inboxTypeLabels[item.type]}</span>
            <time dateTime={item.occurredAt}>{format(new Date(item.occurredAt), "M월 d일 HH:mm")}</time>
          </p>
        </li>
      ))}
    </ul>
  );
}
