import { usePrimaryTimetable } from "@/hooks/useTimetable";
import { useWishlist } from "@/hooks/useWishlist";
import { useSubscriptions } from "@/hooks/useSubscriptions";
import { GraduationCap, Heart, Bell, Zap } from "lucide-react";
import { cn } from "@/lib/utils";
import { TimetableDetailResponse } from "@/types/api";

// ... (StatCard 컴플리먼트 내용은 동일하므로 생략하지 않고 전체 유지)
interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ElementType;
  description: string;
  color: string;
  className?: string;
}

function StatCard({ title, value, icon: Icon, description, color, className }: StatCardProps) {
  return (
    <div className={cn(
      "relative overflow-hidden group p-6 rounded-[2rem] bg-white dark:bg-gray-900 border border-border/40 shadow-sm hover:shadow-xl transition-all duration-500",
      className
    )}>
      <div className={cn("absolute top-0 right-0 w-32 h-32 -mr-8 -mt-8 opacity-[0.03] group-hover:opacity-[0.05] transition-opacity duration-500 rounded-full", color)} />
      
      <div className="relative flex flex-col gap-4">
        <div className={cn("w-12 h-12 rounded-2xl flex items-center justify-center shadow-sm group-hover:scale-110 transition-transform duration-500", color)}>
          <Icon className="w-6 h-6 text-white" />
        </div>
        
        <div>
          <div className="text-3xl font-black tracking-tight text-foreground mb-1">
            {value}
          </div>
          <div className="text-[13px] font-bold text-foreground/80 mb-0.5">{title}</div>
          <div className="text-[11px] text-muted-foreground font-medium">{description}</div>
        </div>
      </div>
      
      <div className="absolute bottom-4 right-6 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all duration-500">
        <Zap className="w-4 h-4 text-primary/40" />
      </div>
    </div>
  );
}

export function DashboardStats() {
  const { data: rawTimetable } = usePrimaryTimetable();
  const timetable = rawTimetable as TimetableDetailResponse | null;
  const { data: wishlist } = useWishlist();
  const { data: subscriptions } = useSubscriptions();

  const stats = [
    {
      title: "총 신청 학점",
      value: `${timetable?.totalCredits || 0}학점`,
      icon: GraduationCap,
      description: "현재 대표 시간표 기준",
      color: "bg-[#56296e]",
    },
    {
      title: "찜한 강의",
      value: `${wishlist?.length || 0}개`,
      icon: Heart,
      description: "관심 목록에 담긴 강의",
      color: "bg-rose-500",
    },
    {
      title: "활성 알림",
      value: `${subscriptions?.filter(s => s.isActive).length || 0}개`,
      icon: Bell,
      description: "실시간 빈자리 모니터링",
      color: "bg-indigo-500",
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      {stats.map((stat, idx) => (
        <StatCard key={idx} {...stat} />
      ))}
    </div>
  );
}
