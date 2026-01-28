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
    <Card className="transition-all hover:shadow-md">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 flex-1">
            <Bell className="w-4 h-4 text-blue-600" />
            <h3 className="font-semibold text-sm">{notification.title}</h3>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-2">{notification.message}</p>
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span className="font-medium">{notification.courseKey}</span>
          <span>
            {formatRelativeTime(notification.createdAt)}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
