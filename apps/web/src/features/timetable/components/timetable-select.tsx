"use client";

import { useState } from 'react';
import { Plus, Trash2, MoreVertical, Crown } from 'lucide-react';
import type { TimetableListResponse } from '@/shared/types/api';
import { Button } from '@/shared/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/shared/ui/dialog";
import { Input } from '@/shared/ui/input';
import { cn } from '@/shared/lib/utils';

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
    <div className="-m-0.5 flex items-center gap-2 overflow-x-auto p-0.5 scrollbar-none">
      {sortedTimetables.map((t) => (
        <div 
          key={t.id} 
          className={cn(
            "flex shrink-0 items-center overflow-hidden rounded-md border transition-colors",
            t.id === currentId 
              ? "border-primary bg-primary text-primary-foreground"
              : "border-border bg-background text-foreground hover:border-primary/30"
          )}
        >
          <button
            type="button"
            aria-pressed={t.id === currentId}
            onClick={() => onSelect(t.id)}
            className="flex h-8 min-w-16 max-w-32 items-center gap-1.5 px-2.5 text-sm font-medium focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-current sm:max-w-44"
          >
            {t.primary && (
              <Crown className="size-4 shrink-0 text-current" />
            )}
            <span className="truncate">{t.name}</span>
          </button>
          
          <div className="h-4 w-px bg-current opacity-15" />

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button
                type="button"
                aria-label={`${t.name} 설정`}
                className={cn(
                  "grid size-8 place-items-center transition-colors focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-current",
                  t.id === currentId
                    ? "hover:bg-primary-foreground/10"
                    : "text-muted-foreground hover:bg-accent hover:text-foreground"
                )}
              >
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
        className="h-[34px] border-dashed"
      >
        <Plus className="size-4" />
        새 시간표
      </Button>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>새 시간표 생성</DialogTitle>
            <DialogDescription className="sr-only">
              새로운 시간표 이름을 입력하는 대화상자입니다.
            </DialogDescription>
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
