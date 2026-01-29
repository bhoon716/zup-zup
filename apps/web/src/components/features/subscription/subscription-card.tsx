"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useUnsubscribe } from "@/hooks/useSubscriptions";
import type { Subscription } from "@/types/api";
import { Trash2, Bell } from "lucide-react";
import { useState } from "react";
import { CourseDetailDialog } from "../course/course-detail-dialog";
import { useCourseDetail } from "@/hooks/useCourses";

interface SubscriptionCardProps {
  subscription: Subscription;
}

export function SubscriptionCard({ subscription }: SubscriptionCardProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const { mutate: unsubscribe, isPending: isUnsubscribing } = useUnsubscribe();
  const { data: courseDetail } = useCourseDetail(subscription.courseKey);

  const handleUnsubscribe = () => {
    unsubscribe(subscription.id, {
      onSuccess: () => setIsDialogOpen(false),
    });
  };

  const handleOpenDetail = () => {
    setIsDetailOpen(true);
  };


  return (
    <>
      <Card 
        className="bg-card/40 backdrop-blur-xl border-white/10 shadow-xl transition-all hover:shadow-2xl hover:bg-card/50 group relative overflow-hidden cursor-pointer"
        onClick={handleOpenDetail}
      >
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
              <div className="flex items-center gap-2 bg-indigo-50/50 dark:bg-indigo-500/10 px-3 py-1.5 rounded-xl border border-indigo-100/20 dark:border-indigo-500/20">
                <Bell className="w-4 h-4 text-indigo-600 animate-pulse" />
                <span className="text-[10px] font-black text-indigo-600 uppercase tracking-wider">
                  Monitoring
                </span>
              </div>
              <Button 
                variant="ghost" 
                size="icon" 
                className="text-muted-foreground hover:text-red-500 hover:bg-red-500/10 rounded-xl transition-colors"
                onClick={(e) => {
                  e.stopPropagation();
                  setIsDialogOpen(true);
                }}
              >
                <Trash2 className="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-between text-[11px] font-medium text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <div className="w-2 h-2 rounded-full bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.4)]" />
              <span>실시간 빈자리 감시 중</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 구독 취소 확인 다이얼로그 (이벤트 버블링 방지를 위해 Card 외부 배치) */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent 
          className="bg-card/90 backdrop-blur-2xl border-white/10"
          onClick={(e) => e.stopPropagation()}
        >
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

      {/* 강의 상세 다이얼로그 */}
      <CourseDetailDialog 
        course={courseDetail || null} 
        open={isDetailOpen} 
        onOpenChange={setIsDetailOpen} 
      />
    </>
  );
}
