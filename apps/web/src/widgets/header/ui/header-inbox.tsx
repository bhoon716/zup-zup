"use client";

import { format } from "date-fns";
import { ArrowRight, Bell } from "lucide-react";
import Link from "next/link";
import type { InboxSelection } from "@/features/notification/api/inbox.api";
import { useInbox } from "@/features/notification/hooks/useInbox";
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
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          type="button"
          variant="ghost"
          size="icon-lg"
          aria-label="알림함"
          title="알림함"
          className="rounded-xl text-slate-600 hover:bg-primary/5 hover:text-primary data-[state=open]:bg-primary/10 data-[state=open]:text-primary xl:size-9"
        >
          <Bell aria-hidden="true" className="size-5" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" sideOffset={10} collisionPadding={12} className="w-80 max-w-[calc(100vw-1.5rem)] rounded-2xl border-slate-200 p-0 shadow-lg">
        <DropdownMenuLabel className="border-b border-slate-100 px-4 py-3 font-semibold text-slate-900">
          최근 알림
        </DropdownMenuLabel>
        <RecentInboxItems userId={userId} />
        <div className="border-t border-slate-100 p-1">
          <DropdownMenuItem asChild className="justify-center rounded-xl py-2 font-semibold text-primary focus:text-primary">
            <Link href="/notifications">
              모두 보기
              <ArrowRight aria-hidden="true" className="size-4 text-primary" />
            </Link>
          </DropdownMenuItem>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function RecentInboxItems({ userId }: { userId: number }) {
  const inbox = useInbox(userId, previewSelection);
  const statusClass = "px-4 py-5 text-center text-sm text-slate-600";

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
    <ul className="divide-y divide-slate-100">
      {items.map((item) => (
        <li key={item.id} className="min-w-0 px-4 py-2.5">
          <p className="truncate text-sm font-medium text-slate-900">{getInboxItemTitle(item)}</p>
          <p className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-slate-600">
            <span>{inboxTypeLabels[item.type]}</span>
            <time dateTime={item.occurredAt}>{format(new Date(item.occurredAt), "M월 d일 HH:mm")}</time>
          </p>
        </li>
      ))}
    </ul>
  );
}
