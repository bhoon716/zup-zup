"use client";

import { useMemo, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toPng } from "html-to-image";
import { toast } from "sonner";
import { timetableApi } from "@/features/timetable/api/timetable.api";
import {
  useAddCourseToTimetable,
  useRemoveCourseFromTimetable,
  useTimetableDetail,
  useTimetables,
} from "@/features/timetable/hooks/useTimetable";
import { useWishlist } from "@/features/wishlist/hooks/useWishlist";
import { formatDayOfWeek } from "@/shared/lib/formatters";
import type {
  Course,
  CourseClassification,
  TimetableEntryResponse,
  WishlistResponse,
} from "@/shared/types/api";

const TODAY_LABELS = ["일", "월", "화", "수", "목", "금", "토"] as const;
const COURSE_CLASSIFICATIONS = new Set<CourseClassification>([
  "계열공통",
  "교양",
  "교직(대학원)",
  "교직(대)",
  "교직",
  "군사학",
  "기초필수",
  "선수",
  "일반선택",
  "전공",
  "전공선택",
  "전공필수",
]);

type SidebarCourseSource = TimetableEntryResponse | WishlistResponse;

function normalizeClassification(value?: string): CourseClassification | undefined {
  if (!value || !COURSE_CLASSIFICATIONS.has(value as CourseClassification)) {
    return undefined;
  }

  return value as CourseClassification;
}

