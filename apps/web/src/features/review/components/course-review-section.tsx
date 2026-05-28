"use client";

import { AlertCircle, Loader2, MessageSquare } from "lucide-react";

import { useAuthStore } from "@/features/auth/store/useAuthStore";
import { useCourseEmojis, useToggleCourseEmoji } from "@/features/review/hooks/useReviews";
import { useUser } from "@/features/user/hooks/useUser";
import { cn } from "@/shared/lib/utils";
import { Button } from "@/shared/ui/button";

interface CourseReviewSectionProps {
  courseKey: string;
  isReviewed?: boolean;
}

const EMOJI_ITEMS = [
  { emoji: "👍", label: "공감" },
  { emoji: "🔥", label: "뜨거움" },
  { emoji: "🎓", label: "유익함" },
  { emoji: "📝", label: "메모" },
  { emoji: "😴", label: "졸림" },
  { emoji: "🚨", label: "주의" },
] as const;

/**
 * 강의 상세 화면의 이모지 반응 섹션입니다.
 * 로그인 사용자는 각 이모지를 탭해서 반응을 남기거나 취소할 수 있습니다.
 */
export function CourseReviewSection({ courseKey }: CourseReviewSectionProps) {
  const { data: emojiStats, status } = useCourseEmojis(courseKey);
  const { mutate: toggleCourseEmoji, isPending: isToggling } = useToggleCourseEmoji(courseKey);
  const { data: user, isPending: isUserLoading } = useUser();
  const setLoginModalOpen = useAuthStore((state) => state.setLoginModalOpen);

  if (!courseKey) {
    return null;
  }

  const handleEmojiClick = (emoji: string) => {
    if (!user) {
      setLoginModalOpen(true);
      return;
    }

    toggleCourseEmoji(emoji);
  };

  return (
    <section className="flex flex-col gap-4 mt-8">
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-primary" />
        <h2 className="text-base font-bold text-gray-900 dark:text-gray-100">강의 이모지</h2>
      </div>

      <div className="rounded-3xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-700 dark:bg-[#1E1E1E]">
        <div className="flex flex-col gap-2 border-b border-gray-100 pb-4 dark:border-gray-800">
          <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
            이 강의에 어울리는 이모지를 눌러주세요.
          </p>
          <div className="flex flex-wrap items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
            <span>총 6종의 고정 이모지만 사용합니다.</span>
            {!user && !isUserLoading && <span>로그인 후 이모지를 남길 수 있습니다.</span>}
            {isUserLoading && <span>로그인 상태를 확인하는 중입니다.</span>}
          </div>
        </div>

        {status === "pending" ? (
          <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500 dark:text-gray-400">
            <Loader2 className="h-4 w-4 animate-spin" />
            이모지 통계를 불러오는 중입니다.
          </div>
        ) : status === "error" ? (
          <div className="flex items-center gap-2 rounded-2xl border border-dashed border-red-200 bg-red-50/60 px-4 py-3 text-sm text-red-600 dark:border-red-900/40 dark:bg-red-900/10 dark:text-red-300">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>이모지 통계를 불러오지 못했습니다.</span>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3 pt-4 sm:grid-cols-3 lg:grid-cols-6">
            {EMOJI_ITEMS.map((item) => {
              const stat = emojiStats?.find((entry) => entry.emoji === item.emoji) ?? {
                emoji: item.emoji,
                count: 0,
                isMine: false,
              };

              return (
                <Button
                  key={item.emoji}
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => handleEmojiClick(item.emoji)}
                  disabled={isToggling || isUserLoading}
                  aria-pressed={stat.isMine}
                  aria-label={`${item.label} ${stat.count}개`}
                  className={cn(
                    "h-auto min-h-24 flex-col gap-1.5 rounded-2xl border-gray-200 px-3 py-4 text-center transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:bg-primary/5 dark:border-gray-700 dark:hover:border-primary/40 dark:hover:bg-primary/10",
                    stat.isMine &&
                      "border-primary/40 bg-primary/10 text-primary dark:border-primary/50 dark:bg-primary/15 dark:text-primary-light"
                  )}
                >
                  <span className="text-2xl leading-none">{item.emoji}</span>
                  <span className="text-xs font-semibold">{item.label}</span>
                  <span className="text-sm font-bold tabular-nums">{stat.count}</span>
                  {stat.isMine && (
                    <span className="text-[10px] font-bold uppercase tracking-wide text-primary/70 dark:text-primary-light/80">
                      내 반응
                    </span>
                  )}
                </Button>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
