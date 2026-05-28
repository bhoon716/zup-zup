"use client";

import { useState } from "react";

import emojiData from "@emoji-mart/data";
import koI18n from "@emoji-mart/data/i18n/ko.json";
import Picker from "@emoji-mart/react";
import axios from "axios";
import { AlertCircle, Loader2, MessageSquare, Plus, Smile, Star, ThumbsDown, ThumbsUp } from "lucide-react";
import { toast } from "sonner";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useCreateReview, useReviews, useToggleCourseEmoji, useToggleReviewReaction } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { formatRelativeTime } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogTitle } from "@/shared/ui/dialog";

interface CourseReviewSectionProps {
  courseKey: string;
  isReviewed?: boolean;
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
 * 별점 리뷰와 이모지 반응을 함께 제공합니다.
 */
export function CourseReviewSection({ courseKey, isReviewed }: CourseReviewSectionProps) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useReviews(courseKey);
  const { mutate: createReview, isPending: isCreating } = useCreateReview(courseKey);
  const { mutate: toggleReaction } = useToggleReviewReaction(courseKey);
  const { data: emojiStats, status: emojiStatus } = useCourseEmojis(courseKey);
  const { mutate: toggleEmoji, isPending: isEmojiToggling } = useToggleCourseEmoji(courseKey);
  const { data: user, isPending: isUserLoading } = useUser();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const [rating, setRating] = useState<number>(0);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [isEmojiPickerOpen, setIsEmojiPickerOpen] = useState(false);

  if (!courseKey) {
    return null;
  }

  const reviews = data?.pages.flatMap((page) => page.content) || [];
  const visibleEmojiStats = (emojiStats ?? []).filter((item) => item.count > 0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (rating === 0) {
      toast.error("별점을 선택해주세요.");
      return;
    }

    createReview(
      { rating },
      {
        onSuccess: () => {
          setRating(0);
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

  const handleReactionClick = (reviewId: number, reactionType: "LIKE" | "DISLIKE") => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }

    toggleReaction({ reviewId, request: { reactionType } });
  };

  const handleEmojiToggle = (emoji: string) => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }

    toggleEmoji(emoji);
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
        <div className="flex flex-col gap-4 border-b border-gray-100 pb-5 dark:border-gray-800">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">별점 리뷰</p>
          </div>

          {user ? (
            !isReviewed ? (
              <form onSubmit={handleSubmit} className="flex flex-wrap items-center gap-2">
                <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">별점</span>
                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      aria-label={`${star}점`}
                      className="focus:outline-hidden"
                    >
                      <Star
                        className={cn(
                          "h-6 w-6 transition-colors",
                          star <= (hoverRating || rating)
                            ? "fill-yellow-400 text-yellow-400"
                            : "text-gray-300 dark:text-gray-600"
                        )}
                      />
                    </button>
                  ))}
                </div>
                {rating > 0 && <span className="text-sm font-bold text-primary">{rating}점</span>}
                <Button type="submit" disabled={isCreating} className="ml-auto font-bold">
                  {isCreating ? "등록 중..." : "등록"}
                </Button>
              </form>
            ) : (
              <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-600 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                이미 별점 리뷰를 남겼습니다.
              </div>
            )
          ) : (
            <div className="rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-600 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
              리뷰를 작성하려면 로그인이 필요합니다.
            </div>
          )}
        </div>

        <div className="mt-5 flex flex-col gap-4">
          <div className="flex items-center gap-2">
            <Smile className="h-4 w-4 text-primary" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">이모지 리뷰</h3>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {visibleEmojiStats.map((item) => (
              <span
                key={item.emoji}
                className={cn(
                  "inline-flex items-center gap-1 rounded-full border px-3 py-1 text-sm font-medium",
                  item.isMine
                    ? "border-primary/30 bg-primary/10 text-primary dark:border-primary/40 dark:bg-primary/15 dark:text-primary-light"
                    : "border-gray-200 bg-gray-50 text-gray-700 dark:border-gray-700 dark:bg-gray-900/40 dark:text-gray-300"
                )}
              >
                <span>{item.emoji}</span>
                <span className="tabular-nums">{item.count}</span>
                {item.isMine && <span className="text-[10px] font-bold uppercase tracking-wide">내 반응</span>}
              </span>
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

      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-2">
          <MessageSquare className="h-5 w-5 text-primary" />
          <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">별점 리뷰 목록</h3>
        </div>

        {status === "pending" ? (
          <div className="py-4 text-center text-sm text-gray-500">리뷰를 불러오는 중입니다...</div>
        ) : reviews.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-gray-200 bg-gray-50 px-4 py-8 text-center text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
            아직 등록된 리뷰가 없습니다. 첫 번째 별점 리뷰를 남겨주세요!
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {reviews.map((review) => (
              <div
                key={review.id}
                className={cn(
                  "rounded-2xl border p-4 transition-colors",
                  review.isMine
                    ? "border-primary/20 bg-primary/5 dark:border-primary/30 dark:bg-primary/10"
                    : "border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-900/40"
                )}
              >
                <div className="mb-2 flex items-start justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <div className="flex items-center gap-1">
                      {[1, 2, 3, 4, 5].map((star) => (
                        <Star
                          key={star}
                          className={cn(
                            "h-4 w-4",
                            star <= review.rating
                              ? "fill-yellow-400 text-yellow-400"
                              : "text-gray-300 dark:text-gray-600"
                          )}
                        />
                      ))}
                    </div>
                    <span className="text-sm font-bold text-primary">{review.rating}점</span>
                  </div>
                  {review.isMine && (
                    <span className="rounded-full bg-primary/10 px-2 py-1 text-[11px] font-bold text-primary dark:bg-primary/15 dark:text-primary-light">
                      내가 남긴 리뷰
                    </span>
                  )}
                </div>

                <div className="mb-3 flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
                  <span>{formatRelativeTime(review.createdAt)}</span>
                  <span className="text-[11px]">{review.likeCount + review.dislikeCount}명 반응</span>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleReactionClick(review.id, "LIKE")}
                    className={cn(
                      "rounded-full",
                      review.isMine &&
                        "border-primary/30 bg-primary/10 text-primary dark:border-primary/40 dark:bg-primary/15 dark:text-primary-light"
                    )}
                  >
                    <ThumbsUp className="h-4 w-4" />
                    <span>{review.likeCount}</span>
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => handleReactionClick(review.id, "DISLIKE")}
                    className="rounded-full"
                  >
                    <ThumbsDown className="h-4 w-4" />
                    <span>{review.dislikeCount}</span>
                  </Button>
                </div>
              </div>
            ))}

            {hasNextPage && (
              <div className="pt-2 text-center">
                <Button
                  variant="outline"
                  onClick={() => fetchNextPage()}
                  disabled={isFetchingNextPage}
                  className="rounded-full"
                >
                  {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
