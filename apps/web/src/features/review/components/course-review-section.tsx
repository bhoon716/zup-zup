"use client";

import { useRef, useState } from "react";

import emojiData from "@emoji-mart/data";
import koI18n from "@emoji-mart/data/i18n/ko.json";
import Picker from "@emoji-mart/react";
import { AlertCircle, Loader2, MessageSquare, Plus, Star } from "lucide-react";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useToggleCourseEmoji } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/shared/ui/dialog";

interface CourseReviewSectionProps {
  courseKey: string;
  averageRating?: number;
  reviewCount?: number;
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
 * 강의 상세 화면의 리뷰 섹션입니다.
 * 전체 평균 별점과 이모지 반응만 제공합니다.
 */
export function CourseReviewSection({ courseKey, averageRating, reviewCount }: CourseReviewSectionProps) {
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
  const hasAverageRating = typeof averageRating === "number" && reviewCount !== undefined && reviewCount > 0;

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
    <section className="mt-8 flex flex-col gap-6">
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">강의 리뷰</h2>
      </div>

      <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-[#1E1E1E]">
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">전체 평균 별점</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">모든 별점 리뷰의 평균입니다.</p>
            </div>

            {hasAverageRating ? (
              <div className="flex items-center gap-1">
                {Array.from({ length: 5 }, (_, index) => {
                  const starNumber = index + 1;
                  return (
                    <Star
                      key={starNumber}
                      className={cn(
                        "h-5 w-5",
                        starNumber <= Math.round(averageRating)
                          ? "fill-yellow-400 text-yellow-400"
                          : "text-gray-300 dark:text-gray-600"
                      )}
                    />
                  );
                })}
                <span className="ml-2 text-lg font-bold text-primary">{averageRating.toFixed(1)}</span>
                <span className="text-sm text-gray-500 dark:text-gray-400">({reviewCount}개)</span>
              </div>
            ) : (
              <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                아직 등록된 별점 리뷰가 없습니다.
              </div>
            )}
          </div>

          <div className="flex flex-col gap-2 pt-1">
            <div className="flex flex-col gap-1">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">이모지 리뷰</h3>
              <p className="text-xs text-gray-500 dark:text-gray-400">이 강의에 어울리는 이모지를 골라주세요 😊</p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              {visibleEmojiStats.length > 0 ? (
                visibleEmojiStats.map((item) => (
                  <Button
                    key={item.emoji}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => handleEmojiToggle(item.emoji)}
                  disabled={isEmojiToggling || isUserLoading}
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
              ) : (
                <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                  아직 등록된 이모지가 없습니다. 첫 반응을 남겨주세요.
                </div>
              )}

              <Button
                type="button"
                variant="outline"
                size="icon-sm"
                onClick={() => handleEmojiPickerOpenChange(true)}
                disabled={isEmojiToggling || isUserLoading}
                aria-label="이모지 추가"
                className="h-9 w-9 rounded-full border-dashed border-primary/30 bg-primary/5 text-primary hover:bg-primary/10 dark:border-primary/40 dark:bg-primary/10"
              >
                <Plus className="h-4 w-4" />
              </Button>
            </div>
          </div>

          <Dialog open={isEmojiPickerOpen} onOpenChange={handleEmojiPickerOpenChange}>
            <DialogContent
              showCloseButton={false}
              className="overflow-hidden border-slate-700 bg-[#1f2023] p-0 text-white sm:max-w-[42rem]"
            >
              <DialogTitle className="sr-only">이모지 선택</DialogTitle>
              <DialogDescription className="sr-only">
                반응을 남길 이모지를 검색하거나 골라주세요.
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
