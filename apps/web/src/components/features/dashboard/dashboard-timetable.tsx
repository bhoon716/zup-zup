"use client";

import { motion } from "framer-motion";
import { ExternalLink, Calendar } from "lucide-react";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { cn } from "@/lib/utils";

/**
 * 대시보드에서 사용자의 대표 시간표를 시각적으로 요약하여 보여줍니다.
 */
export function DashboardTimetable({ timetable }: { timetable: any }) {
  const days = ["월", "화", "수", "목", "금", "토"];
  
  // 시간 계산 헬퍼
  const toMin = (t: string) => {
    if (!t) return 0;
    const [h, m] = t.split(':').map(Number);
    return h * 60 + (m || 0);
  };

  // 동적 시간 범위 계산 (기본 18시, 필요시 연장)
  const allSchedules: any[] = [];
  timetable?.courses?.forEach((c: any) => c.schedules?.forEach((s: any) => allSchedules.push(s)));
  timetable?.customSchedules?.forEach((s: any) => allSchedules.push(s));
  
  const maxEndMin = allSchedules.reduce((max, s) => Math.max(max, toMin(s.endTime)), 0);
  const maxEndHour = Math.ceil(maxEndMin / 60);
  const endHour = Math.max(18, maxEndHour);
  const startHour = 9;
  const range = endHour - startHour;
  const hours = Array.from({ length: range + 1 }, (_, i) => i + startHour);

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-7 shadow-floating border border-gray-50 dark:border-gray-800 h-full flex flex-col min-h-[600px]">
      <div className="flex justify-between items-center mb-5">
        <h3 className="text-xl font-bold text-gray-900 dark:text-gray-100 flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-blue-50 dark:bg-blue-950/30 flex items-center justify-center">
            <Calendar className="w-5 h-5 text-blue-600" />
          </div>
          대표 시간표
        </h3>
        <Link href="/timetable">
          <Button variant="ghost" size="sm" className="text-xs font-bold text-primary hover:text-primary-dark hover:bg-primary/5 transition-colors group">
            전체보기 <ExternalLink className="w-3.5 h-3.5 ml-1.5 group-hover:translate-x-0.5 transition-transform" />
          </Button>
        </Link>
      </div>

      <div className="flex-1 relative border border-gray-100 dark:border-gray-800 rounded-3xl overflow-hidden bg-gray-50/30 dark:bg-gray-950/20">
        <div className="grid grid-cols-[30px_1fr] h-full text-[10px] font-bold">
          {/* 타임 슬롯 (측면) */}
          <div className="flex flex-col border-r border-gray-100 dark:border-gray-800 bg-white dark:bg-gray-900 shrink-0">
            <div className="h-9 border-b border-gray-100 dark:border-gray-800" />
            {hours.map((hour) => (
              <div key={hour} className="flex-1 border-b border-gray-50 dark:border-gray-800/50 flex items-center justify-center text-gray-400">
                {hour}
              </div>
            ))}
          </div>

          {/* 메인 시간표 그리드 영역 */}
          <div 
            className="grid grid-cols-6 w-full bg-white/50 dark:bg-gray-900/50"
            style={{ aspectRatio: `8 / ${range}` }}
          >
            {days.map((day) => {
              // 요일 매핑 헬퍼 (API 응답의 다양한 형식을 현재 열의 요일명과 비교)
              const isMatch = (dayOfWeek: string) => {
                const dayMap: Record<string, string> = {
                  MONDAY: "월", TUESDAY: "화", WEDNESDAY: "수", THURSDAY: "목", FRIDAY: "금", SATURDAY: "토", SUNDAY: "일",
                  MO: "월", TU: "화", WE: "수", TH: "목", FR: "금", SA: "토", SU: "일",
                  월요일: "월", 화요일: "화", 수요일: "수", 목요일: "목", 금요일: "금", 토요일: "토", 일요일: "일"
                };
                return (dayMap[dayOfWeek] || dayOfWeek).startsWith(day);
              };

              return (
                <div key={day} className="flex flex-col border-r last:border-r-0 border-gray-100 dark:border-gray-800">
                  <div className="h-9 border-b border-gray-100 dark:border-gray-800 flex items-center justify-center bg-white dark:bg-gray-900 text-gray-500 uppercase">
                    {day}
                  </div>
                  <div className="flex-1 relative">
                    {/* 통합 일정 목록 생성 및 그룹화 (가로 분할 배치) */}
                    {(() => {
                      const daySchedules: any[] = [];

                      timetable?.courses?.forEach((course: any) => {
                        course.schedules?.filter((s: any) => isMatch(s.dayOfWeek)).forEach((s: any) => {
                          daySchedules.push({ 
                            id: `${course.courseKey}-${s.startTime}`, 
                            title: course.name,
                            professor: course.professor,
                            classroom: course.classroom,
                            startMin: toMin(s.startTime), 
                            endMin: toMin(s.endTime), 
                            type: 'course' 
                          });
                        });
                      });

                      timetable?.customSchedules?.filter((s: any) => isMatch(s.dayOfWeek)).forEach((s: any) => {
                        daySchedules.push({ id: `custom-${s.id}`, title: s.title, startMin: toMin(s.startTime), endMin: toMin(s.endTime), color: s.color, type: 'custom' });
                      });

                      // 시간순 정렬 및 중복 그룹화
                      const groups: any[][] = [];
                      daySchedules.sort((a,b) => a.startMin - b.startMin).forEach(sched => {
                        let placed = false;
                        for (const group of groups) {
                          const isOverlapping = group.some(item => 
                            Math.max(sched.startMin, item.startMin) < Math.min(sched.endMin, item.endMin)
                          );
                          if (isOverlapping) {
                            group.push(sched);
                            placed = true;
                            break;
                          }
                        }
                        if (!placed) groups.push([sched]);
                      });

                      return groups.map((group) => {
                        const count = group.length;
                        return group.map((item, idx) => {
                          const totalGridMinutes = range * 60;
                          const topOffset = (item.startMin - (startHour * 60)) / totalGridMinutes;
                          const duration = (item.endMin - item.startMin) / totalGridMinutes;

                          // 가로 등분 배치 계산
                          const widthFraction = 1 / count;
                          const leftOffset = widthFraction * idx;

                          return (
                            <motion.div
                              key={item.id}
                              initial={{ opacity: 0, scale: 0.95 }}
                              animate={{ opacity: 1, scale: 1 }}
                              whileHover={{ scale: 1.02, zIndex: 50 }}
                              className={cn(
                                "absolute rounded-md border p-1.5 overflow-hidden shadow-sm transition-all flex flex-col gap-0.5",
                                item.type === 'course' 
                                  ? "bg-primary/10 border-primary/20 text-primary" 
                                  : "bg-white dark:bg-gray-800 border-gray-200"
                              )}
                              style={{
                                top: `${topOffset * 100}%`,
                                height: `${duration * 100}%`,
                                width: `calc(${widthFraction * 100}% - 2px)`,
                                left: `calc(${leftOffset * 100}% + 1px)`,
                                backgroundColor: item.color ? `${item.color}20` : undefined,
                                borderColor: item.color ? `${item.color}40` : undefined,
                                color: item.color || undefined
                              }}
                            >
                              <p className="font-black text-[10px] leading-tight break-keep line-clamp-2">
                                {item.title}
                              </p>
                              {item.type === 'course' && (
                                <div className="flex flex-col gap-0.5 mt-0.5">
                                  {item.professor && (
                                    <p className="text-[8px] font-medium opacity-80 truncate flex items-center gap-1">
                                      {item.professor}
                                    </p>
                                  )}
                                  {item.classroom && (
                                    <p className="text-[8px] opacity-70 truncate font-medium">
                                      {item.classroom}
                                    </p>
                                  )}
                                </div>
                              )}
                            </motion.div>
                          );
                        });
                      });
                    })()}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
