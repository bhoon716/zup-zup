"use client";

import { useState } from "react";

import axios from "axios";
import { AlertCircle, Loader2, MessageSquare, Plus, Smile, Star, ThumbsDown, ThumbsUp } from "lucide-react";
import { toast } from "sonner";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useCreateReview, useReviews, useToggleCourseEmoji, useToggleReviewReaction } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { formatRelativeTime } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/shared/ui/dialog";

interface CourseReviewSectionProps {
  courseKey: string;
  isReviewed?: boolean;
}

const PRESET_EMOJIS = [
  "👍",
  "🔥",
  "🎓",
  "📝",
  "😴",
  "🚨",
] as const;

const EXTRA_SYSTEM_EMOJIS = [
  "😂",
  "🥹",
  "😭",
  "😍",
  "🤔",
  "👏",
  "🙏",
  "💯",
  "👀",
  "✨",
  "😎",
  "😅",
  "🫡",
  "✅",
] as const;

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

  const handleEmojiPickerOpenChange = (open: boolean) => {
    if (open && !user) {
      setLoginModalOpen(true);
      return;
    }

    setIsEmojiPickerOpen(open);
  };

  const renderEmojiPickerButton = (emoji: string) => (
    <Button
      key={emoji}
      type="button"
      variant="ghost"
      size="icon-lg"
      onClick={() => {
        handleEmojiToggle(emoji);
        setIsEmojiPickerOpen(false);
      }}
      disabled={isEmojiToggling || isUserLoading}
      aria-label={emoji}
      className="h-11 w-11 rounded-xl text-xl transition-transform hover:scale-105"
    >
      <span>{emoji}</span>
    </Button>
  );

  return (
    <section className="flex flex-col gap-6 mt-8">
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">강의 리뷰</h2>
      </div>

      <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-[#1E1E1E]">
        <div className="flex flex-col gap-4 border-b border-gray-100 pb-5 dark:border-gray-800">
          <div className="flex flex-col gap-2">
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">별점 리뷰</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              코멘트 없이 별점만 남길 수 있습니다.
            </p>
          </div>

          {user ? (
            !isReviewed ? (
              <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                <div className="flex items-center gap-2 flex-wrap">
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
                </div>

                <div className="flex justify-end">
                  <Button type="submit" disabled={isCreating} className="font-bold">
                    {isCreating ? "등록 중..." : "등록"}
                  </Button>
                </div>
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
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">이모지 반응</h3>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {emojiStats?.length ? (
              emojiStats.map((item) => (
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
              ))
            ) : (
              <span className="rounded-full border border-dashed border-gray-200 bg-gray-50 px-3 py-1 text-sm text-gray-500 dark:border-gray-700 dark:bg-gray-900/30 dark:text-gray-400">
                아직 등록된 이모지가 없습니다.
              </span>
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

          <Dialog open={isEmojiPickerOpen} onOpenChange={handleEmojiPickerOpenChange}>
            <DialogContent className="sm:max-w-2xl">
              <DialogHeader>
                <DialogTitle>이모지 선택</DialogTitle>
                <DialogDescription>반응을 남길 이모지를 골라주세요. 선택하면 바로 반영됩니다.</DialogDescription>
              </DialogHeader>

              <div className="space-y-5">
                <div className="space-y-3">
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">고정 이모지</p>
                  <div className="grid grid-cols-6 gap-2 sm:grid-cols-8">
                    {PRESET_EMOJIS.map(renderEmojiPickerButton)}
                  </div>
                </div>

                <div className="space-y-3">
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">다른 시스템 이모지</p>
                  <div className="grid grid-cols-6 gap-2 sm:grid-cols-8">
                    {EXTRA_SYSTEM_EMOJIS.map(renderEmojiPickerButton)}
                  </div>
                </div>
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
          <div className="text-center text-sm text-gray-500 py-4">리뷰를 불러오는 중입니다...</div>
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
                  "rounded-xl border border-gray-100 bg-white p-4 shadow-xs transition-colors hover:border-gray-200 dark:border-gray-800 dark:bg-[#1E1E1E] dark:hover:border-gray-700",
                  review.isMine && "border-primary/30 bg-primary/5 dark:border-primary/30 dark:bg-primary/5"
                )}
              >
                <div className="mb-2 flex items-start justify-between gap-4">
                  <div className="flex items-center gap-2">
                    <div className="flex items-center rounded-full border border-yellow-200 bg-yellow-50 px-2 py-0.5 text-xs font-bold text-yellow-600 dark:border-yellow-500/20 dark:bg-yellow-500/10 dark:text-yellow-400">
                      <Star className="mr-1 h-3 w-3 fill-yellow-500 text-yellow-500" />
                      {review.rating}.0
                    </div>
                    {review.isMine && (
                      <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">내 리뷰</span>
                    )}
                  </div>
                  <span className="text-xs font-medium text-gray-400">{formatRelativeTime(review.createdAt)}</span>
                </div>

                <div className="mt-4 flex items-center gap-3">
                  <button
                    onClick={() => handleReactionClick(review.id, "LIKE")}
                    className="flex items-center gap-1.5 rounded-full border border-gray-200 bg-gray-50 px-3 py-1.5 text-xs font-semibold text-gray-600 transition-colors hover:bg-gray-100 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700"
                  >
                    <ThumbsUp className="h-3.5 w-3.5" />
                    <span>공감 {review.likeCount > 0 && review.likeCount}</span>
                  </button>
                  <button
                    onClick={() => handleReactionClick(review.id, "DISLIKE")}
                    className="flex items-center gap-1.5 rounded-full border border-gray-200 bg-gray-50 px-3 py-1.5 text-xs font-semibold text-gray-600 transition-colors hover:bg-gray-100 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:hover:bg-gray-700"
                  >
                    <ThumbsDown className="h-3.5 w-3.5" />
                    <span>비공감 {review.dislikeCount > 0 && review.dislikeCount}</span>
                  </button>
                </div>
              </div>
            ))}

            {hasNextPage && (
              <Button
                variant="outline"
                onClick={() => fetchNextPage()}
                disabled={isFetchingNextPage}
                className="mt-2 text-sm text-gray-600 dark:text-gray-400 border-dashed"
              >
                {isFetchingNextPage ? "불러오는 중..." : "더보기"}
              </Button>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
