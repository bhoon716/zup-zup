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
    timetableName,
    setSelectedTimetableId,
    onCreateTimetable,
    onDeleteTimetable,
    onSetPrimaryTimetable,
    isCreatingTimetable,
    handleExportImage,
    sidebarTab,
    setSidebarTab,
    totalCredits,
    todayLabel,
    todaySchedules,
    timetableCourses,
    wishlist,
    isWishlistLoading,
    handleSidebarCourseClick,
    handleAddCourse,
    handleRemoveCourse,
    isAddingCourse,
    isRemovingCourse,
    pendingAddCourseKey,
    pendingRemoveCourseKey,
  } = model;

  return (
    <div className="min-h-[calc(100vh-4rem)] bg-slate-100">
      <main className="mx-auto flex w-full max-w-[1600px] flex-col gap-2 p-2 sm:gap-4 sm:p-4 lg:flex-row">
        <TimetableMainSection
          timetables={timetables}
          isListLoading={isListLoading}
          activeTimetableId={activeTimetableId}
          timetableDetail={timetableDetail}
          isDetailLoading={isDetailLoading}
          timetableName={timetableName}
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
          todayLabel={todayLabel}
          todaySchedules={todaySchedules}
          timetableCourses={timetableCourses}
          wishlist={wishlist}
          isWishlistLoading={isWishlistLoading}
          timetableDetail={timetableDetail}
          activeTimetableId={activeTimetableId}
          handleSidebarCourseClick={handleSidebarCourseClick}
          handleAddCourse={handleAddCourse}
          handleRemoveCourse={handleRemoveCourse}
          isAddingCourse={isAddingCourse}
          isRemovingCourse={isRemovingCourse}
          pendingAddCourseKey={pendingAddCourseKey}
          pendingRemoveCourseKey={pendingRemoveCourseKey}
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
