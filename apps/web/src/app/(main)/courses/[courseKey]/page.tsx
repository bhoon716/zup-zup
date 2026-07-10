import type { Metadata } from "next";
import { getCourseMetadata } from "@/shared/lib/public-metadata";
import CourseDetailPage from "./course-detail-page";

interface CoursePageProps {
  params: Promise<{ courseKey: string }>;
}

export async function generateMetadata({ params }: CoursePageProps): Promise<Metadata> {
  const { courseKey } = await params;
  return getCourseMetadata(courseKey);
}

export default function Page() {
  return <CourseDetailPage />;
}
