"use client";

import { useRef, useState } from "react";

import emojiData from "@emoji-mart/data";
import koI18n from "@emoji-mart/data/i18n/ko.json";
import Picker from "@emoji-mart/react";
import { AlertCircle, Loader2, Smile, Plus } from "lucide-react";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useToggleCourseEmoji } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/shared/ui/dialog";

interface CourseReviewSectionProps {
  courseKey: string;
}

const emojiPickerI18n = {
  ...koI18n,
  search: "모든 이모티콘 검색",
  categories: {
    ...koI18n.categories,
    frequent: "자주 사용됨",
  },
};

/**
 * 강의 상세 화면의 이모지 리뷰 섹션입니다.
 * 시스템 기본 이모지만 남길 수 있도록 제한합니다.
 */
export function CourseReviewSection({ courseKey }: CourseReviewSectionProps) {
  const { data: emojiStats, status: emojiStatus } = useCourseEmojis(courseKey);
  const { mutate: toggleEmoji, isPending: isEmojiToggling } = useToggleCourseEmoji(courseKey);
  const { data: user, isPending: isUserLoading } = useUser();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false);
  const pendingEmojiRef = useRef<string | null>(null);

  if (!courseKey) {
    return null;
  }

  const visibleEmojiStats = (emojiStats ?? []).filter((item) => item.count > 0);
  const isEmojiStatsReady = emojiStatus === "success";

  const handleEmojiToggle = (emoji: string) => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }

    if (pendingEmojiRef.current !== null) {
      return;
    }

    pendingEmojiRef.current = emoji;
    toggleEmoji(emoji, {
      onSettled: () => {
        if (pendingEmojiRef.current === emoji) {
          pendingEmojiRef.current = null;
        }
      },
    });
  };

  const handleEmojiSelect = (emoji: { native?: string }) => {
    if (!emoji.native) {
      return;
    }

    handleEmojiToggle(emoji.native);
    setIsEmojiPickerOpen(false);
  };

  const handleEmojiPickerOpenChange = (open: boolean) => {
    if (open && !user) {
      setLoginModalOpen(true);
      return;
    }

    setIsEmojiPickerOpen(open);
  };

  return (
    <section className="mt-8 flex flex-col gap-4">
      <div className="flex items-center gap-2">
        <Smile className="h-5 w-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">이모지 리뷰</h2>
      </div>

      <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-[#1E1E1E]">
        <div className="flex flex-col gap-3">
          <div className="flex items-center gap-2">
            <Smile className="h-4 w-4 text-primary" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">이모지 반응</h3>
          </div>
          <p className="text-xs text-gray-500 dark:text-gray-400">시스템 기본 이모지만 사용할 수 있습니다.</p>

          <div className="flex flex-wrap items-center gap-2">
            {isEmojiStatsReady && visibleEmojiStats.length > 0 ? (
              visibleEmojiStats.map((item) => (
                <Button
                  key={item.emoji}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => handleEmojiToggle(item.emoji)}
                  disabled={isEmojiToggling || isUserLoading || !isEmojiStatsReady}
                  aria-pressed={item.isMine}
                  aria-label={`${item.emoji} ${item.count}개`}
                  className={cn(
                    "h-9 rounded-full border px-3 py-1 text-sm font-medium transition-all hover:scale-[1.01]",
                    item.isMine
                      ? "border-primary/40 bg-primary/20 text-primary dark:border-primary/50 dark:bg-primary/25 dark:text-primary-light shadow-sm"
                      : "border-gray-200 bg-gray-50 text-gray-700 dark:border-gray-700 dark:bg-gray-900/40 dark:text-gray-300"
                  )}
                >
                  <span>{item.emoji}</span>
                  <span className="tabular-nums">{item.count}</span>
                </Button>
              ))
            ) : isEmojiStatsReady ? (
              <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                아직 등록된 이모지가 없습니다. 첫 반응을 남겨주세요.
              </div>
            ) : null}

            <Button
              type="button"
              variant="outline"
              size="icon-sm"
              onClick={() => handleEmojiPickerOpenChange(true)}
              disabled={isEmojiToggling || isUserLoading || !isEmojiStatsReady}
              aria-label="이모지 추가"
              className="h-9 w-9 rounded-full border-dashed border-primary/30 bg-primary/5 text-primary hover:bg-primary/10 dark:border-primary/40 dark:bg-primary/10"
            >
              <Plus className="h-4 w-4" />
            </Button>
          </div>

          <Dialog open={isEmojiPickerOpen} onOpenChange={handleEmojiPickerOpenChange}>
            <DialogContent
              showCloseButton={false}
              className="overflow-hidden border-slate-700 bg-[#1f2023] p-0 text-white sm:max-w-[42rem]"
            >
              <DialogTitle className="sr-only">이모지 선택</DialogTitle>
              <DialogDescription className="sr-only">
                반응을 남길 시스템 기본 이모지를 검색하거나 골라주세요.
              </DialogDescription>

              <div className="p-3">
                <Picker
                  data={emojiData}
                  i18n={emojiPickerI18n}
                  theme="dark"
                  onEmojiSelect={handleEmojiSelect}
                  searchPosition="top"
                  navPosition="top"
                  previewPosition="none"
                  maxFrequentRows={2}
                  perLine={8}
                  emojiSize={24}
                  style={{ width: "100%" }}
                />
              </div>
            </DialogContent>
          </Dialog>

          {emojiStatus === "pending" ? (
            <div className="flex items-center justify-center gap-2 py-6 text-sm text-gray-500 dark:text-gray-400">
              <Loader2 className="h-4 w-4 animate-spin" />
              이모지 통계를 불러오는 중입니다.
            </div>
          ) : emojiStatus === "error" ? (
            <div className="flex items-center gap-2 rounded-2xl border border-dashed border-red-200 bg-red-50/60 px-4 py-3 text-sm text-red-600 dark:border-red-900/40 dark:bg-red-900/10 dark:text-red-300">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>이모지 통계를 불러오지 못했습니다.</span>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}
