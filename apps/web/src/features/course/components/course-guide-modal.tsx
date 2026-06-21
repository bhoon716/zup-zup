"use client";

import { useCallback, useEffect, useState } from "react";
import { BookOpen } from "lucide-react";

import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";

export const COURSE_GUIDE_MODAL_STORAGE_KEY = "course-guide-modal-seen-v1";

const guideSteps = [
  {
    title: "강의명이나 교수명을 먼저 입력하세요.",
    description: "검색창에 원하는 키워드를 넣고 바로 결과를 확인합니다.",
  },
  {
    title: "필요한 필터만 펼쳐서 좁히세요.",
    description: "기본 정보, 강의 상세, 스마트 필터를 필요한 만큼만 사용합니다.",
  },
  {
    title: "강의 카드를 눌러 상세를 확인하세요.",
    description: "강의 상세에서 찜, 알림, 시간표 추가 같은 후속 동작을 이어갈 수 있습니다.",
  },
];

export function CourseGuideModal() {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    try {
      if (window.localStorage.getItem(COURSE_GUIDE_MODAL_STORAGE_KEY) !== "seen") {
        const timeoutId = window.setTimeout(() => {
          setOpen(true);
        }, 0);

        return () => window.clearTimeout(timeoutId);
      }
    } catch {
      const timeoutId = window.setTimeout(() => {
        setOpen(true);
      }, 0);

      return () => window.clearTimeout(timeoutId);
    }
  }, []);

  const handleOpenChange = useCallback((nextOpen: boolean) => {
    setOpen(nextOpen);

    if (nextOpen) {
      return;
    }

    try {
      window.localStorage.setItem(COURSE_GUIDE_MODAL_STORAGE_KEY, "seen");
    } catch {
      // 저장소를 사용할 수 없어도 검색 흐름은 계속 진행한다.
    }
  }, []);

  return (
    <>
      <Button
        type="button"
        variant="outline"
        size="sm"
        onClick={() => setOpen(true)}
        className="h-9 rounded-lg border-border/60 bg-white/70 text-xs font-bold text-foreground/70 hover:bg-white hover:text-foreground"
      >
        <BookOpen className="h-4 w-4" />
        사용법
      </Button>

      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent
          showCloseButton={false}
          className="sm:max-w-[540px] rounded-[2rem] border border-border/60 bg-white/95 p-0 shadow-2xl backdrop-blur-xl"
        >
          <div className="space-y-6 px-6 py-6 sm:px-8 sm:py-8">
            <DialogHeader className="space-y-3 text-left">
              <div className="inline-flex w-fit items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-bold text-primary">
                <BookOpen className="h-3.5 w-3.5" />
                처음 사용하는 분
              </div>
              <DialogTitle className="text-2xl font-black tracking-tight text-foreground">
                강의 검색 사용법
              </DialogTitle>
              <DialogDescription className="text-sm leading-relaxed text-muted-foreground">
                핵심만 보고 바로 검색할 수 있게, 처음 진입할 때만 짧게 안내합니다.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-3">
              {guideSteps.map((step, index) => (
                <div
                  key={step.title}
                  className="flex gap-3 rounded-2xl border border-border/60 bg-muted/20 px-4 py-3"
                >
                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-black text-primary">
                    {index + 1}
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm font-bold text-foreground">{step.title}</p>
                    <p className="text-xs leading-relaxed text-muted-foreground">{step.description}</p>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex flex-col gap-3 border-t border-border/60 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <p className="text-xs leading-relaxed text-muted-foreground">
                닫아도 검색은 계속할 수 있고, 오른쪽 버튼으로 다시 열 수 있습니다.
              </p>
              <Button type="button" onClick={() => handleOpenChange(false)} className="rounded-full px-5 font-bold">
                이해했어요
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
