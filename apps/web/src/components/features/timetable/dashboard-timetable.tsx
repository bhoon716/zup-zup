"use client";

import { usePrimaryTimetable } from "@/hooks/useTimetable";
import { TimetableGrid } from "./timetable-grid";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Calendar, ArrowRight, Loader2, Plus, Info, Crown } from "lucide-react";
import Link from "next/link";

export function DashboardTimetable() {
  const { data: timetable, isLoading } = usePrimaryTimetable();

  if (isLoading) {
    return (
      <Card className="border-none shadow-none bg-white/50 dark:bg-gray-800/50 min-h-[500px] flex items-center justify-center rounded-[2rem]">
        <Loader2 className="w-8 h-8 animate-spin text-primary/30" />
      </Card>
    );
  }

  if (!timetable) {
    return (
      <Card className="border-none shadow-none bg-gradient-to-br from-indigo-50/50 to-blue-50/50 dark:from-indigo-500/5 dark:to-blue-500/5 min-h-[500px] flex flex-col items-center justify-center rounded-[2rem] border-2 border-dashed border-primary/10 group hover:border-primary/30 transition-all duration-500">
         <div className="w-16 h-16 rounded-3xl bg-white dark:bg-gray-800 flex items-center justify-center shadow-xl shadow-primary/5 mb-6 group-hover:scale-110 transition-transform duration-500">
            <Calendar className="w-8 h-8 text-primary" />
         </div>
         <h3 className="text-xl font-black tracking-tight mb-2">아직 시간표가 없습니다</h3>
         <p className="text-sm text-muted-foreground font-medium mb-8 text-center max-w-[240px] leading-relaxed">
            나만의 수강 바구니를 구성하고<br />최적의 시간표를 설계해 보세요.
         </p>
         <Link href="/timetable">
            <Button className="rounded-2xl px-6 h-11 bg-primary shadow-lg shadow-primary/20 hover:shadow-primary/40 hover:-translate-y-0.5 transition-all">
               <Plus className="w-4 h-4 mr-2" />
               첫 시간표 만들기
            </Button>
         </Link>
      </Card>
    );
  }

  return (
    <Card className="border-none shadow-none bg-white dark:bg-gray-900 overflow-hidden rounded-[2rem] group">
      <CardHeader className="flex flex-row items-center justify-between pb-6 px-1">
        <div className="space-y-1.5">
          <div className="flex items-center gap-2">
            <div className="w-1.5 h-1.5 rounded-full bg-primary animate-pulse" />
            <span className="text-[10px] font-black uppercase tracking-widest text-primary/60">Representative</span>
          </div>
          <CardTitle className="text-2xl font-black tracking-tight flex items-center gap-2.5">
            {timetable.primary && <Crown className="w-6 h-6 mr-2 text-yellow-500 fill-yellow-500 drop-shadow-md" />}
            {timetable.name}
          </CardTitle>
          <CardDescription className="text-xs font-medium text-muted-foreground flex items-center gap-1.5">
            <Info className="w-3 h-3" />
            시간표 이미지를 클릭하여 상세 관리 페이지로 이동합니다.
          </CardDescription>
        </div>
        <Link href="/timetable">
          <Button variant="ghost" size="sm" className="rounded-xl px-4 h-10 font-bold hover:bg-primary/5 hover:text-primary transition-all group/btn">
            전체 보기
            <ArrowRight className="w-4 h-4 ml-2 group-hover/btn:translate-x-1 transition-transform" />
          </Button>
        </Link>
      </CardHeader>
      <CardContent className="p-0 select-none grayscale-[0.2] hover:grayscale-0 transition-all duration-700">
         <Link href="/timetable" className="cursor-pointer">
            <div className="relative rounded-[1.5rem] overflow-hidden border border-border/50 bg-gray-50/50 dark:bg-gray-800/30">
               <div className="scale-[0.98] origin-top transform-gpu">
                  <TimetableGrid timetable={timetable} isPreview />
               </div>
               <div className="absolute inset-0 bg-gradient-to-t from-background/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
            </div>
         </Link>
      </CardContent>
    </Card>
  );
}

// Badge is not imported but used, let me add it.
import { Badge } from "@/components/ui/badge";
