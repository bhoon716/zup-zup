import { usePrimaryTimetable } from "@/hooks/useTimetable";
import { TimetableGrid } from "./timetable-grid";
import { Button } from "@/components/ui/button";
import { Calendar, Loader2, Plus } from "lucide-react";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";

export function DashboardTimetable() {
  const { data: timetable, isLoading } = usePrimaryTimetable();

  if (isLoading) {
    return (
      <div className="min-h-[500px] flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-primary/30" />
      </div>
    );
  }

  if (!timetable) {
    return (
      <div className="min-h-[500px] flex flex-col items-center justify-center group">
         <div className="w-16 h-16 rounded-3xl bg-white dark:bg-gray-800 flex items-center justify-center shadow-xl shadow-primary/5 mb-6 group-hover:scale-110 transition-transform duration-500">
            <Calendar className="w-8 h-8 text-primary" />
         </div>
         <h3 className="text-xl font-black tracking-tight mb-2 text-foreground">아직 시간표가 없습니다</h3>
         <p className="text-sm text-muted-foreground font-medium mb-8 text-center max-w-[240px] leading-relaxed">
            나만의 수강 바구니를 구성하고<br />최적의 시간표를 설계해 보세요.
         </p>
         <Link href="/timetable">
            <Button className="rounded-2xl px-6 h-11 bg-primary text-white shadow-lg shadow-primary/20 hover:shadow-primary/40 hover:-translate-y-0.5 transition-all">
               <Plus className="w-4 h-4 mr-2" />
               첫 시간표 만들기
            </Button>
         </Link>
      </div>
    );
  }

  return (
    <div className="w-full h-full select-none grayscale-[0.2] hover:grayscale-0 transition-all duration-700">
       <Link href="/timetable" className="cursor-pointer block h-full">
          <div className="relative h-full rounded-[1.5rem] overflow-hidden border border-border/50 bg-gray-50/50 dark:bg-gray-800/30">
             <div className="scale-[0.98] origin-top transform-gpu h-full">
                <TimetableGrid timetable={timetable} isPreview />
             </div>
             <div className="absolute inset-0 bg-gradient-to-t from-background/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          </div>
       </Link>
    </div>
  );
}
