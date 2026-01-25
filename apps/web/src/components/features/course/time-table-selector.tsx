"use client";

import { cn } from "@/lib/utils";
import type { CourseDayOfWeek, ClassPeriod, ScheduleCondition } from "@/types/api";
import { useCallback, useState, useEffect } from "react";

const DAYS: CourseDayOfWeek[] = ['월', '화', '수', '목', '금', '토'];
const PERIOD_NAMES = Array.from({ length: 16 }, (_, i) => i);
const START_TIMES = [
  "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", 
  "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", 
  "20:00", "21:00", "22:00", "23:00"
];

interface TimeTableSelectorProps {
  selected: ScheduleCondition[];
  onChange: (selected: ScheduleCondition[]) => void;
}

export function TimeTableSelector({ selected, onChange }: TimeTableSelectorProps) {
  // ... (previous state and handlers remain the same)
  const [isDragging, setIsDragging] = useState(false);
  const [dragMode, setDragMode] = useState<'select' | 'deselect' | null>(null);
  const [hoveredDay, setHoveredDay] = useState<CourseDayOfWeek | null>(null);
  const [hoveredPeriod, setHoveredPeriod] = useState<number | null>(null);

  const isSelected = useCallback((day: CourseDayOfWeek, period: ClassPeriod) => {
    return selected.some(s => s.dayOfWeek === day && s.period === period);
  }, [selected]);

  const handleSlotAction = useCallback((day: CourseDayOfWeek, period: ClassPeriod, mode: 'select' | 'deselect') => {
    const alreadySelected = isSelected(day, period);
    if (mode === 'select' && !alreadySelected) {
      onChange([...selected, { dayOfWeek: day, period }]);
    } else if (mode === 'deselect' && alreadySelected) {
      onChange(selected.filter(s => !(s.dayOfWeek === day && s.period === period)));
    }
  }, [selected, onChange, isSelected]);

  const onMouseDown = (day: CourseDayOfWeek, period: ClassPeriod) => {
    const currentlySelected = isSelected(day, period);
    const mode = currentlySelected ? 'deselect' : 'select';
    setIsDragging(true);
    setDragMode(mode);
    handleSlotAction(day, period, mode);
  };

  const onMouseEnter = (day: CourseDayOfWeek, period: ClassPeriod, n: number) => {
    setHoveredDay(day);
    setHoveredPeriod(n);
    if (isDragging && dragMode) {
      handleSlotAction(day, period, dragMode);
    }
  };

  useEffect(() => {
    const handleMouseUp = () => {
      setIsDragging(false);
      setDragMode(null);
    };
    window.addEventListener('mouseup', handleMouseUp);
    return () => window.removeEventListener('mouseup', handleMouseUp);
  }, []);

  const handleSelectAll = (day: CourseDayOfWeek) => {
    const allSlotsForDay: ScheduleCondition[] = [];
    PERIOD_NAMES.forEach(n => {
      allSlotsForDay.push({ dayOfWeek: day, period: `${n}-A` as ClassPeriod });
      allSlotsForDay.push({ dayOfWeek: day, period: `${n}-B` as ClassPeriod });
    });

    const otherSchedules = selected.filter(s => s.dayOfWeek !== day);
    const isDayFullySelected = allSlotsForDay.every(slot => 
      selected.some(s => s.dayOfWeek === slot.dayOfWeek && s.period === slot.period)
    );

    if (isDayFullySelected) {
      onChange(otherSchedules);
    } else {
      onChange([...otherSchedules, ...allSlotsForDay]);
    }
  };

  return (
    <div className="w-fit overflow-hidden rounded-xl border border-white/10 bg-card/20 backdrop-blur-md p-3 select-none" onMouseLeave={() => { setHoveredDay(null); setHoveredPeriod(null); }}>
      <div className="w-full">
        <div className="grid grid-cols-[60px_repeat(6,36px)] mb-1 gap-1">
          <div className="text-[9px] font-black uppercase text-muted-foreground/30 flex items-center justify-center">시간</div>
          {DAYS.map(day => (
            <button
              key={day}
              onClick={() => handleSelectAll(day)}
              onMouseEnter={() => setHoveredDay(day)}
              className={cn(
                "text-[11px] font-bold text-center py-1 rounded transition-all group cursor-pointer border-t-2 border-transparent",
                hoveredDay === day ? "bg-primary/10 border-primary text-primary" : "text-muted-foreground/60 hover:text-primary"
              )}
            >
              <span>{day}</span>
            </button>
          ))}
        </div>

        <div className="space-y-1">
          {PERIOD_NAMES.map((n, idx) => (
            <div 
              key={idx} 
              className={cn(
                "grid grid-cols-[60px_repeat(6,36px)] gap-1 rounded-lg transition-colors",
                idx % 2 === 1 ? "bg-white/[0.02]" : "",
                hoveredPeriod === n ? "bg-primary/[0.05]" : ""
              )}
            >
              <div className={cn(
                "flex flex-col justify-center items-center h-10 border-r border-white/5 pr-2 transition-colors",
                hoveredPeriod === n ? "text-primary bg-primary/10 rounded-l-lg" : "text-muted-foreground/40"
              )}>
                <span className={cn("text-[10px] font-black leading-none mb-0.5", hoveredPeriod === n ? "text-primary" : "text-primary/60")}>{START_TIMES[idx]}</span>
                <span className="text-[8px] font-bold">{n}교시</span>
              </div>
              {DAYS.map(day => (
                <div 
                  key={day} 
                  className={cn(
                    "flex flex-col gap-1 h-10 p-1 rounded-md transition-colors border-r border-white/5 last:border-r-0",
                    hoveredDay === day ? "bg-primary/[0.05] border-x border-primary/[0.08]" : ""
                  )}
                  onMouseEnter={() => { setHoveredDay(day); setHoveredPeriod(n); }}
                >
                  <div
                    onMouseDown={() => onMouseDown(day, `${n}-A` as ClassPeriod)}
                    onMouseEnter={() => onMouseEnter(day, `${n}-A` as ClassPeriod, n)}
                    className={cn(
                      "flex-1 rounded-sm text-[8px] font-black transition-all duration-150 border border-white/5 flex items-center justify-center cursor-pointer uppercase",
                      isSelected(day, `${n}-A` as ClassPeriod)
                        ? "bg-primary text-primary-foreground shadow-[0_0_8px_rgba(var(--primary),0.3)] border-primary/50"
                        : "bg-background/10 text-transparent hover:bg-primary/20 hover:text-primary/40"
                    )}
                  >
                    a
                  </div>
                  <div
                    onMouseDown={() => onMouseDown(day, `${n}-B` as ClassPeriod)}
                    onMouseEnter={() => onMouseEnter(day, `${n}-B` as ClassPeriod, n)}
                    className={cn(
                      "flex-1 rounded-sm text-[8px] font-black transition-all duration-150 border border-white/5 flex items-center justify-center cursor-pointer uppercase",
                      isSelected(day, `${n}-B` as ClassPeriod)
                        ? "bg-primary text-primary-foreground shadow-[0_0_8px_rgba(var(--primary),0.3)] border-primary/50"
                        : "bg-background/10 text-transparent hover:bg-primary/20 hover:text-primary/40"
                    )}
                  >
                    b
                  </div>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
      <div className="mt-4 flex items-center justify-between px-1 border-t border-white/5 pt-3">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <div className="w-1.5 h-1.5 rounded-full bg-primary shadow-[0_0_5px_rgba(var(--primary),0.5)]" />
            <span className="text-[9px] font-bold text-muted-foreground/60 uppercase tracking-tighter">Selected</span>
          </div>
          <p className="text-[9px] text-muted-foreground/40 italic">
            * drag to select
          </p>
        </div>
        <button 
          onClick={() => onChange([])}
          className="text-[9px] font-black uppercase tracking-widest text-primary/60 hover:text-primary hover:underline transition-all active:scale-95"
        >
          Reset
        </button>
      </div>
    </div>
  );
}
