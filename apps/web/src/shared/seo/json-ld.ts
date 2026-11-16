import { BRAND_LOGO_URL, SITE_URL } from "@/shared/seo/branding";

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
