"use client";

import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import type { NotificationHistory } from "@/types/api";
import { Bell, Mail, Globe } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { ko } from "date-fns/locale";

interface NotificationCardProps {
  notification: NotificationHistory;
}

export function NotificationCard({ notification }: NotificationCardProps) {
  const getIcon = () => {
    switch (notification.channel) {
      case "FCM":
        return <Bell className="w-4 h-4" />;
      case "EMAIL":
        return <Mail className="w-4 h-4" />;
      case "WEB":
        return <Globe className="w-4 h-4" />;
      default:
        return <Bell className="w-4 h-4" />;
    }
  };

  const getChannelLabel = () => {
    switch (notification.channel) {
      case "FCM":
        return "푸시";
      case "EMAIL":
        return "이메일";
      case "WEB":
        return "웹푸시";
      default:
        return "기타";
    }
  };

  return (
    <Card className="transition-all hover:shadow-md">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2 flex-1">
            {getIcon()}
            <h3 className="font-semibold text-sm">{notification.title}</h3>
          </div>
          <Badge variant="outline" className="text-xs">
            {getChannelLabel()}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-2">{notification.message}</p>
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <span className="font-medium">{notification.courseKey}</span>
          <span>
            {formatDistanceToNow(new Date(notification.sentAt), {
              addSuffix: true,
              locale: ko,
            })}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
