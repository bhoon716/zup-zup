"use client";

import { Card, CardContent, CardHeader } from "@/components/ui/card";
import type { NotificationHistory } from "@/types/api";
import { Bell } from "lucide-react";
import { formatRelativeTime } from "@/lib/utils/formatters";

interface NotificationCardProps {
  notification: NotificationHistory;
}

export function NotificationCard({ notification }: NotificationCardProps) {
  return (
    <Card className="transition-all hover:shadow-md border border-border/40 bg-card/60 backdrop-blur-md rounded-2xl overflow-hidden active:scale-[0.99]">
      <CardHeader className="p-4 pb-2">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 flex-1">
            <div className="w-8 h-8 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
               <Bell className="w-4 h-4 text-primary" />
            </div>
            <h3 className="font-bold text-sm md:text-base text-foreground leading-tight">{notification.title}</h3>
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-4 pt-0">
        <p className="text-xs md:text-sm text-muted-foreground mb-3 leading-relaxed">{notification.message}</p>
        <div className="flex items-center justify-between text-[10px] md:text-xs text-muted-foreground/60 font-medium">
          <span className="bg-muted px-2 py-0.5 rounded-full uppercase tracking-tighter">{notification.courseKey}</span>
          <span>
            {formatRelativeTime(notification.createdAt)}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
