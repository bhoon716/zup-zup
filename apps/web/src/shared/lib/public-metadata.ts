import type { Metadata } from "next";
import type { AnnouncementDetailResponse, Course } from "@/shared/types/api";
import {
  BRAND_ICON_512_PATH,
  BRAND_OG_IMAGE,
  SITE_URL,
} from "@/shared/seo/branding";

const getApiUrl = () => process.env.API_URL || process.env.NEXT_PUBLIC_API_URL;

const stripMarkup = (value: string) => value
  .replace(/[#*_>`~-]/g, "")
  .replace(/\s+/g, " ")
  .trim()
  .slice(0, 160);

const fetchPublicData = async <T>(path: string): Promise<T | null> => {
  const apiUrl = getApiUrl();
  if (!apiUrl) {
    return null;
  }

  try {
    const response = await fetch(new URL(path, apiUrl), {
      next: { revalidate: 60 },
    });
    if (!response.ok) {
      return null;
    }

    const body = await response.json() as { data?: T };
    return body.data ?? null;
  } catch {
    return null;
  }
};

export const getAnnouncementMetadata = async (id: number): Promise<Metadata> => {
  const fallbackTitle = "공지사항 | 줍줍";
  const announcement = await fetchPublicData<AnnouncementDetailResponse>(`/api/v1/announcements/${id}`);

  if (!announcement) {
    return {
      title: fallbackTitle,
      description: "줍줍 공지사항을 확인하세요.",
      openGraph: {
        title: fallbackTitle,
        description: "줍줍 공지사항을 확인하세요.",
        images: [BRAND_OG_IMAGE],
      },
      twitter: {
        card: "summary_large_image",
        title: fallbackTitle,
        description: "줍줍 공지사항을 확인하세요.",
        images: [BRAND_ICON_512_PATH],
      },
    };
  }

  const description = stripMarkup(announcement.content) || "줍줍 공지사항을 확인하세요.";
  const url = `${SITE_URL}/announcements/${id}`;
  return {
    title: `${announcement.title} | 줍줍`,
    description,
    keywords: ["전북대", "줍줍", "공지사항", announcement.title],
    alternates: { canonical: url },
    openGraph: {
      title: announcement.title,
      description,
      url,
      siteName: "줍줍",
      locale: "ko_KR",
      type: "article",
      images: [BRAND_OG_IMAGE],
    },
    twitter: {
      card: "summary_large_image",
      title: announcement.title,
      description,
      images: [BRAND_ICON_512_PATH],
    },
  };
};

export const getCourseMetadata = async (courseKey: string): Promise<Metadata> => {
  const course = await fetchPublicData<Course>(`/api/v1/courses/${encodeURIComponent(courseKey)}`);
  const fallbackTitle = `${courseKey} 강의 상세 | 줍줍`;

  if (!course) {
    return {
      title: fallbackTitle,
      description: "전북대학교 강의 상세 정보를 확인하세요.",
      openGraph: {
        title: fallbackTitle,
        description: "전북대학교 강의 상세 정보를 확인하세요.",
        images: [BRAND_OG_IMAGE],
      },
      twitter: {
        card: "summary_large_image",
        title: fallbackTitle,
        description: "전북대학교 강의 상세 정보를 확인하세요.",
        images: [BRAND_ICON_512_PATH],
      },
    };
  }

  const description = stripMarkup([
    course.professor && `담당교수 ${course.professor}`,
    course.department,
    course.credits && `${course.credits}학점`,
  ].filter(Boolean).join(" · ")) || "전북대학교 강의 상세 정보를 확인하세요.";
  const url = `${SITE_URL}/courses/${encodeURIComponent(courseKey)}`;
  const title = `${course.name} (${course.classNumber}) | 줍줍`;

  return {
    title,
    description,
    keywords: [
      "전북대",
      "전북대학교",
      course.name,
      course.department,
      course.professor,
      "수강신청",
      "강의상세",
    ].filter(Boolean) as string[],
    alternates: { canonical: url },
    openGraph: {
      title: `${course.name} (${course.classNumber})`,
      description,
      url,
      siteName: "줍줍",
      locale: "ko_KR",
      type: "website",
      images: [BRAND_OG_IMAGE],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: [BRAND_ICON_512_PATH],
    },
  };
};
