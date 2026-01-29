"use client";

import { useWishlist, useToggleWishlist } from "@/hooks/useWishlist";
import { useSubscriptions, useSubscribe } from "@/hooks/useSubscriptions";
import { Button } from "@/components/ui/button";
import { Heart, Bell, ArrowRight, Loader2, Bookmark } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Subscription, WishlistResponse } from "@/types/api";
import { useState } from "react";
import { CourseDetailDialog } from "./course-detail-dialog";
import { useCourseDetail } from "@/hooks/useCourses";

export function DashboardWishlist() {
  const { data: wishlist, isLoading } = useWishlist();
  const { mutate: toggleWishlist } = useToggleWishlist();
  const { mutate: subscribe } = useSubscribe();
  const { data: subscriptions } = useSubscriptions();
  const [selectedCourseKey, setSelectedCourseKey] = useState<string | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const { data: courseDetail } = useCourseDetail(selectedCourseKey || "");

  const getSubscription = (courseKey: string) => 
    subscriptions?.find((sub: Subscription) => sub.courseKey === courseKey);

  const isSubscribed = (courseKey: string) => 
    getSubscription(courseKey)?.isActive || false;

  const handleToggleSubscription = (courseKey: string) => {
    const sub = getSubscription(courseKey);
    if (!sub) {
      subscribe({ courseKey });
    }
  };
  const handleOpenDetail = (courseKey: string) => {
    setSelectedCourseKey(courseKey);
    setIsDetailOpen(true);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-8 bg-white/50 dark:bg-gray-800/50 rounded-3xl border border-dashed animate-pulse">
        <Loader2 className="w-5 h-5 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!wishlist || wishlist.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-8 bg-white/50 dark:bg-gray-800/50 rounded-3xl border border-dashed group hover:border-primary/30 transition-colors">
        <Bookmark className="w-8 h-8 text-muted-foreground/30 mb-3 group-hover:scale-110 transition-transform" />
        <p className="text-xs text-muted-foreground font-medium">찜한 강의가 없습니다.</p>
        <Link href="/search" className="mt-3">
          <Button variant="ghost" size="xs" className="text-[11px] h-7 px-3 rounded-full hover:bg-primary/5 hover:text-primary transition-colors">
            강의 찾으러 가기
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between px-1">
        <h3 className="text-[13px] font-bold text-foreground/80 flex items-center gap-2">
           <Heart className="w-3.5 h-3.5 text-rose-500 fill-rose-500" />
           찜 목록 <span className="text-primary/70">{wishlist.length}</span>
        </h3>
        <Link href="/search">
           <Button variant="ghost" size="xs" className="h-6 text-[10px] text-muted-foreground hover:text-primary transition-colors gap-1">
              더보기 <ArrowRight className="w-2.5 h-2.5" />
           </Button>
        </Link>
      </div>

      <div className="grid gap-2.5">
        {wishlist.map((item: WishlistResponse) => (
          <div 
            key={item.courseKey}
            className="group flex flex-col p-3.5 bg-white dark:bg-gray-800/40 rounded-2xl border border-transparent hover:border-primary/10 hover:shadow-sm transition-all cursor-pointer"
            onClick={() => handleOpenDetail(item.courseKey)}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                   <Badge variant="outline" className="text-[9px] font-mono font-medium px-1.5 py-0 h-4 border-primary/20 bg-primary/5 text-primary">
                      {item.courseKey}
                   </Badge>
                </div>
                <h4 className="text-[13px] font-bold text-foreground truncate group-hover:text-primary transition-colors">
                  {item.courseName}
                </h4>
                <div className="flex items-center gap-2 mt-1.5 text-[11px] text-muted-foreground font-medium">
                  <span>{item.professor || "교수 미지정"}</span>
                  <span className="w-1 h-1 rounded-full bg-border" />
                  <span>{item.credits}학점</span>
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <Button
                  variant="ghost"
                  size="icon"
                  className="w-8 h-8 rounded-full hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-500/10 transition-colors"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleWishlist(item.courseKey);
                  }}
                  title="찜 해제"
                >
                  <Heart className="w-4 h-4 fill-rose-500 text-rose-500" />
                </Button>
                {isSubscribed(item.courseKey) ? (
                  <div className="flex flex-col items-center gap-1.5 p-2 bg-indigo-50/50 dark:bg-indigo-500/10 rounded-2xl border border-indigo-100/50 dark:border-indigo-500/20">
                     <Bell className="w-4 h-4 text-indigo-600 fill-indigo-600" />
                     <span className="text-[9px] font-black text-indigo-600/80 uppercase tracking-tighter">Monitoring</span>
                  </div>
                ) : (
                  <Button
                    variant="ghost"
                    size="xs"
                    className="h-8 px-3 rounded-xl text-[11px] font-bold text-muted-foreground/60 hover:bg-indigo-50 hover:text-indigo-600 transition-all border border-dashed border-muted-foreground/20 hover:border-indigo-200"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleToggleSubscription(item.courseKey);
                    }}
                  >
                    빈자리 알림 받기
                  </Button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      <CourseDetailDialog 
        course={courseDetail || null} 
        open={isDetailOpen} 
        onOpenChange={setIsDetailOpen} 
      />
    </div>
  );
}
