import { BRAND_LOGO_URL, SITE_URL } from "@/shared/seo/branding";

interface CourseSchemaParams {
  name: string;
  code?: string;
  professor?: string;
  department?: string;
  credits?: string;
}

interface ArticleSchemaParams {
  title: string;
  description: string;
  url: string;
  createdAt?: string;
}

export function generateWebsiteJsonLd() {
  return {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebSite",
        "@id": `${SITE_URL}/#website`,
        "url": SITE_URL,
        "name": "줍줍",
        "description": "전북대 수강신청 빈자리 알림 및 전북대학교 스마트 시간표 서비스",
        "inLanguage": "ko-KR",
        "image": BRAND_LOGO_URL,
      },
      {
        "@type": "SoftwareApplication",
        "@id": `${SITE_URL}/#application`,
        "name": "줍줍",
        "operatingSystem": "Web, iOS, Android",
        "applicationCategory": "EducationalApplication",
        "url": SITE_URL,
        "image": BRAND_LOGO_URL,
        "offers": {
          "@type": "Offer",
          "price": "0",
          "priceCurrency": "KRW",
        },
      },
    ],
  };
}

export function generateCourseJsonLd({
  name,
  code,
  professor,
  department,
  credits,
}: CourseSchemaParams) {
  return {
    "@context": "https://schema.org",
    "@type": "Course",
    "name": name,
    "courseCode": code ?? "",
    "description": [
      professor && `담당교수: ${professor}`,
      department && `학과: ${department}`,
      credits && `학점: ${credits}학점`,
    ]
      .filter(Boolean)
      .join(" · ") || "전북대학교 개설 강의",
    "provider": {
      "@type": "CollegeOrUniversity",
      "name": "전북대학교",
      "sameAs": "https://www.jbnu.ac.kr",
    },
  };
}

export function generateArticleJsonLd({
  title,
  description,
  url,
  createdAt,
}: ArticleSchemaParams) {
  return {
    "@context": "https://schema.org",
    "@type": "NewsArticle",
    "headline": title,
    "description": description,
    "url": url,
    "datePublished": createdAt || new Date().toISOString(),
    "publisher": {
      "@type": "Organization",
      "name": "줍줍",
      "logo": {
        "@type": "ImageObject",
        "url": BRAND_LOGO_URL,
      },
    },
  };
}
