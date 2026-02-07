"use client";

import React, { useState } from 'react';
import { Plus, Trash2, Check, MoreVertical, Crown } from 'lucide-react';
import { 
  TimetableListResponse
} from '@/types/api';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';

interface TimetableSelectProps {
  timetables: TimetableListResponse[];
  currentId: number;
  onSelect: (id: number) => void;
  onCreate: (name: string) => void;
  onDelete: (id: number) => void;
  onSetPrimary: (id: number) => void;
  isLoading?: boolean;
}

export function TimetableSelect({
  timetables,
  currentId,
  onSelect,
  onCreate,
  onDelete,
  onSetPrimary,
  isLoading
}: TimetableSelectProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newTimetableName, setNewTimetableName] = useState('');

  const handleCreate = () => {
    if (!newTimetableName.trim()) return;
    onCreate(newTimetableName);
    setNewTimetableName('');
    setIsDialogOpen(false);
  };

  const sortedTimetables = [...timetables].sort((a, b) => {
    if (a.primary) return -1;
    if (b.primary) return 1;
    return 0;
  });

  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
      {sortedTimetables.map((t) => (
        <div 
          key={t.id} 
          className={cn(
            "flex items-center gap-0 rounded-xl transition-all border shrink-0 overflow-hidden",
            t.id === currentId 
              ? "bg-primary text-primary-foreground border-primary shadow-md shadow-primary/10" 
              : "bg-background text-foreground border-border hover:border-primary/30 hover:bg-accent/5"
          )}
        >
          <button
            onClick={() => onSelect(t.id)}
            className="flex items-center gap-2 pl-3.5 pr-1.5 py-1.5 min-w-[80px] max-w-[180px] text-sm font-bold focus:outline-none transition-opacity active:opacity-80"
          >
            {t.primary && (
              <Crown className="w-4 h-4 text-yellow-400 fill-yellow-400 shrink-0 drop-shadow-[0_0_3px_rgba(250,204,21,0.4)]" />
            )}
            <span className="truncate">{t.name}</span>
          </button>
          
          <div className="h-4 w-[1px] bg-current opacity-10" />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className={cn(
                "p-2 pr-2.5 transition-colors focus:outline-none active:opacity-70",
                t.id === currentId 
                  ? "text-primary-foreground/60 hover:text-primary-foreground hover:bg-white/10" 
                  : "text-muted-foreground hover:text-foreground hover:bg-accent"
              )}>
                <MoreVertical className="w-3.5 h-3.5" />
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48 rounded-xl shadow-xl border-border/50">
              <DropdownMenuItem 
                onClick={() => onSetPrimary(t.id)} 
                disabled={t.primary}
                className="gap-2 py-2.5 cursor-pointer font-medium"
              >
                <Crown className="w-4 h-4 text-yellow-500" />
                대표 시간표로 설정
              </DropdownMenuItem>
              <DropdownMenuItem 
                onClick={() => onDelete(t.id)}
                disabled={timetables.length <= 1}
                className="gap-2 py-2.5 cursor-pointer font-medium text-destructive focus:text-destructive focus:bg-destructive/5"
              >
                <Trash2 className="w-4 h-4" />
                시간표 삭제
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      ))}

      <Button
        variant="outline"
        size="sm"
        onClick={() => setIsDialogOpen(true)}
        disabled={timetables.length >= 10 || isLoading}
        className="rounded-xl border-dashed shrink-0 h-[38px] px-4 font-bold hover:bg-accent/50 transition-colors"
      >
        <Plus className="w-4 h-4 mr-2" />
        새 시간표
      </Button>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 시간표 생성</DialogTitle>
          </DialogHeader>
          <div className="py-4">
            <Input
              placeholder="시간표 이름을 입력하세요 (예: 2024-1학기 기본)"
              value={newTimetableName}
              onChange={(e) => setNewTimetableName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsDialogOpen(false)}>취소</Button>
            <Button onClick={handleCreate} disabled={!newTimetableName.trim()}>생성</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
