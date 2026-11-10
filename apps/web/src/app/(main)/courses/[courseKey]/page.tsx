import type { Metadata } from "next";
import { getCourseMetadata, getPublicCourse } from "@/shared/lib/public-metadata";
import { decodeCourseKey } from "@/shared/lib/course";
import CourseDetailPage from "./course-detail-page";

interface CoursePageProps {
  params: Promise<{ courseKey: string }>;
}

export async function generateMetadata({ params }: CoursePageProps): Promise<Metadata> {
  const { courseKey } = await params;
  return getCourseMetadata(decodeCourseKey(courseKey));
}

export default async function Page({ params }: CoursePageProps) {
  const { courseKey } = await params;
  const initialPublicCourse = await getPublicCourse(decodeCourseKey(courseKey));
  return <CourseDetailPage initialPublicCourse={initialPublicCourse} />;
}
