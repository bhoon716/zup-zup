"use client";

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Users, BookOpen, Bell, Activity, Loader2, RefreshCcw, Send } from "lucide-react";
import { useAdminStats } from "@/hooks/useAdminStats";
import { useHealth } from "@/hooks/useHealth";
import { Button } from "@/components/ui/button";
import { useCrawlCourses } from "@/hooks/useAdminActions";
import Link from "next/link";

export default function AdminDashboardPage() {
  const { data, isLoading: isStatsLoading, error: statsError } = useAdminStats();
  const { data: healthData, isLoading: isHealthLoading } = useHealth();
  const { mutate: crawl, isPending: isCrawling } = useCrawlCourses();

  const isLoading = isStatsLoading || isHealthLoading;
  const error = statsError;

  if (isLoading) {
    return (
      <div className="flex h-[60vh] items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">데이터를 불러오는데 실패했습니다.</p>
      </div>
    );
  }

  const stats = [
    { 
      title: "전체 사용자", 
      value: data?.totalUsers.toLocaleString() || "0", 
      icon: <Users className="w-5 h-5 text-blue-600" />, 
      description: "가입 유저 수" 
    },
    { 
      title: "전체 구독", 
      value: data?.totalActiveSubscriptions.toLocaleString() || "0", 
      icon: <BookOpen className="w-5 h-5 text-green-600" />, 
      description: "활성 강의 구독 수" 
    },
    { 
      title: "오늘의 알림", 
      value: data?.todayNotificationCount.toLocaleString() || "0", 
      icon: <Bell className="w-5 h-5 text-orange-600" />, 
      description: "발송된 빈자리 알림" 
    },
    { 
      title: "크롤링 상태", 
      value: data?.crawlingStatus === "RUNNING" ? "정상" : "점검 중", 
      icon: <Activity className="w-5 h-5 text-purple-600" />, 
      description: `마지막: ${data?.lastCrawledAt ? new Date(data.lastCrawledAt).toLocaleTimeString() : "-"}` 
    },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">관리자 대시보드</h1>
          <p className="text-muted-foreground mt-2">전체 서비스 현황을 한눈에 파악합니다.</p>
        </div>
        <div className="flex items-center gap-3">
          <Button 
            variant="outline" 
            onClick={() => crawl()} 
            disabled={isCrawling}
            className="hidden md:flex gap-2"
          >
            {isCrawling ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <RefreshCcw className="w-4 h-4" />
            )}
            강제 크롤링 실행
          </Button>
          <Link href="/admin/notification-test">
            <Button variant="outline" className="hidden md:flex gap-2 border-orange-500/50 text-orange-600 hover:bg-orange-500/5 hover:text-orange-700">
              <Send className="w-4 h-4" />
              알림 테스트
            </Button>
          </Link>
          <div className="flex items-center gap-2 bg-white dark:bg-gray-800 px-4 py-2 rounded-full border shadow-sm">
            <div className={`w-3 h-3 rounded-full ${healthData?.status === 'UP' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
            <span className="text-sm font-medium">
              서버 상태: {healthData?.status === 'UP' ? '정상' : '확인 불가'}
            </span>
          </div>
        </div>
      </div>

      <div className="md:hidden">
        <Button 
          variant="outline" 
          onClick={() => crawl()} 
          disabled={isCrawling}
          className="w-full gap-2"
        >
          {isCrawling ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <RefreshCcw className="w-4 h-4" />
          )}
          강제 크롤링 실행
        </Button>
        <Link href="/admin/notification-test" className="w-full mt-2 block">
          <Button variant="outline" className="w-full gap-2 border-orange-500/50 text-orange-600">
            <Send className="w-4 h-4" />
            알림 테스트
          </Button>
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {stats.map((stat, index) => (
          <Card key={index}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
              {stat.icon}
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{stat.value}</div>
              <p className="text-xs text-muted-foreground mt-1">{stat.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <Card>
          <CardHeader>
            <CardTitle>인기 구독 강의 (준비 중)</CardTitle>
            <CardDescription>가장 많이 구독 중인 과목 순위입니다.</CardDescription>
          </CardHeader>
          <CardContent className="h-[300px] flex items-center justify-center text-muted-foreground">
            데이터 수집 중...
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>최근 알림 로그 (준비 중)</CardTitle>
            <CardDescription>시스템 전체 알림 발송 기록입니다.</CardDescription>
          </CardHeader>
          <CardContent className="h-[300px] flex items-center justify-center text-muted-foreground">
            로그 로딩 중...
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
