"use client";

import { useState } from "react";
import { Star, ThumbsUp, ThumbsDown, MessageSquare } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { useReviews, useCreateReview, useToggleReviewReaction } from "@/features/review/hooks/useReviews";
import { formatRelativeTime } from "@/shared/lib/formatters";
import { cn } from "@/shared/lib/utils";
import { useUser } from "@/features/user/hooks/useUser";
import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { toast } from "sonner";

interface CourseReviewSectionProps {
  courseKey: string;
  isReviewed?: boolean;
}

export function CourseReviewSection({ courseKey, isReviewed }: CourseReviewSectionProps) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, status } = useReviews(courseKey);
  const { mutate: createReview, isPending: isCreating } = useCreateReview(courseKey);
  const { mutate: toggleReaction } = useToggleReviewReaction(courseKey);

  const { data: user } = useUser();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  const [rating, setRating] = useState<number>(0);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [content, setContent] = useState<string>("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0) {
      toast.error("별점을 선택해주세요.");
      return;
    }
    if (content.trim().length === 0) {
      toast.error("리뷰 내용을 입력해주세요.");
      return;
    }

    createReview(
      { rating, content },
      {
        onSuccess: () => {
          setRating(0);
          setContent("");
          toast.success("리뷰가 등록되었습니다.");
        },
        onError: (err: unknown) => {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const errorMsg = (err as any)?.response?.data?.message;
          toast.error(errorMsg || "작성에 실패했습니다. 이미 작성하셨을 수 있습니다.");
        }
      }
    );
  };

  const handleReactionClick = (reviewId: number, reactionType: 'LIKE' | 'DISLIKE') => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }
    toggleReaction({ reviewId, request: { reactionType } });
  };

  const reviews = data?.pages.flatMap((page) => page.content) || [];

  return (
    <div className="flex flex-col gap-6 mt-8">
      <div className="flex items-center gap-2">
        <MessageSquare className="w-5 h-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">강의 리뷰</h2>
      </div>

      {/* 리뷰 작성 폼 */}
      {user ? (
        !isReviewed && (
          <div className="bg-white dark:bg-[#1E1E1E] border border-gray-200 dark:border-gray-700 rounded-2xl p-6 shadow-sm">
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">별점</span>
                <div className="flex items-center gap-1">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      onClick={() => setRating(star)}
                      onMouseEnter={() => setHoverRating(star)}
                      onMouseLeave={() => setHoverRating(0)}
                      className="focus:outline-hidden"
                    >
                      <Star
                        className={cn(
                          "w-6 h-6 transition-colors",
                          star <= (hoverRating || rating)
                            ? "text-yellow-400 fill-yellow-400"
                            : "text-gray-300 dark:text-gray-600"
                        )}
                      />
                    </button>
                  ))}
                </div>
                {rating > 0 && <span className="ml-2 text-sm font-bold text-primary">{rating}점</span>}
              </div>
              
              <div className="flex gap-3">
                <Input
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="강의 리뷰를 남겨주세요."
                  maxLength={255}
                  className="flex-1 bg-gray-50 dark:bg-black/20 focus-visible:ring-primary/50"
                />
                <Button type="submit" disabled={isCreating} className="font-bold">
                  {isCreating ? "등록 중..." : "등록"}
                </Button>
              </div>
            </form>
          </div>
        )
      ) : (
        <div className="bg-gray-50 dark:bg-[#1E1E1E] border border-gray-200 dark:border-gray-700 rounded-2xl p-6 text-center shadow-sm">
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-3">
            리뷰를 작성하려면 로그인이 필요합니다.
          </p>
          <Button variant="outline" onClick={() => setLoginModalOpen(true)}>
            로그인하기
          </Button>
        </div>
      )}

      {/* 리뷰 목록 */}
      <div className="flex flex-col gap-4">
        {status === 'pending' ? (
          <div className="text-center text-sm text-gray-500 py-4">리뷰를 불러오는 중입니다...</div>
        ) : reviews.length === 0 ? (
          <div className="text-center text-sm text-gray-500 py-8 bg-gray-50 dark:bg-[#1E1E1E] rounded-2xl border border-gray-200 dark:border-gray-700 border-dashed">
            아직 등록된 리뷰가 없습니다. 첫 번째 리뷰를 남겨주세요!
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {reviews.map((review) => (
              <div 
                key={review.id} 
                className={cn(
                  "bg-white dark:bg-[#1E1E1E] border border-gray-100 dark:border-gray-800 rounded-xl p-4 md:p-5 shadow-xs transition-colors hover:border-gray-200 dark:hover:border-gray-700",
                  review.isMine && "border-primary/30 dark:border-primary/30 bg-primary/5 dark:bg-primary/5"
                )}
              >
                <div className="flex justify-between items-start gap-4 mb-2">
                  <div className="flex items-center gap-2">
                    <div className="flex items-center text-yellow-500 bg-yellow-50 dark:bg-yellow-500/10 px-2 py-0.5 rounded-full text-xs font-bold border border-yellow-200 dark:border-yellow-500/20">
                      <Star className="w-3 h-3 fill-yellow-500 mr-1" />
                      {review.rating}.0
                    </div>
                    {review.isMine && (
                      <span className="text-[10px] font-bold text-primary px-1.5 py-0.5 rounded bg-primary/10">내 리뷰</span>
                    )}
                  </div>
                  <span className="text-xs text-gray-400 font-medium">
                    {formatRelativeTime(review.createdAt)}
                  </span>
                </div>
                
                <p className="text-sm md:text-base text-gray-800 dark:text-gray-200 leading-relaxed break-all">
                  {review.content}
                </p>

                <div className="flex items-center gap-3 mt-4">
                  <button
                    onClick={() => handleReactionClick(review.id, 'LIKE')}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    <ThumbsUp className="w-3.5 h-3.5" />
                    <span>공감 {review.likeCount > 0 && review.likeCount}</span>
                  </button>
                  <button
                    onClick={() => handleReactionClick(review.id, 'DISLIKE')}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold text-gray-600 dark:text-gray-400 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    <ThumbsDown className="w-3.5 h-3.5" />
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
    </div>
  );
}
