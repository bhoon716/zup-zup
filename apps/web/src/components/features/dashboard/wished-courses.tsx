"use client";

import { Heart } from "lucide-react";
import Link from "next/link";

/**
 * 사용자가 찜한(관심) 강의 목록 중 최상위 3개를 요약하여 보여줍니다.
 * 강의 검색 페이지로 이동할 수 있는 링크를 제공합니다.
 */
export function WishedCourses({ wishlist }: { wishlist: any }) {
  const limitedWishlist = wishlist?.slice(0, 5) || [];

  return (
    <div className="bg-white dark:bg-gray-900 rounded-[2.5rem] p-6 shadow-card border border-gray-100 dark:border-gray-800 transition-all hover:shadow-xl group flex flex-col h-full min-h-[460px]">
      <div className="flex justify-between items-center mb-6 shrink-0">
        <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
          <span className="w-8 h-8 rounded-xl bg-pink-50 dark:bg-pink-950/30 flex items-center justify-center">
            <Heart className="w-4 h-4 text-pink-500 fill-pink-500" />
          </span>
          찜한 강의
        </h3>
        <Link href="/search" className="text-xs font-bold text-gray-400 hover:text-primary transition-colors">
          더보기
        </Link>
      </div>

      <div className="space-y-3">
        {limitedWishlist.length > 0 ? (
          limitedWishlist.map((course: any) => (
            <div key={course.courseKey} className="p-4 border border-gray-100 dark:border-gray-800 rounded-2xl hover:shadow-md hover:border-pink-100 dark:hover:border-pink-950 transition-all cursor-pointer bg-white dark:bg-gray-900 group/item">
              <div className="flex justify-between items-center mb-2">
                <span className="text-[9px] font-black text-pink-500 bg-pink-50 dark:bg-pink-950/30 px-2 py-0.5 rounded-md border border-pink-100 dark:border-pink-900/50">
                  {course.classification}
                </span>
                <span className="text-[9px] font-bold text-gray-400 uppercase tracking-tighter">{course.credits} Credits</span>
              </div>
              <h4 className="font-bold text-sm text-gray-800 dark:text-gray-200 group-hover/item:text-pink-600 transition-colors">
                {course.courseName}
              </h4>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1 flex items-center gap-1">
                {course.professor} <span className="w-1 h-1 rounded-full bg-gray-200 dark:bg-gray-700" /> {course.classTime}
              </p>
            </div>
          ))
        ) : (
          <p className="text-xs text-center py-6 text-gray-400 font-medium">찜한 강의가 없습니다.</p>
        )}
      </div>
    </div>
  );
}
