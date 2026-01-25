"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { useSubscribe, useSubscriptions } from "@/hooks/useSubscriptions";
import type { Course } from "@/types/api";
import { Check, Plus } from "lucide-react";
import Link from "next/link";

interface CourseTableProps {
  courses: Course[];
}

export function CourseTable({ courses }: CourseTableProps) {
  const { data: subscriptions } = useSubscriptions();
  const { mutate: subscribe, isPending } = useSubscribe();

  const isSubscribed = (courseKey: string) => {
    return subscriptions?.some((sub) => sub.courseKey === courseKey);
  };

  const handleSubscribe = (courseKey: string) => {
    subscribe({ courseKey });
  };

  return (
    <div className="border rounded-xl overflow-hidden shadow-sm bg-card/30 backdrop-blur-md">
      <Table>
        <TableHeader className="bg-muted/50">
          <TableRow>
            <TableHead className="w-[100px]">과목코드</TableHead>
            <TableHead>강좌명</TableHead>
            <TableHead>교수명</TableHead>
            <TableHead className="text-center">이수구분</TableHead>
            <TableHead className="text-center">언어</TableHead>
            <TableHead className="text-center">정원</TableHead>
            <TableHead className="text-center">여석</TableHead>
            <TableHead className="text-center">상태</TableHead>
            <TableHead className="text-center w-[120px]">구독</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {courses.length === 0 ? (
            <TableRow>
              <TableCell colSpan={9} className="text-center py-16 text-muted-foreground italic">
                검색 조건에 맞는 강좌가 없습니다.
              </TableCell>
            </TableRow>
          ) : (
            courses.map((course) => {
              const subscribed = isSubscribed(course.courseKey);
              const isAvailable = (course.available ?? 0) > 0;

              return (
                <TableRow key={course.courseKey} className="hover:bg-muted/30 transition-colors">
                  <TableCell className="font-mono text-xs text-muted-foreground">{course.subjectCode}</TableCell>
                  <TableCell className="font-semibold">
                    <div className="flex flex-col">
                      <Link
                        href={`/courses/${encodeURIComponent(course.courseKey)}`}
                        className="text-primary hover:underline transition-colors"
                      >
                        {course.name}
                      </Link>
                      <span className="text-[10px] text-muted-foreground">{course.department}</span>
                    </div>
                  </TableCell>
                  <TableCell className="text-sm">{course.professor || "-"}</TableCell>
                  <TableCell className="text-center">
                    <span className="text-xs px-2 py-0.5 rounded-full bg-secondary/50 text-secondary-foreground border border-secondary">
                      {course.classification || "-"}
                    </span>
                  </TableCell>
                  <TableCell className="text-center text-xs">{course.lectureLanguage || "-"}</TableCell>
                  <TableCell className="text-center text-sm font-medium">{course.capacity || 0}</TableCell>
                  <TableCell className="text-center font-bold">
                    <span className={isAvailable ? "text-green-500" : "text-destructive"}>
                      {course.available || 0}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-bold uppercase ${
                        isAvailable
                          ? "bg-green-100/10 text-green-500 border border-green-500/20"
                          : "bg-destructive/10 text-destructive border border-destructive/20"
                      }`}
                    >
                      {isAvailable ? "Available" : "Full"}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    {subscribed ? (
                      <Button variant="ghost" size="sm" disabled className="gap-2 text-green-500/80 bg-green-500/5 hover:bg-green-500/5">
                        <Check className="w-4 h-4" />
                        진행중
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        onClick={() => handleSubscribe(course.courseKey)}
                        disabled={isPending}
                        className="gap-2 h-8 rounded-full shadow-lg shadow-primary/10"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        구독
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              );
            })
          )}
        </TableBody>
      </Table>
    </div>
  );
}
