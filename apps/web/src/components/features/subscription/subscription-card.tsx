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

  const handleToggle = () => {
    toggleSubscription(subscription.id);
  };

  return (
    <Card className="bg-card/40 backdrop-blur-xl border-white/10 shadow-xl transition-all hover:shadow-2xl hover:bg-card/50 group relative overflow-hidden">
      <div className="absolute top-0 left-0 w-1 h-full bg-primary/40 group-hover:bg-primary transition-colors" />
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg font-bold group-hover:text-primary transition-colors">{subscription.courseName}</CardTitle>
            <p className="text-xs text-muted-foreground mt-1.5 flex items-center gap-2">
              <span className="font-mono bg-muted/50 px-1.5 py-0.5 rounded text-[10px]">{subscription.courseKey}</span>
              <span className="w-1 h-1 rounded-full bg-muted-foreground/30" />
              <span>{subscription.professorName}</span>
            </p>
          </div>
          <div className="flex items-center gap-1.5">
            <div className="flex items-center gap-2.5 bg-background/30 px-2.5 py-1.5 rounded-xl border border-white/5">
              {subscription.isActive ? (
                <Bell className="w-4 h-4 text-primary animate-pulse" />
              ) : (
                <BellOff className="w-4 h-4 text-muted-foreground/50" />
              )}
              <Switch
                checked={subscription.isActive}
                onCheckedChange={handleToggle}
                disabled={isToggling}
                className="scale-90"
              />
            </div>
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
              <DialogTrigger asChild>
                <Button variant="ghost" size="icon" className="text-muted-foreground hover:text-red-500 hover:bg-red-500/10 rounded-xl transition-colors">
                  <Trash2 className="w-4 h-4" />
                </Button>
              </DialogTrigger>
              <DialogContent className="bg-card/90 backdrop-blur-2xl border-white/10">
                <DialogHeader>
                  <DialogTitle>구독 취소</DialogTitle>
                  <DialogDescription>
                    정말로 이 강의의 구독을 취소하시겠습니까?
                    <br />
                    <span className="text-foreground font-semibold">[{subscription.courseKey}] {subscription.courseName}</span>
                  </DialogDescription>
                </DialogHeader>
                <DialogFooter className="gap-2">
                  <Button variant="outline" onClick={() => setIsDialogOpen(false)} className="rounded-xl border-white/10">
                    취소
                  </Button>
                  <Button
                    variant="destructive"
                    onClick={handleUnsubscribe}
                    disabled={isUnsubscribing}
                    className="rounded-xl"
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
        <div className="flex items-center justify-between text-[11px] font-medium text-muted-foreground">
          <div className="flex items-center gap-1.5">
            <div className={`w-2 h-2 rounded-full ${subscription.isActive ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]' : 'bg-muted-foreground/30'}`} />
            <span>{subscription.isActive ? "알림 활성화됨" : "알림 꺼짐"}</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
