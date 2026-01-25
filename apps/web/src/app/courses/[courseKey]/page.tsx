"use client";

import { useParams } from "next/navigation";
import { Header } from "@/components/layout/header";
import { CourseDetailCard } from "@/components/features/course/course-detail-card";
import { CourseHistoryChart } from "@/components/features/course/course-history-chart";
import { useCourseDetail, useCourseHistory } from "@/hooks/useCourses";
import { Loader2, AlertCircle, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import Link from "next/link";

export const dynamic = 'force-dynamic';

export default function CourseDetailPage() {
  const params = useParams();
  const courseKey = params.courseKey as string;

  const { data: course, isLoading: isCourseLoading, error: courseError } = useCourseDetail(courseKey);
  const { data: histories, isLoading: isHistoryLoading } = useCourseHistory(courseKey);

  if (isCourseLoading) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <Header />
        <main className="container py-8 flex flex-col items-center justify-center min-h-[60vh]">
          <Loader2 className="w-10 h-10 animate-spin text-blue-600 mb-4" />
          <p className="text-muted-foreground">강좌 정보를 불러오는 중...</p>
        </main>
      </div>
    );
  }

  if (courseError || !course) {
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
        <Header />
        <main className="container py-8">
          <div className="max-w-md mx-auto text-center space-y-4 py-12">
            <AlertCircle className="w-16 h-16 text-red-500 mx-auto" />
            <h2 className="text-2xl font-bold">강좌를 찾을 수 없습니다</h2>
            <p className="text-muted-foreground">
              요청하신 강좌 정보가 존재하지 않거나 일시적인 오류가 발생했습니다.
            </p>
            <Link href="/search">
              <Button className="mt-4">강좌 검색으로 돌아가기</Button>
            </Link>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <main className="container py-8 space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/search">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="w-5 h-5" />
            </Button>
          </Link>
          <h1 className="text-3xl font-bold">강좌 상세 정보</h1>
        </div>

        <CourseDetailCard course={course} />

        <div className="grid gap-6">
          {isHistoryLoading ? (
            <div className="h-[300px] flex items-center justify-center border rounded-lg bg-white dark:bg-gray-800">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : histories && histories.length > 0 ? (
            <CourseHistoryChart histories={histories} />
          ) : (
            <div className="h-[200px] flex flex-col items-center justify-center border rounded-lg bg-white dark:bg-gray-800 text-muted-foreground">
              <p>인원 변동 이력이 없습니다.</p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
