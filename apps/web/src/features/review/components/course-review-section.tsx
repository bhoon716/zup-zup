"use client";

import { useRef, useState } from "react";

import emojiData from "@emoji-mart/data";
import koI18n from "@emoji-mart/data/i18n/ko.json";
import Picker from "@emoji-mart/react";
import axios from "axios";
import { AlertCircle, Loader2, MessageSquare, Plus, Star } from "lucide-react";
import { toast } from "sonner";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useCreateReview, useReviews, useToggleCourseEmoji, useUpdateReview } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/shared/ui/dialog";

interface CourseReviewSectionProps {
  courseKey: string;
  reviewScopeKey: string;
  averageRating?: number;
  reviewCount?: number;
  isReviewed?: boolean;
  isProfessorUnassigned?: boolean;
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
 * 전체 평균 별점, 별점 입력/수정, 이모지 반응만 제공합니다.
 */
export function CourseReviewSection({
  courseKey,
  reviewScopeKey,
  averageRating,
  reviewCount,
  isReviewed,
  isProfessorUnassigned = false,
}: CourseReviewSectionProps) {
  const { data: myReview, status: reviewStatus } = useReviews(reviewScopeKey, courseKey);
  const { mutate: createReview, isPending: isCreating } = useCreateReview(reviewScopeKey, courseKey);
  const { mutate: updateReview, isPending: isUpdating } = useUpdateReview(reviewScopeKey, courseKey);
  const { data: emojiStats, status: emojiStatus } = useCourseEmojis(reviewScopeKey, courseKey);
  const { mutate: toggleEmoji, isPending: isEmojiToggling } = useToggleCourseEmoji(reviewScopeKey, courseKey);
  const { data: user, isPending: isUserLoading } = useUser({ skipAuthRefresh: true });
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const [draftRating, setDraftRating] = useState<number | null>(null);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false);
  const pendingEmojiRef = useRef<string | null>(null);

  const visibleEmojiStats = (emojiStats ?? []).filter((item) => item.count > 0);
  const currentRating = draftRating ?? myReview?.rating ?? 0;
  const hasAverageRating = (reviewCount ?? 0) > 0;
  const averageRatingText = hasAverageRating ? (averageRating ?? 0).toFixed(1) : "0";
  const averageReviewCount = reviewCount ?? 0;
  const averageStars = Math.round(averageRating ?? 0);
  const isReviewReady = reviewStatus === "success";
  const isEditingReview = Boolean(myReview) || Boolean(isReviewed);
  const isSubmittingReview = isCreating || isUpdating;

  if (!courseKey || !reviewScopeKey) {
    return null;
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (currentRating === 0) {
      toast.error("별점을 선택해주세요.");
      return;
    }

    if (myReview) {
      updateReview(
        { reviewId: myReview.id, request: { rating: currentRating } },
        {
          onSuccess: () => {
            setDraftRating(null);
            toast.success("리뷰가 수정되었습니다.");
          },
          onError: (err: unknown) => {
            let errorMsg = "수정에 실패했습니다.";
            if (axios.isAxiosError(err)) {
              errorMsg = err.response?.data?.message || errorMsg;
            }
            toast.error(errorMsg);
          },
        }
      );
      return;
    }

    createReview(
      { rating: currentRating },
      {
        onSuccess: () => {
          setDraftRating(null);
          toast.success("리뷰가 등록되었습니다.");
        },
        onError: (err: unknown) => {
          let errorMsg = "작성에 실패했습니다. 이미 작성하셨을 수 있습니다.";
          if (axios.isAxiosError(err)) {
            errorMsg = err.response?.data?.message || errorMsg;
          }
          toast.error(errorMsg);
        },
      }
    );
  };

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
        <div className="grid gap-5 lg:grid-cols-[1.1fr_0.9fr]">
          <div className="rounded-2xl border border-gray-100 bg-gray-50/70 p-4 dark:border-gray-800 dark:bg-gray-900/30">
            <div className="flex items-end justify-between gap-3">
              <div>
                <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">전체 평균 별점</p>
              </div>
              <div className="flex items-center gap-1">
                {Array.from({ length: 5 }, (_, index) => {
                  const starNumber = index + 1;
                  return (
                    <Star
                      key={starNumber}
                      className={cn(
                        "h-5 w-5",
                        starNumber <= averageStars
                          ? "fill-yellow-400 text-yellow-400"
                          : "text-gray-300 dark:text-gray-600"
                      )}
                    />
                  );
                })}
              </div>
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-3xl font-black tracking-tight text-primary">{averageRatingText}점</span>
              <span className="text-sm text-gray-500 dark:text-gray-400">({averageReviewCount}개)</span>
            </div>
          </div>

          <div className="rounded-2xl border border-gray-100 bg-white p-4 dark:border-gray-800 dark:bg-[#151515]">
            <div className="flex flex-col gap-3">
              <div className="flex flex-col gap-1">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                  {isEditingReview ? "내 별점 수정" : "별점 리뷰 등록"}
                </h3>
                <p className="text-xs text-gray-500 dark:text-gray-400">별점만 남길 수 있습니다.</p>
              </div>

              {user ? (
                <form onSubmit={handleSubmit} className="flex flex-wrap items-center gap-2">
                  <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">별점</span>
                  <div className="flex items-center gap-1">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setDraftRating(star)}
                        onMouseEnter={() => setHoverRating(star)}
                        onMouseLeave={() => setHoverRating(0)}
                        aria-label={`${star}점`}
                        className="focus:outline-hidden"
                      >
                        <Star
                          className={cn(
                            "h-6 w-6 transition-colors",
                            star <= (hoverRating || currentRating)
                              ? "fill-yellow-400 text-yellow-400"
                              : "text-gray-300 dark:text-gray-600"
                          )}
                        />
                      </button>
                    ))}
                  </div>
                  {currentRating > 0 && <span className="text-sm font-bold text-primary">{currentRating}점</span>}
                  <Button type="submit" disabled={isSubmittingReview || !isReviewReady} className="ml-auto font-bold">
                    {isEditingReview ? "수정" : "등록"}
                  </Button>
                </form>
              ) : (
                <div className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-600 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                  리뷰를 작성하려면 로그인이 필요합니다.
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="mt-5 flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">이모지 리뷰</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">이모지만 남길 수 있습니다.</p>
            {isProfessorUnassigned && (
              <p className="mt-1 flex items-start gap-1.5 text-xs text-amber-700 dark:text-amber-300" role="status">
                <AlertCircle className="mt-0.5 h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                교수 정보가 없어 이전 학기 반응과 자동 연결하지 않았습니다.
              </p>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {visibleEmojiStats.map((item) => (
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
            ))}

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
