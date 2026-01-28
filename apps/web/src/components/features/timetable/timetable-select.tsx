"use client";

import React, { useState } from 'react';
import { Plus, Trash2, Check, MoreVertical } from 'lucide-react';
import { 
  TimetableListResponse, 
  TimetableResponse 
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

  return (
    <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
      {timetables.map((t) => (
        <div key={t.id} className="relative group flex-shrink-0">
          <Button
            variant={t.id === currentId ? "default" : "outline"}
            size="sm"
            onClick={() => onSelect(t.id)}
            className={cn(
              "pr-8 transition-all relative",
              t.id === currentId ? "bg-primary text-primary-foreground" : "hover:bg-accent"
            )}
          >
            {t.isPrimary && <Check className="w-3 h-3 mr-1" />}
            {t.name}
          </Button>
          
          <div className="absolute right-1 top-1/2 -translate-y-1/2 z-10">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className={cn(
                    "w-6 h-6 p-0",
                    t.id === currentId ? "text-primary-foreground hover:bg-primary-foreground/20" : "text-muted-foreground hover:bg-accent"
                  )}
                >
                  <MoreVertical className="w-3 h-3" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => onSetPrimary(t.id)} disabled={t.isPrimary}>
                  대표 시간표로 설정
                </DropdownMenuItem>
                <DropdownMenuItem 
                  className="text-destructive focus:text-destructive" 
                  onClick={() => onDelete(t.id)}
                  disabled={timetables.length <= 1}
                >
                  <Trash2 className="w-4 h-4 mr-2" />
                  삭제
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      ))}

      <Button
        variant="outline"
        size="sm"
        onClick={() => setIsDialogOpen(true)}
        disabled={timetables.length >= 10 || isLoading}
        className="dashed shrink-0"
      >
        <Plus className="w-4 h-4 mr-1" />
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