export const useTimetablePage = () => {
  const queryClient = useQueryClient();
  const [selectedTimetableId, setSelectedTimetableId] = useState<number | null>(null);
  const [sidebarTab, setSidebarTab] = useState<"schedule" | "wishlist">("schedule");
  const [customDialogOpen, setCustomDialogOpen] = useState(false);
  const [selectedSidebarCourse, setSelectedSidebarCourse] = useState<Course | null>(null);
  const [sidebarCourseDialogOpen, setSidebarCourseDialogOpen] = useState(false);

  const { data: timetablesData, isLoading: isListLoading } = useTimetables();
  const timetables = useMemo(() => timetablesData ?? [], [timetablesData]);
  const { data: wishlistData, isLoading: isWishlistLoading } = useWishlist();
  const wishlist = wishlistData ?? [];

  const activeTimetableId = useMemo(() => {
    if (selectedTimetableId !== null) {
      return selectedTimetableId;
    }

    if (timetables.length === 0) {
      return null;
    }

    const primary = timetables.find((timetable) => timetable.primary);
    return primary ? primary.id : timetables[0].id;
  }, [selectedTimetableId, timetables]);

  const { data: timetableDetail, isLoading: isDetailLoading } = useTimetableDetail(activeTimetableId);
  const todayLabel = TODAY_LABELS[new Date().getDay()];

  const createMutation = useMutation({
    mutationFn: (name: string) => timetableApi.createTimetable({ name, primary: timetables.length === 0 }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["timetables"] });
      setSelectedTimetableId(response.data.id);
      toast.success("시간표가 생성되었습니다.");
    },
    onError: () => toast.error("시간표 생성에 실패했습니다."),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => timetableApi.deleteTimetable(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["timetables"] });
      setSelectedTimetableId(null);
      toast.success("시간표가 삭제되었습니다.");
    },
    onError: () => toast.error("시간표 삭제에 실패했습니다."),
  });

  const setPrimaryMutation = useMutation({
    mutationFn: (id: number) => timetableApi.setPrimary(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["timetables"] });
      toast.success("대표 시간표로 설정되었습니다.");
    },
    onError: () => toast.error("대표 시간표 설정에 실패했습니다."),
  });

  const handleExportImage = async () => {
    const element = document.querySelector(".timetable-grid-content") as HTMLElement;
    if (!element) {
      toast.error("시간표를 찾을 수 없습니다.");
      return;
    }

    try {
      const padding = 40;
      const width = element.scrollWidth + padding * 2;
      const height = element.scrollHeight + padding * 2;

      const dataUrl = await toPng(element, {
        backgroundColor: "#ffffff",
        cacheBust: true,
        width,
        height,
        style: {
          padding: `${padding}px`,
          borderRadius: "0",
          margin: "0",
          transform: "none",
          width: `${element.scrollWidth}px`,
          height: `${element.scrollHeight}px`,
          boxSizing: "content-box",
          maxWidth: "none",
          minWidth: "none",
        },
        pixelRatio: 2,
      });

      const link = document.createElement("a");
      link.download = `timetable-${Date.now()}.png`;
      link.href = dataUrl;
      link.click();
      toast.success("시간표 이미지가 저장되었습니다.");
    } catch (error) {
      console.error("Export failed:", error);
      toast.error("이미지 저장에 실패했습니다.");
    }
  };

  const totalCredits = useMemo(() => {
    if (!timetableDetail) {
      return 0;
    }

    const parsedTotal = Number(timetableDetail.totalCredits);
    if (Number.isFinite(parsedTotal)) {
      return parsedTotal;
    }

    return timetableDetail.courses.reduce((sum, course) => sum + (Number(course.credits) || 0), 0);
  }, [timetableDetail]);

  const todaySchedules = useMemo(() => {
    if (!timetableDetail) {
      return [];
    }

    const courseItems = timetableDetail.courses.flatMap((course) =>
      (course.schedules ?? [])
        .filter((schedule) => formatDayOfWeek(String(schedule.dayOfWeek)) === todayLabel)
        .map((schedule, idx) => ({
          key: `${course.courseKey}-${idx}`,
          title: course.name,
          subtitle: [course.professor, course.classroom].filter(Boolean).join(" • "),
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          type: "course" as const,
        }))
    );

    const customItems = (timetableDetail.customSchedules ?? []).flatMap((custom) =>
      (custom.schedules ?? [])
        .filter((schedule) => formatDayOfWeek(schedule.dayOfWeek) === todayLabel)
        .map((schedule) => ({
          key: `custom-${custom.id}-${schedule.id}`,
          title: custom.title,
          subtitle: "직접 추가 일정",
          startTime: schedule.startTime,
          endTime: schedule.endTime,
          type: "custom" as const,
        }))
    );

    return [...courseItems, ...customItems].sort((left, right) => left.startTime.localeCompare(right.startTime));
  }, [timetableDetail, todayLabel]);

  const timetableCourses = useMemo(() => {
    if (!timetableDetail) {
      return [];
    }

    return timetableDetail.courses.map((course) => {
      const scheduleText = (course.schedules ?? [])
        .map((schedule) => `${formatDayOfWeek(String(schedule.dayOfWeek))} ${schedule.startTime}-${schedule.endTime}`)
        .join(", ");

      return {
        key: course.courseKey,
        name: course.name,
        professor: course.professor,
        classroom: course.classroom,
        credits: course.credits,
        classification: course.classification,
        scheduleText: scheduleText || course.classTime || "시간 정보 없음",
      };
    });
  }, [timetableDetail]);

  const handleSidebarCourseClick = (course: SidebarCourseSource) => {
    const mapped: Course = {
      courseKey: course.courseKey,
      subjectCode: "subjectCode" in course ? course.subjectCode : "",
      name: "courseName" in course ? course.courseName : course.name,
      classNumber: "classNumber" in course ? course.classNumber : "",
      professor: course.professor,
      credits: course.credits,
      classification: normalizeClassification(course.classification),
      classroom: "classroom" in course ? course.classroom : undefined,
      classTime: course.classTime,
    };

    setSelectedSidebarCourse(mapped);
    setSidebarCourseDialogOpen(true);
  };

  const addCourseMutation = useAddCourseToTimetable();
  const removeCourseMutation = useRemoveCourseFromTimetable();

  const handleAddCourse = (courseKey: string) => {
    if (!activeTimetableId) {
      toast.error("강의를 추가할 시간표를 먼저 선택해주세요.");
      return;
    }

    addCourseMutation.mutate({
      timetableId: activeTimetableId,
      courseKey,
    });
  };

  const handleRemoveCourse = (courseKey: string, courseName: string) => {
    if (!activeTimetableId) {
      return;
    }

    if (!confirm(`${courseName} 강의를 시간표에서 삭제하시겠습니까?`)) {
      return;
    }

    removeCourseMutation.mutate({
      timetableId: activeTimetableId,
      courseKey,
    });
  };

  return {
    selectedTimetableId,
    setSelectedTimetableId,
    sidebarTab,
    setSidebarTab,
    customDialogOpen,
    setCustomDialogOpen,
    selectedSidebarCourse,
    sidebarCourseDialogOpen,
    setSidebarCourseDialogOpen,
    timetables,
    isListLoading,
    activeTimetableId,
    timetableDetail,
    isDetailLoading,
    timetableName: timetableDetail?.name || "시간표",
    totalCredits,
    todayLabel,
    todaySchedules,
    timetableCourses,
    wishlist,
    isWishlistLoading,
    handleExportImage,
    handleSidebarCourseClick,
    onCreateTimetable: (name: string) => createMutation.mutate(name),
    onDeleteTimetable: (id: number) => deleteMutation.mutate(id),
    onSetPrimaryTimetable: (id: number) => setPrimaryMutation.mutate(id),
    isCreatingTimetable: createMutation.isPending,
    handleAddCourse,
    handleRemoveCourse,
    pendingAddCourseKey: addCourseMutation.isPending ? addCourseMutation.variables?.courseKey ?? null : null,
    pendingRemoveCourseKey: removeCourseMutation.isPending ? removeCourseMutation.variables?.courseKey ?? null : null,
    isAddingCourse: addCourseMutation.isPending,
    isRemovingCourse: removeCourseMutation.isPending,
  };
};

export type TimetablePageModel = ReturnType<typeof useTimetablePage>;
