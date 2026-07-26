import type { MetadataRoute } from "next";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = "https://zup-zup.com";
  const now = new Date();

  const staticRoutes: MetadataRoute.Sitemap = [
    {
      url: baseUrl,
      lastModified: now,
      changeFrequency: "daily",
      priority: 1.0,
    },
    {
      url: `${baseUrl}/search`,
      lastModified: now,
      changeFrequency: "daily",
      priority: 0.9,
    },
    {
      url: `${baseUrl}/timetable`,
      lastModified: now,
      changeFrequency: "weekly",
      priority: 0.8,
    },
    {
      url: `${baseUrl}/announcements`,
      lastModified: now,
      changeFrequency: "daily",
      priority: 0.7,
    },
    {
      url: `${baseUrl}/feedback`,
      lastModified: now,
      changeFrequency: "monthly",
      priority: 0.5,
    },
    {
      url: `${baseUrl}/privacy`,
      lastModified: now,
      changeFrequency: "monthly",
      priority: 0.3,
    },
    {
      url: `${baseUrl}/terms`,
      lastModified: now,
      changeFrequency: "monthly",
      priority: 0.3,
    },
  ];

  const apiUrl = process.env.API_URL || process.env.NEXT_PUBLIC_API_URL;
  if (!apiUrl) {
    return staticRoutes;
  }

  const dynamicRoutes: MetadataRoute.Sitemap = [];

  try {
    const annRes = await fetch(new URL("/api/v1/announcements", apiUrl), {
      next: { revalidate: 3600 },
    });
    if (annRes.ok) {
      const annData = (await annRes.json()) as { data?: { id: number; updatedAt?: string }[] };
      if (Array.isArray(annData.data)) {
        annData.data.forEach((ann) => {
          dynamicRoutes.push({
            url: `${baseUrl}/announcements/${ann.id}`,
            lastModified: ann.updatedAt ? new Date(ann.updatedAt) : now,
            changeFrequency: "weekly",
            priority: 0.6,
          });
        });
      }
    }
  } catch {
    // Ignore fetch errors during build
  }

  return [...staticRoutes, ...dynamicRoutes];
}
