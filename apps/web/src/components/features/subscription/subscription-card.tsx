"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useUnsubscribe, useToggleSubscription } from "@/hooks/useSubscriptions";
import type { Subscription } from "@/types/api";
import { Trash2, Bell, BellOff } from "lucide-react";
import { useState } from "react";

interface SubscriptionCardProps {
  subscription: Subscription;
}

export function SubscriptionCard({ subscription }: SubscriptionCardProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const { mutate: toggleSubscription, isPending: isToggling } = useToggleSubscription();

  const handleUnsubscribe = () => {
    unsubscribe(subscription.id, {
      onSuccess: () => setIsDialogOpen(false),
    });
  };

  const handleToggle = (checked: boolean) => {
    toggleSubscription(subscription.id);
  };

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg">{subscription.courseName}</CardTitle>
            <p className="text-sm text-muted-foreground mt-1">
              {subscription.courseKey} | {subscription.professorName}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2">
              {subscription.isActive ? (
                <Bell className="w-4 h-4 text-green-600" />
              ) : (
                <BellOff className="w-4 h-4 text-gray-400" />
              )}
              <Switch
                checked={subscription.isActive}
                onCheckedChange={handleToggle}
                disabled={isToggling}
              />
            </div>
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
              <DialogTrigger asChild>
                <Button variant="ghost" size="icon" className="text-red-600 hover:text-red-700">
                  <Trash2 className="w-4 h-4" />
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>구독 취소</DialogTitle>
                  <DialogDescription>
                    정말로 이 강좌의 구독을 취소하시겠습니까?
                    <br />
                    <strong>{subscription.courseName}</strong>
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                    취소
                  </Button>
                  <Button
                    variant="destructive"
                    onClick={handleUnsubscribe}
                    disabled={isUnsubscribing}
                  >
                    {isUnsubscribing ? "처리 중..." : "구독 취소"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            알림 상태: {subscription.isActive ? "활성화" : "비활성화"}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
