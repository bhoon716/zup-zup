import type { Metadata } from "next";
import { getAnnouncementMetadata, getPublicAnnouncement } from "@/shared/lib/public-metadata";
import AnnouncementDetailPage from "./announcement-detail-page";

interface AnnouncementPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: AnnouncementPageProps): Promise<Metadata> {
  const { id } = await params;
  const numericId = Number(id);
  return getAnnouncementMetadata(Number.isInteger(numericId) ? numericId : 0);
}

export default async function Page({ params }: AnnouncementPageProps) {
  const { id } = await params;
  const initialAnnouncement = await getPublicAnnouncement(Number(id));
  return <AnnouncementDetailPage initialAnnouncement={initialAnnouncement} />;
}
