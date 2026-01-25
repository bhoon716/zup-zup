"use client";

import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { NotificationHistory } from "@/types/api";
import { useMarkAsRead } from "@/hooks/useNotifications";
import { Bell, Mail, Globe } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { ko } from "date-fns/locale";

interface NotificationCardProps {
  notification: NotificationHistory;
}

export function NotificationCard({ notification }: NotificationCardProps) {
  const { mutate: markAsRead } = useMarkAsRead();

  const handleClick = () => {
    if (!notification.isRead) {
      markAsRead(notification.id);
    }
  };

  const getIcon = () => {
    switch (notification.type) {
      case "FCM":
        return <Bell className="w-4 h-4" />;
      case "EMAIL":
        return <Mail className="w-4 h-4" />;
      case "WEB_PUSH":
        return <Globe className="w-4 h-4" />;
    }
  };

  const getTypeLabel = () => {
    switch (notification.type) {
      case "FCM":
        return "푸시";
      case "EMAIL":
        return "이메일";
      case "WEB_PUSH":
        return "웹푸시";
    }
  };

  return (
    <Card
      className={`cursor-pointer transition-all hover:shadow-md ${
        !notification.isRead ? "bg-blue-50 dark:bg-blue-950" : ""
      }`}
      onClick={handleClick}
    >
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 flex-1">
            {getIcon()}
            <h3 className="font-semibold text-sm">{notification.title}</h3>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="outline" className="text-xs">
              {getTypeLabel()}
            </Badge>
            {!notification.isRead && (
              <div className="w-2 h-2 bg-blue-600 rounded-full" />
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-2">{notification.message}</p>
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span className="font-medium">{notification.courseName}</span>
          <span>
            {formatDistanceToNow(new Date(notification.createdAt), {
              addSuffix: true,
              locale: ko,
            })}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
