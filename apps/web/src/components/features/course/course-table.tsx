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
    <div className="border rounded-lg">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>과목코드</TableHead>
            <TableHead>강좌명</TableHead>
            <TableHead>교수명</TableHead>
            <TableHead className="text-center">학년</TableHead>
            <TableHead className="text-center">정원</TableHead>
            <TableHead className="text-center">현재인원</TableHead>
            <TableHead className="text-center">여석</TableHead>
            <TableHead className="text-center">상태</TableHead>
            <TableHead className="text-center">구독</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {courses.length === 0 ? (
            <TableRow>
              <TableCell colSpan={9} className="text-center py-8 text-muted-foreground">
                검색 결과가 없습니다.
              </TableCell>
            </TableRow>
          ) : (
            courses.map((course) => {
              const subscribed = isSubscribed(course.courseKey);
              const availableSeats = course.totalSeats - course.currentSeats;

              return (
                <TableRow key={course.courseKey}>
                  <TableCell className="font-mono text-sm">{course.subjectCode}</TableCell>
                  <TableCell className="font-medium">
                    <Link
                      href={`/courses/${encodeURIComponent(course.courseKey)}`}
                      className="text-blue-600 hover:underline transition-colors"
                    >
                      {course.name}
                    </Link>
                  </TableCell>
                  <TableCell>{course.professorName}</TableCell>
                  <TableCell className="text-center">{course.targetGrade}</TableCell>
                  <TableCell className="text-center">{course.totalSeats}</TableCell>
                  <TableCell className="text-center">{course.currentSeats}</TableCell>
                  <TableCell className="text-center font-semibold">
                    <span className={availableSeats > 0 ? "text-green-600" : "text-red-600"}>
                      {availableSeats}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    <span
                      className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        course.status === "AVAILABLE"
                          ? "bg-green-100 text-green-800"
                          : "bg-red-100 text-red-800"
                      }`}
                    >
                      {course.status === "AVAILABLE" ? "여석있음" : "마감"}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    {subscribed ? (
                      <Button variant="outline" size="sm" disabled className="gap-2">
                        <Check className="w-4 h-4" />
                        구독 중
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        onClick={() => handleSubscribe(course.courseKey)}
                        disabled={isPending}
                        className="gap-2"
                      >
                        <Plus className="w-4 h-4" />
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
