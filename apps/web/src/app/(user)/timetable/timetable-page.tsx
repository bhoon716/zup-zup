"use client";

import { CourseDetailDialog } from "@/features/course/components/course-detail-dialog";
import { CustomScheduleDialog } from "@/features/timetable/components/custom-schedule-dialog";
import type { TimetablePageModel } from "./useTimetablePage";
import { TimetableMainSection } from "./timetable-main-section";
import { TimetableSidebarSection } from "./timetable-sidebar-section";

export function TimetablePage({ model }: { model: TimetablePageModel }) {
  const {
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
    setSelectedTimetableId,
    onCreateTimetable,
    onDeleteTimetable,
    onSetPrimaryTimetable,
    isCreatingTimetable,
    handleExportImage,
    sidebarTab,
    setSidebarTab,
    totalCredits,
    timetableCourses,
    wishlist,
    isWishlistLoading,
    handleSidebarCourseClick,
    handleAddCourse,
    handleRemoveCourse,
    isRemovingCourse,
    isAddingCourse,
    pendingAddCourseKey,
  } = model;

  return (
    <div className="min-h-[calc(100vh-4rem)] bg-background">
      <main className="mx-auto flex w-full max-w-[1600px] flex-col gap-3 px-3 py-3 sm:gap-4 sm:px-4 sm:py-4 lg:flex-row lg:items-start">
        <TimetableMainSection
          timetables={timetables}
          isListLoading={isListLoading}
          activeTimetableId={activeTimetableId}
          timetableDetail={timetableDetail}
          isDetailLoading={isDetailLoading}
          setSelectedTimetableId={setSelectedTimetableId}
          onCreateTimetable={onCreateTimetable}
          onDeleteTimetable={onDeleteTimetable}
          onSetPrimaryTimetable={onSetPrimaryTimetable}
          isCreatingTimetable={isCreatingTimetable}
          handleExportImage={handleExportImage}
          onOpenCustomDialog={() => setCustomDialogOpen(true)}
        />
        <TimetableSidebarSection
          sidebarTab={sidebarTab}
          setSidebarTab={setSidebarTab}
          totalCredits={totalCredits}
          timetableCourses={timetableCourses}
          wishlist={wishlist}
          isWishlistLoading={isWishlistLoading}
          timetableDetail={timetableDetail}
          activeTimetableId={activeTimetableId}
          handleSidebarCourseClick={handleSidebarCourseClick}
          handleAddCourse={handleAddCourse}
          handleRemoveCourse={handleRemoveCourse}
          isRemovingCourse={isRemovingCourse}
          isAddingCourse={isAddingCourse}
          pendingAddCourseKey={pendingAddCourseKey}
        />
      </main>

      <CustomScheduleDialog
        timetableId={activeTimetableId}
        open={customDialogOpen}
        onOpenChange={setCustomDialogOpen}
      />

      <CourseDetailDialog
        course={selectedSidebarCourse}
        open={sidebarCourseDialogOpen}
        onOpenChange={setSidebarCourseDialogOpen}
      />
    </div>
  );
}
